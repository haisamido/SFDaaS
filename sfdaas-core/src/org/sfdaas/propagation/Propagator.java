package org.sfdaas.propagation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.AbstractIntegrator;

import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

/***
 * <p>Class to perform propagation using a numerical propagator.  The initial 
 * state is specified using strings.  There are two possible constructors: one 
 * taking the initial state as a HashMap&lsaquo;String,String&rsaquo; which has 
 * keys <em>r0</em>, <em>v0</em>, <em>t0</em>, and <em>tf</em>,and another 
 * taking the initial state as strings in the order r0, v0, t0, tf.  In both 
 * constructors, <em>r0</em> and <em>v0</em> have the String format 
 * "[xxx.xxx, yyy.yyy, zzz.zzz]".  <em>t0</em> has the format 
 * "YYYY-MM-DDTHH:MM:SS.SSS". Right now, I don't support the timezone suffix.
 * r0 and v0 must be specified in meters in the Earth-centered J2000 frame.</p>
 *    
 * <p>Example of how to use this class.  In a webapp you don't call main, 
 * instead just create an instance of Propagator and pass it the HashMap 
 * containing the parameters extracted from the URL, then call the propagate 
 * method.  The propagate method returns a HashMap containing the propagation 
 * results.  See the main() routine.</p>
 * 
 * <em>Example:</em>
 * <pre>
 *   Propagator p = new Propagator(parms);  // Where parms is a HashMap, or...      
 *   //Propagator p = new Propagator(r0, v0, t0, tf);  // Using Strings      
 *   HashMap&lsaquo;String,String&rsaquo; finalState = p.propagate();
 * </pre>
 *
 * @author Steve
 *
 */
public class Propagator {

    /*
     * stepSize            - integrator step size
     * parms               - HashMap of integration parameters
     * numericalPropagator - Orekit propagator.
     */
    private double stepSize = 60.;
    private HashMap<String,String> parms;
    private NumericalPropagator numericalPropagator;
    private FrameType frameType = FrameType.EME2000;
    private EngineType engineType = EngineType.OREKIT;
    
    /*
     * Regular expression for matching the string vector format: 
     * "[1.23, 4.56, 7.789]".  Using the format "1.23,4.56,7.89" might be nicer
     * and is easier to parse (you just split it on commas).
     */    
    /* Should change regex using samples from 
     * http://www.regular-expressions.info/floatingpoint.html
     */
//    private static String VECTOR_REGEX = 
//        "\\[\\s*([0-9\\.\\+\\-]+)\\s*,\\s*([0-9\\.\\+\\-]+)\\s*,\\s*([0-9\\.\\+\\-]+)\\s*\\]";
    private static String NUMBER_ANY_REGEX =
    		"[-+]?[0-9]*\\.?[0-9]+(?:[eEdD][-+]?[0-9]+)?";

    private static String NUMBER_FLOATINGPOINT_REGEX =
    		"[-+]?[0-9]*\\.[0-9]+(?:[eEdD][-+]?[0-9]+)?";

    private static String NUMBER_INTEGER_REGEX =
    		"[+-]?\\d+";

    private static String VECTOR_REGEX = 
            "\\[\\s*("+
            NUMBER_FLOATINGPOINT_REGEX +
            ")\\s*,\\s*+("+
            NUMBER_FLOATINGPOINT_REGEX +
            ")\\s*,\\s*(" +
            NUMBER_FLOATINGPOINT_REGEX +
            ")\\s*\\]";
 
    
    /*
     * Data path - defaults to 'data' directory relative to working directory
     * Can be overridden via system property: orekit.data.path
     */
    private static String UTCTAI_PATH =
    		System.getProperty("orekit.data.path",
    			System.getProperty("user.dir") + "/data/");
    
    /**
     * Empty (default) constructor.
     */
    public Propagator() {       
    }
    
    /**
     * Construct an instance of the Propagator using a HashMap containing the 
     * propagation options.  The epochs are assumed to be UTC and r0 and v0 are 
     * in meters and in the J2000 Earth-centered frame.
     * 
     * @param hm - HashMap<String,String> with keys "r0", "v0", "t0", "tf"
     */
    public Propagator(HashMap<String,String> hm) {
        
        /*
         * Initialize the propagator using the user supplied parameters before 
         * you can propagate.
         */
        initialize(hm);

    }

