package org.spaceflightdynamics.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;

import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Memcached libraries
 */
import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;

/*
 * OreKit libraries
 */
import org.orekit.data.DataProvidersManager;
import org.spaceflightdynamics.propagation.Propagator;

/* Overview
 * --------
 * <p>A servlet class which numerically propagates an orbit forwards or 
 * backwards in time. It currently only utilizes the free and open source 
 * OreKit [http://orekit.org] a "A free low level space dynamics library"</p>.
 *
 * <em>Usage:</em>
 *   <a href="http://localhost:8080/SFDaaS/orekit/propagate2?cf=0&ca=127.0.0.1:11211%20127.0.0.1:11211&t0=2010-05-28T12:00:00.000&tf=2010-05-29T12:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]">	
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
 * Servlet implementation class for OreKit
 * @author  Haisam K. Ido <haisam.ido@gmail.com>
 * @license LGPL v3.0 
*/

@WebServlet(
		name = "OreKitPropagate", 
		description = "Numerical propagation using the OreKit library [http://orekit.org]", 
		urlPatterns = { "/orekit/propagate", "/orekit/propagate/usage" }
)

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
		
		// Printout Section
    	PrintWriter out          = response.getWriter();
    	String runComments       = " ";
    	String propagateComments = " None. Found in cache.";
    	
