package org.spaceflightdynamics.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

    	PrintWriter out = response.getWriter();
        
        String t0 = request.getParameter("t0");
        String r0 = request.getParameter("r0");
        String v0 = request.getParameter("v0");
        String tf = request.getParameter("tf");
        
        //r0_mag=Vector3D.dotProduct(r0,r0);
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
        out.println("<br>");
        out.println("<font face=\"Courier\">");
        out.println("Run Start : "+strDate0+"<br>");
        out.println("Run &nbsp;&nbsp;End : "+strDatef+"<br><hr><br>");

        out.println("A priori state : <br>");
        out.println("&nbsp;&nbsp;t0 = " + t0 + "<br>");
        out.println("&nbsp;&nbsp;r0 = " + r0 + "<br>");
        out.println("&nbsp;&nbsp;v0 = " + v0 + "<br>");
        out.println("<br><br>");

        out.println("A posteriori state : <br>");
        out.println("&nbsp;&nbsp;tf = " + tf + "<br>");
        out.println("&nbsp;&nbsp;rf = " + rf + "<br>");
        out.println("&nbsp;&nbsp;vf = " + vf + "<br><br><hr><br>");
        out.println("The epochs, t0 and tf, are assumed to be in UTC. The radius and veclocity vectors are in meters and meters/second, respectively.  The frame is assumed to be the J2000 Earth-centered one.");
        
        out.println("</font>");

    }
    public static final String DOCTYPE =
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">";

//    private static String UTCTAI_PATH = 
//    	"war/resource_files/orekit/regular-data/";

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