    /**
     * Initialize the Orekit components by creating the numerical integrator,
     * creating initial orbit state and assigning it to the propagator.
     * @param hm - HashMap of propagation parameters
     */
    public void initialize(HashMap<String,String> hm) {

        parms = hm;

        AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;

        /*
         * Extract step size if provided, otherwise use default (60 seconds)
         */
        if (parms.containsKey("stepSize") && parms.get("stepSize") != null) {
            try {
                stepSize = Double.parseDouble(parms.get("stepSize"));
            } catch (NumberFormatException e) {
                // Keep default if parsing fails
                stepSize = 60.0;
            }
        }

        /*
         * Extract frame type if provided, otherwise use default (EME2000)
         */
        if (parms.containsKey("frame") && parms.get("frame") != null) {
            frameType = FrameType.fromKey(parms.get("frame"));
        }

        /*
         * Extract engine type if provided, otherwise use default (OREKIT)
         */
        if (parms.containsKey("engine") && parms.get("engine") != null) {
            engineType = EngineType.fromKey(parms.get("engine"));
        }

        /*
         * Validate that the selected engine is implemented
         */
        if (!engineType.isImplemented()) {
            throw new IllegalArgumentException(
                "Engine '" + engineType.getDisplayName() + "' is not yet implemented. " +
                "Currently implemented engines: " +
                java.util.Arrays.stream(EngineType.getImplemented())
                    .map(EngineType::getKey)
                    .collect(java.util.stream.Collectors.joining(", "))
            );
        }

        /*
         * This is how you tell Orekit where the UTC-TAI data is.  You need to
         * change this path to the regular-data directory on your machine, and
         * figure out how to reference it as a resource on Google App Engine!
         */
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, UTCTAI_PATH); 

        /*
         * Extract the epoch parameter ("t0") and convert it to an Orekit
         * AbsoluteDate.  These exceptions should really be thrown up the chain
         * to the calling application, so the web user gets feedback.  Handling
         * the exceptions here only prints them to stdout.
         */
        try {

            // Get the time scale (defaults to UTC if not specified)
            String timeScaleKey = parms.get("timeScale");
            org.orekit.time.TimeScale orekitTimeScale = getTimeScale(timeScaleKey);

            epoch = new AbsoluteDate(parms.get("t0"), orekitTimeScale);

        } catch (IllegalArgumentException e) {

            e.printStackTrace();
            return;

        } catch (OrekitException e) {

            e.printStackTrace();
            return;
        }
        
        System.out.println("t0=" + epoch);
        
        /*
         * Extract the position from the r0 string and convert it to an apache 
         * commons Vector3D.
         */        
        Pattern pattern = Pattern.compile(VECTOR_REGEX);
        
        Matcher matcher = pattern.matcher(parms.get("r0"));

        if (!matcher.find()) {
            System.out.println("Couldn't match the position parameter");
            System.out.println(parms.get("r0"));
        }
                
        Vector3D v3r = new Vector3D(Double.parseDouble(matcher.group(1)), 
                                    Double.parseDouble(matcher.group(2)), 
                                    Double.parseDouble(matcher.group(3)));
        
        System.out.println(v3r);
        
        /*
         * Extract the velocity from the v0 strings and convert it to an apache 
         * commons Vector3D.
         */        
        matcher = matcher.reset(parms.get("v0"));

        if (!matcher.find()) {
            System.out.println("Couldn't match the velocity parameter");
        }
                
        Vector3D v3v = new Vector3D(Double.parseDouble(matcher.group(1)), 
                                    Double.parseDouble(matcher.group(2)), 
                                    Double.parseDouble(matcher.group(3)));
 
        System.out.println(v3v);

        /*
         * We're finally ready to start the Orekit stuff.  First create an
         * Orekit NumericalPropagator using the selected integrator type.
         * Default to Runge-Kutta if no propagator type is specified.
         */
        PropagatorType propagatorType = PropagatorType.fromKey(parms.get("propagator"));

        // Default tolerances for adaptive integrators
        double minStep = 0.001;           // 1 millisecond
        double maxStep = 1000.0;          // 1000 seconds
        double positionTolerance = 10.0;  // 10 meters
        double velocityTolerance = 0.01;  // 0.01 m/s