//    	out.println(getServletConfig().getInitParameter("param1"));
     	
    	// Cache Section
    	String cacheComments     = " ";
    	Object Content           = null; // Content of cache
    	String ck                = null;

    	// Output Orbital State Section
        String rf                = null;
        String vf                = null;
    	
    	// Set Date/Time format and Timezone
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS Z");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));

		// Session management
		HttpSession session = request.getSession(true);
		String JSESSIONID   = session.getId();
		String USERNAME     = System.getProperty("user.name");
        String _KEY1        = USERNAME + "]|[" + JSESSIONID;

        // To determine if usage() is requested
        String RequestURI    = request.getRequestURI();

        /*
         * HTML Header stuff
         */
        out.println(DOCTYPE);
        out.println("<hr>");
        out.println("Space Flight Dynamics as a Service (SFDaaS)");
        out.println("<hr>");
        out.println("<pre>");
       	//out.println(getServletConfig());
      	//out.println(getServletInfo());

        Pattern usagePattern = Pattern.compile("usage$");
        Matcher usageMatcher = usagePattern.matcher(RequestURI);

        if (usageMatcher.find()) {
        	
        	out.println( (String)(usage(request)) );
        	//out.println( "<hr");
        	out.println( (String)(bugs()) );
        	
        } else {
        
	        // Default Initial conditions via URI
            
        	// Caching section
        	// Caching Servers' addresses
        	//  e.g. ca="127.0.0.1:11211 192.168.1.99:11212"
			String ca = null;
			
			/*
			 * cf = caching flag
			 *  0 = Ignore caching and proceed with actions <default>
			 *  1 = Determine if content (final orbit state) is in cache, if not 
			 *      put it in cache, after propagation
			 */
			String cf = "0";
			
			// ct = number of integer seconds to hold cache for, used if cf=1		
			String ct = "60";  // 60 seconds value for caching

			// Caching server and port aaa.bbb.ccc.ddd:nnnnn
	        if( request.getParameter("ca") != null) { ca=request.getParameter("ca"); }
	
	        // User Input caching flag
	        if( request.getParameter("cf") != null) { cf=request.getParameter("cf"); }

	        // User Input cache expiration time in seconds
	        if( request.getParameter("ct") != null) { ct=request.getParameter("ct"); }	
		
	        //
			// URI Input: Session Section
	        //
			String sf = null;   // Check if there is a session id, if not create one
			String st = "1800"; // 300 seconds value for session id

	        // User Input session flag
	        if( request.getParameter("sf") != null) {
	        	
	        	sf=request.getParameter("sf"); 
	        	
		        // User Input session expiration time in seconds
		        if( request.getParameter("st") != null) { 
		        	st=request.getParameter("st"); 
		        	session.setMaxInactiveInterval(Integer.valueOf(st));
		        }
		        
	        }
	

	        /*
	         * Initial Orbit State and final epoch
	         */
	        /**
	         * @param t0 - initial epoch "YYYY-MM-DDTHH:MM:SS.SSS in UTC"
	         * @param r0 - initial position "[x, y, z] in meters"
	         * @param v0 - initial velocity "[vx,vy,vz] in meters/sec"
	         * @param tf - final   epoch "YYYY-MM-DDTHH:MM:SS.SSS in UTC"
	         */
	        
	        //String reqVal = new String(request.getParameter("r0").getBytes("UTF-8"),"UTF-8");
	        //out.println(reqVal);
	        
	        String t0 = request.getParameter("t0"); // Initial Epoch
	        String r0 = request.getParameter("r0"); // Initial radius vector
	        String v0 = request.getParameter("v0"); // Initial velocity vector
	        String tf = request.getParameter("tf"); // Final Epoch
	
	        //
	        runComments=" Run Start       : " + df.format(new Date()) + "\n";
	        /*
	         * Start Caching Logic
	         */
	        String _KEY2 = t0 + "]|[" + r0 + "]|[" + v0 + "]|[" + tf;
	        String KEY   = _KEY1 + "]|[" + _KEY2;        

	        // User Input: Override cache's key and use user provided one
	        if( request.getParameter("ck") != null) { ck=request.getParameter("ck"); KEY=ck;}
	        
	        // Caching
	        if( Integer.valueOf(cf) == 1 ) {
	    		MemcachedClient Cache=null;
	    		Cache =  new MemcachedClient(new BinaryConnectionFactory(),AddrUtil.getAddresses(ca));      
	        	Content = Cache.get(KEY);
	         }
	        
	        // Source: http://code.google.com/p/spymemcached/wiki/Examples
	        /*
	        MemcachedClient Cache2=null;
	        Cache2 = new MemcachedClient(new BinaryConnectionFactory(),AddrUtil.getAddresses(ca));
	        Future<Object> f = Cache2.asyncGet(KEY);        
	        try {
	            Content=f.get(2, TimeUnit.SECONDS);
	        } catch(TimeoutException e) {
	            // Since we don't need this, go ahead and cancel the operation.  This
	            // is not strictly necessary, but it'll save some work on the server.
	            f.cancel(false);
	            // Do other timeout related stuff
	        }
	        */

			if (Content != null) {
	            cacheComments = " Caching Flag    : " + cf +"\n";
				cacheComments = cacheComments + " Found in cache  : " + (df.format(new Date())) + "\n";
				HashMap<String,String> finalState = (HashMap<String, String>) Content;		
				tf = finalState.get("tf");
		        rf = finalState.get("rf");
		        vf = finalState.get("vf");
				cacheComments = cacheComments + " Retrieved cache : " + (df.format(new Date())) + "\n";    
	            cacheComments = cacheComments + " Cache's Key     : " + KEY + "\n";            
				cacheComments = cacheComments + " Caching Server  : " + ca ;
			} else {
	
	        /*
	         * Start of Propagation Logic
	         */			
	            cacheComments = " Caching Flag    : " + cf ;
				
				propagateComments = " Start Propagate : "+(df.format(new Date()))+"\n";
				Propagator ps = new Propagator(r0, v0, t0, tf);
				HashMap<String,String> finalState = ps.propagate();
				propagateComments = propagateComments + " End   Propagate : "+(df.format(new Date()));	
				
		        if( Integer.valueOf(cf) == 1 ) {
		    		MemcachedClient Cache=null;
		    		Cache =  new MemcachedClient(new BinaryConnectionFactory(),AddrUtil.getAddresses(ca));      
					cacheComments = cacheComments + "\n Not in cache    : "+(df.format(new Date()))+"\n";
		        	Cache.set(KEY, Integer.valueOf(ct), finalState);
		            cacheComments = cacheComments + " Placed in cache : " + (df.format(new Date())) + "\n";
		            cacheComments = cacheComments + " Expires in      : " + Integer.valueOf(ct) + " seconds\n";
		            cacheComments = cacheComments + " Expiry Date     : " + df.format(new Date().getTime()+Integer.valueOf(ct)*1000) + "\n";            
		            cacheComments = cacheComments + " Cache's Key     : " + KEY +"\n";            	            
					cacheComments = cacheComments + " Caching Server  : " + ca ;
		        }
		        
				tf = finalState.get("tf");
		        rf = finalState.get("rf");
		        vf = finalState.get("vf");
		        
			}
	       
	        /*
	         * End of Propagation
	         */
	        
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
	
	        // OREKIT properties
	        out.println("OREKIT Properties:");
	        out.println(" OREKIT_DATA_PATH: " + DataProvidersManager.OREKIT_DATA_PATH );        
	        out.println("");
	        
	        // Cache properties
	        out.println("Caching Properties:");
	        out.println(cacheComments);
	        out.println("");
	        
	        // Propagate properties
	        out.println("Propagation Properties:");
	        out.println(propagateComments);
	        out.println("");
	        
	        // Run & System properties
	        out.println("Run & System Properties:");
	        runComments=runComments+" Run Stop        : " + df.format(new Date());
	        out.println(runComments);
	        out.println(" Username        : " + System.getProperty("user.name"));
	        out.println(" Home Directory  : " + System.getProperty("user.home") );
	        out.println(" User's CWD      : " + System.getProperty("user.dir") );
	        out.println("");
	        
	        // Session properties
	        out.println("Session Properties:");
	        out.println(" JSESSIONID      : " + JSESSIONID);
	        out.println(" Created         : " + df.format(new Date(session.getCreationTime())));
	        out.println(" Expiry in       : " + session.getMaxInactiveInterval() + " seconds");
	        out.println(" Expiry Date     : " + df.format(new Date(session.getCreationTime() + session.getMaxInactiveInterval()*1000)) );
	        out.println(" Last Accessed   : " + df.format(new Date(session.getLastAccessedTime())));
	        out.println("");
       
	        // Request properties
	        // Source: http://download.oracle.com/javaee/1.4/api/javax/servlet/http/HttpServletRequest.html
	        out.println("Request Properties:");
	        out.println(" Authentication : " + request.getAuthType());
	        out.println(" Remote User    : " + request.getRemoteUser());
	        out.println(" Remote Address : " + request.getRemoteAddr());
	        out.println(" Path Info      : " + request.getPathInfo());
	        out.println(" Protocol       : " + request.getProtocol());
	        out.println(" Request URI    : " + request.getRequestURI());
	        out.println(" Request URL    : " + request.getRequestURL());
	        out.println(" Query String   : " + request.getQueryString());
	        out.println(" Query Encoded  : " + URLEncoder.encode( request.getQueryString() )); 
	        out.println(" Query Decoded  : " + URLDecoder.decode( URLEncoder.encode( request.getQueryString() )) ); 
	        out.println(" Request Method : " + request.getMethod());
	        out.println("");
	        
	        // Headers
	        out.println("Header Properties:");	        
	        Enumeration e = request.getHeaderNames();
	        while (e.hasMoreElements()) {
	            String name  = (String)e.nextElement();
	            String value = request.getHeader(name);
	            out.println(" " + name + " = " + value );
	        }        

        }
        out.println("</pre>");
	}
    public static final String DOCTYPE =
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">";

	protected String usage(HttpServletRequest request) {
		String usageText;
        
        // Not too happy with this, but this is a first effort
		usageText = "";
		usageText = usageText + "Usage:\n";
		usageText = usageText + " http://" + request.getHeader("host") + "/SFDaaS/orekit/propagate& \\ \n";            	
		usageText = usageText + "  &cf=0                   [default: ignore caching and proceed]\n";
		usageText = usageText + "    &cf=1                 [Check if in cache, if in cache use.\n";
		usageText = usageText + "                           if not put in cache after propagation]\n";
		usageText = usageText + "      &ca=127.0.0.1:11211 [Caching address and port]\n";
		usageText = usageText + "      &ct=60              [Default: 60 seconds to store in cache ]\n";
		usageText = usageText + "      &ck={KEY}           [User supplied caching key, otherwise use built-in one]\n";
		usageText = usageText + "  &sf=1                   [default = null, 1 means use my values ]\n";
		usageText = usageText + "  &st=300                 [default = 1800 seconds]\n";
		usageText = usageText + "  &t0=yyyymmddThhMMss.sss [only UTC Timezone]\n";
		usageText = usageText + "  &tf=yyyymmddThhMMss.sss [only UTC Timezone]\n";
		usageText = usageText + "  &r0=[x0,y0,z0]          [only in meters ]\n";
		usageText = usageText + "  &v0=[vx0,vy0,vz0]       [only in meters/second ]\n\n";
		usageText = usageText + " Usage examples:\n";
		usageText = usageText + "  1) <a href=\"http://localhost:8080/SFDaaS/orekit/propagate/usage\" target=\"orekit_eg_1\">Usage</a>\n";
		usageText = usageText + "  2) <a href=\"http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2010-05-29T12:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]\" target=\"orekit_eg_2\">Propagation</a>\n";
		usageText = usageText + "  3) <a href=\"http://localhost:8080/SFDaaS/orekit/propagate?cf=1&t0=2010-05-28T12:00:00.000&tf=2011-05-28T12:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]\" target=\"orekit_eg_3\">Propagation with Memcaching using default values (should return 'HTTP Status 500' error because memcaching server and port are undefined)</a>\n";
		usageText = usageText + "  4) <a href=\"http://localhost:8080/SFDaaS/orekit/propagate?cf=1&ca=127.0.0.1:11211&%20127.0.0.1:112112%20192.168.1.101:11211&t0=2010-05-28T12:00:00.000&tf=2011-05-28T12:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]\" target=\"orekit_eg_4\">Propagation with Memcaching using default values with 3 memcaching servers defined.</a>\n";
		usageText = usageText + "  5) <a href=\"http://localhost:8080/SFDaaS/orekit/propagate?cf=1&ct=15&ca=127.0.0.1:11211&%20127.0.0.1:112112%20192.168.1.101:11211&t0=2010-05-28T12:00:00.000&tf=2011-05-28T12:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]\" target=\"orekit_eg_5\">Propagation with Memcaching using ct=15, i.e. cache for 15 seconds only</a>\n";
		
		return usageText;
	}
	
	protected String bugs() {
		String bugText;
		// Not to happy with this, but this is a first effort
		bugText = "";
		bugText = "\nKnown issues:\n";
		bugText = bugText + " 1) e+00:\n";
		bugText = bugText + "    -6129.640631e-00 works\n";
		bugText = bugText + "    -6129.640631e+00 will not work. Change e+00 to e%2B00, where %2B is + in URL encoding\n";
		bugText = bugText + " 2) Curl:\n";
		bugText = bugText + "    [ & ] need to be escaped via \\, i.e. \\[ & \\]\n";
		bugText = bugText + "    Quote the URL\n";
		bugText = bugText + " 3) Orekit:\n";
		bugText = bugText + "    Can only propagate forwards or backwards in time and nothing else (OreKit is much more cabpable).\n";
		
		return bugText;
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
        doGet(request, response);
	}

}
