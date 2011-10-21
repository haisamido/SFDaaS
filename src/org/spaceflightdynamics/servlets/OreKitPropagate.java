package org.spaceflightdynamics.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/* Overview
 * --------
 * <p>A servlet class which numerically propagates an orbit forwards or 
 * backwards in time. It currently only utilizes the free and open source 
 * OreKit [http://orekit.org] a "A free low level space dynamics library"</p>.
 *
 * <em>Usage:</em>
 *   <a href="http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2010-05-28T13:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]">	
 * <pre>
 * 
 * Epochs are in the ISO-8601 format, i.e. http://en.wikipedia.org/wiki/ISO_8601
 * 
 * Input:
 *  t0 = Initial epoch in this format yyyy-mm-ddThh:MM:ss.sss
 *  tf = Final   epoch in this format yyyy-mm-ddThh:MM:ss.sss
 *  r0 = [ x0,   y0,  z0 ] (initial radius vector in meters)
 *  v0 = [ vx0, vy0, vz0 ] (initial velocity vector in meters/second)
 *
 * Output:
 *  rf = [ xf,   yf,  zf ]
 *  vf = [ vxf, vyf, vzf ]
 * Epochs are assumed to be UTC and J2000 Earth-centered is the assumed frame. State can be propagated forwards or backwards in time.  
 *
 * </pre>
 *
 * @author  Haisam K. Ido <haisam.ido@gmail.com>
 * @license LGPL v3.0 
*/

import org.spaceflightdynamics.propagation.Propagator;

/**
 * Servlet implementation class OreKit
 */
@WebServlet(
		name = "OreKitPropagate", 
		description = "Numerical propagation using the OreKit library [http://orekit.org]", 
		urlPatterns = { "/orekit/propagate" }, 
		initParams = { 
				@WebInitParam(name = "t0", value = "2010-05-28T12:00:00.000", description = "Initial epoch"), 
				@WebInitParam(name = "tf", value = "2010-05-28T13:00:00.000", description = "Final   epoch"),
				@WebInitParam(name = "r0", value = "[3198022.67,2901879.73,5142928.95]", description = "Initial radial vector (meters)"), 
				@WebInitParam(name = "v0", value = "[-6129.640631,4489.647187,1284.511245]", description = "Initial velocity vector (meters/second)"), 
		})

public class OreKitPropagate extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public OreKitPropagate() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

		HttpSession session = request.getSession(true);
		String JSESSIONID   = session.getId();
		String USERNAME     = System.getProperty("user.name");
        String _KEY1        = USERNAME + "]|[" + JSESSIONID;
		
    	PrintWriter out = response.getWriter();
        
        String t0 = request.getParameter("t0");
        String r0 = request.getParameter("r0");
        String v0 = request.getParameter("v0");
        String tf = request.getParameter("tf");

        // Set part of the key for caching
        String _KEY2 = t0 + "]|[" + r0 + "]|[" + v0 + "]|[" + tf;
        String KEY   = _KEY1 + "]|[" + _KEY2;
        
        /*
         * Normal use of this class starts here.  Construct the propapgator 
         * using the strings.
         */
        Propagator ps = new Propagator(r0, v0, t0, tf);
        
		Date date0 = new Date();
		String strDate0 = df.format( date0 );

		HashMap<String,String> finalState = ps.propagate();

		Date datef = new Date();
		String strDatef = df.format( datef );
		
		tf = finalState.get("tf");
        String rf = finalState.get("rf");
        String vf = finalState.get("vf");
       
        out.println(DOCTYPE);

        out.println("<hr>");
        out.println("Space Flight Dynamics as a Service (SFDaaS)");
        out.println("<hr>");
        out.println("<pre>");
        out.println("A priori state:");
        out.println(" t0 = " + t0);
        out.println(" r0 = " + r0);
        out.println(" v0 = " + v0);
        out.println("");

        out.println("A posteriori state: ");
        out.println(" tf = " + tf);
        out.println(" rf = " + rf);
        out.println(" vf = " + vf);
        out.println("");
        out.println("Assumptions:");
        out.println(" 1) The epochs, t0 and tf, are assumed to be in UTC. ");
        out.println(" 2) The radius and velocity vectors are in meters and meters/second, respectively.");
        out.println(" 3) The frame is assumed to be the J2000 Earth-centered one.");
        out.println("<hr>");

        // Run properties
        out.println("Run Properties:");
        out.println(" Run Start     : " + strDate0);
        out.println(" Run End       : " + strDatef);
        out.println("");
        out.println(" Username      : " + USERNAME );
        
        // print session info
        Date   created     = new Date(session.getCreationTime());
        String strCreated  = df.format( created ); 
        Date accessed      = new Date(session.getLastAccessedTime());
        String strAccessed = df.format( accessed ); 
        
        out.println("");
        out.println(" JSESSIONID    : " + JSESSIONID);
        out.println(" Created       : " + strCreated);
        out.println(" Last Accessed : " + strAccessed);
        out.println("");
        out.println(" CACHING KEY   : " + KEY);

    }
    public static final String DOCTYPE =
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">";

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
        doGet(request, response);
	}

}