        AbstractIntegrator integrator = IntegratorFactory.createIntegrator(
            propagatorType, stepSize, minStep, maxStep, positionTolerance, velocityTolerance);
        numericalPropagator = new NumericalPropagator(integrator);
          
        /*
         * Now create an Orbit from the initialState using the selected reference frame.
         * The exceptions should be thrown back to the web app.
         */

        Orbit orbit = null;

        try {

            // Get mu value for the specified central body (defaults to Earth if not specified)
            String centralBody = parms.get("centralBody");
            double mu = getMuForCentralBody(centralBody);

            orbit = new CartesianOrbit(
                            new PVCoordinates(v3r,v3v),
                            FrameFactory.createFrame(frameType),
                            epoch,
                            mu);

        } catch (IllegalArgumentException e) {

            e.printStackTrace();

        } catch (OrekitException e) {

            e.printStackTrace();

        }
        
        /*
         * Create a SpacecraftState using the orbit and assign the state as the
         * initial state for the orbit propagator.  The numerical propagator is
         * now ready to propagate.
         */
        SpacecraftState state = new SpacecraftState(orbit);
        numericalPropagator.setInitialState(state);

    }
    
    /**
     * Propagate the state using Orekit.  The propagation proceeds from the
     * parameters it was initialized with and propagates to the time tf.
     * If outputInterval is specified, intermediate states are also collected.
     *
     * @return HashMap<String,String> containing the keys "rf", "vf", "tf"
     *         and optionally "intervalStates" if outputInterval parameter is set
     */
    public HashMap<String,String> propagate() {

        SpacecraftState final_state = null;
        List<String> intervalStatesList = new ArrayList<>();

        try {

            // Get the time scale (defaults to UTC if not specified)
            String timeScaleKey = parms.get("timeScale");
            org.orekit.time.TimeScale orekitTimeScale = getTimeScale(timeScaleKey);

            // Get initial and final dates
            AbsoluteDate t0Date = new AbsoluteDate(parms.get("t0"), orekitTimeScale);
            AbsoluteDate tfDate = new AbsoluteDate(parms.get("tf"), orekitTimeScale);

            // Check if output interval is specified
            String outputIntervalStr = parms.get("outputInterval");
            if (outputIntervalStr != null && !outputIntervalStr.trim().isEmpty()) {
                try {
                    double intervalSeconds = Double.parseDouble(outputIntervalStr.trim());

                    if (intervalSeconds > 0) {
                        // Calculate number of interval points
                        double totalDuration = tfDate.durationFrom(t0Date);
                        int numIntervals = (int) Math.floor(totalDuration / intervalSeconds);

                        // Propagate and collect intermediate states
                        for (int i = 1; i <= numIntervals; i++) {
                            double offsetSeconds = i * intervalSeconds;
                            if (offsetSeconds >= totalDuration) break;

                            AbsoluteDate intermediateDate = t0Date.shiftedBy(offsetSeconds);
                            SpacecraftState intermediateState = numericalPropagator.propagate(intermediateDate);

                            // Format as comma-separated row: t,rx,ry,rz,vx,vy,vz
                            String stateRow = String.format("%s,%f,%f,%f,%f,%f,%f",
                                intermediateDate.toString(orekitTimeScale),
                                intermediateState.getPVCoordinates().getPosition().getX(),
                                intermediateState.getPVCoordinates().getPosition().getY(),
                                intermediateState.getPVCoordinates().getPosition().getZ(),
                                intermediateState.getPVCoordinates().getVelocity().getX(),
                                intermediateState.getPVCoordinates().getVelocity().getY(),
                                intermediateState.getPVCoordinates().getVelocity().getZ());

                            intervalStatesList.add(stateRow);
                        }
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid outputInterval value: " + outputIntervalStr);
                }
            }

            // Propagate to final time
            final_state = numericalPropagator.propagate(tfDate);

        } catch (IllegalArgumentException e) {

            e.printStackTrace();

        } catch (OrekitException e) {

            e.printStackTrace();

        }

        /*
         * Stuff the propagation results into a HashMap and return it to the
         * caller.
         */
        HashMap<String,String> final_hash = new HashMap<String,String>();

        final_hash.put("rf", String.format("[%f,%f,%s]",
                final_state.getPVCoordinates().getPosition().getX(),
                final_state.getPVCoordinates().getPosition().getY(),
                final_state.getPVCoordinates().getPosition().getZ()));

        final_hash.put("vf", String.format("[%f,%f,%s]",
                final_state.getPVCoordinates().getVelocity().getX(),
                final_state.getPVCoordinates().getVelocity().getY(),
                final_state.getPVCoordinates().getVelocity().getZ()));

        final_hash.put("tf", parms.get("tf"));

        // Add interval states if any were collected (CSV format with newline separator)
        if (!intervalStatesList.isEmpty()) {
            final_hash.put("intervalStates", String.join("\n", intervalStatesList));
        }

        return(final_hash);

    }

    /**
     * Get the gravitational parameter (mu) for the specified central body.
     *
     * @param centralBody The central body key (e.g., "earth", "sun", "moon") or custom value (e.g., "mu:3.986e14")
     * @return The gravitational parameter in m³/s²
     */
    private double getMuForCentralBody(String centralBody) throws OrekitException {
        if (centralBody == null || centralBody.isEmpty()) {
            return CelestialBodyFactory.getEarth().getGM();
        }

        // Check if custom mu value format: "mu:value"
        if (centralBody.startsWith("mu:")) {
            try {
                String muValueStr = centralBody.substring(3);
                return Double.parseDouble(muValueStr);
            } catch (NumberFormatException e) {
                System.err.println("Invalid custom mu value: " + centralBody);
                // Default to Earth if invalid
                return CelestialBodyFactory.getEarth().getGM();
            }
        }

        switch (centralBody.toLowerCase()) {
            case "earth":
                return CelestialBodyFactory.getEarth().getGM();
            case "sun":
                return CelestialBodyFactory.getSun().getGM();
            case "moon":
                return CelestialBodyFactory.getMoon().getGM();
            case "mars":
                return CelestialBodyFactory.getMars().getGM();
            case "jupiter":
                return CelestialBodyFactory.getJupiter().getGM();
            case "venus":
                return CelestialBodyFactory.getVenus().getGM();
            case "saturn":
                return CelestialBodyFactory.getSaturn().getGM();
            default:
                // Default to Earth if unknown body
                return CelestialBodyFactory.getEarth().getGM();
        }
    }

    /**
     * Get the OreKit TimeScale object for the specified time scale type.
     *
     * @param timeScaleKey The time scale key (e.g., "utc", "tai")
     * @return The OreKit TimeScale object
     * @throws OrekitException if there's an error accessing the time scale
     */
    private org.orekit.time.TimeScale getTimeScale(String timeScaleKey) throws OrekitException {
        TimeScale timeScale = TimeScale.fromKey(timeScaleKey);

        switch (timeScale) {
            case TAI:
                return TimeScalesFactory.getTAI();
            case UTC:
            default:
                return TimeScalesFactory.getUTC();
        }
    }

    /**
     * Here is a test case for this class.  Don't call main when using the
     * class.
     */
    public static void main(String[] args) {
    
        /*
         * HashMap example. These will normally come from parsing the user URL.
         */
        HashMap<String,String> parms = new HashMap<String,String>();
        
        parms.put("r0", "[  3198022.67,  2901879.73,  5142928.95]");
        parms.put("v0", "[-6129.640631, 4489.647187, 1284.511245]");
        parms.put("t0", "2010-05-28T12:00:00.000");
        parms.put("tf", "2010-05-29T12:00:00.000");
        
        /*
         * Normal use of this class starts here.  Construct the propagator 
         * using the HashMap.
         */
        Propagator p = new Propagator(parms);
        
        HashMap<String,String> finalState = p.propagate();
        
        System.out.println(finalState);
        
        /*
         * String example. These will normally come from parsing the user URL.
         * Build a HashMap from the string parameters.
         */
        HashMap<String,String> parms2 = new HashMap<String,String>();
        parms2.put("r0", "[  3198022.67,  2901879.73,  5142928.95]");
        parms2.put("v0", "[-6129.640631, 4489.647187, 1284.511245]");
        parms2.put("t0", "2010-05-28T12:00:00.000");
        parms2.put("tf", "2010-05-29T12:00:00.000");

        /*
         * Normal use of this class starts here.  Construct the propagator
         * using the HashMap.
         */
        Propagator ps = new Propagator(parms2);

        finalState = ps.propagate();

        System.out.println(finalState);

    }
    
}