package org.spaceflightdynamics.propagation;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;

import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
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
    
    /*
     * Regular expression for matching the string vector format: 
     * "[1.23, 4.56, 7.789]".  Using the format "1.23,4.56,7.89" might be nicer
     * and is easier to parse (you just split it on commas).
     */    
    /* Should change regex using samples from 
     * http://www.regular-expressions.info/floatingpoint.html
     */
    private static String VECTOR_REGEX = 
        "\\[\\s*([0-9\\.\\+\\-]+)\\s*,\\s*([0-9\\.\\+\\-]+)\\s*,\\s*([0-9\\.\\+\\-]+)\\s*\\]";
    
    /*
     * Change this to the correct path for your machine.
     */
    private static String UTCTAI_PATH = 
    		"/Users/haisam/Documents/workspace.indigo/SFDaaS/data";
//		"resource_files/orekit/regular-data";
    
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
     * Create a propagator using strings instead of a HashMap.
     * 
     * @param r0 - initial position "[x, y, z]"
     * @param v0 - initial velocity "[vx,vy,vz]"
     * @param t0 - initial epoch "YYYY-MM-DDTHH:MM:SS.SSS"
     * @param tf - final epoch "YYYY-MM-DDTHH:MM:SS.SSS"
     */
    public Propagator(String r0, String v0, String t0, String tf) {

        /*
         * Create a hash map from the String parameters.
         */
        HashMap<String,String> hm = new HashMap<String,String>();
        
        hm.put("r0", r0);
        hm.put("v0", v0);
        hm.put("t0", t0);
        hm.put("tf", tf);
        
        /*
         * Call the initializer with the HashMap.
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
            
            epoch = new AbsoluteDate(parms.get("t0"), 
                            TimeScalesFactory.getUTC());
            
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
         * Orekit NumericalPropagator using the apache-commons Runge-Kutta
         * integrator.
         */
        numericalPropagator = new NumericalPropagator(
                                new ClassicalRungeKuttaIntegrator(stepSize));
          
        /*
         * Now create an Orbit from the initialState.  Again, the exceptions
         * should be thrown back to the web app.
         */

        Orbit orbit = null;
        
        try {
            
            orbit = new CartesianOrbit(
                            new PVCoordinates(v3r,v3v), 
                            FramesFactory.getEME2000(), 
                            epoch, 
                            CelestialBodyFactory.getEarth().getGM());
            
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
     * @return HashMap<String,String> containing the keys "rf", "vf", "tf"
     */
    public HashMap<String,String> propagate() {
        
        SpacecraftState final_state = null;
        
        try {
            
            final_state = numericalPropagator.propagate(
                            new AbsoluteDate(parms.get("tf"), 
                            TimeScalesFactory.getUTC()));
            
        } catch (PropagationException e) {

            e.printStackTrace();
            
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
        
        return(final_hash);
        
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
         */
        String r0 = "[  3198022.67,  2901879.73,  5142928.95]";
        String v0 = "[-6129.640631, 4489.647187, 1284.511245]";
        String t0 = "2010-05-28T12:00:00.000";
        String tf = "2010-05-29T12:00:00.000";
        
        /*
         * Normal use of this class starts here.  Construct the propapgator 
         * using the strings.
         */
        Propagator ps = new Propagator(r0, v0, t0, tf);
        
        finalState = ps.propagate();
        
        System.out.println(finalState);

    }
    
}