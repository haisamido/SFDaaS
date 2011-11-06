package org.spaceflightdynamics.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.net.URLEncoder;
import java.net.URLDecoder;

// This class is not used any where else, yet.
public class QualityControlInput {

	// Year (4 digit only)
	private static String yyyy_REGEX =
        	"\\d{4}";

	// Day of Year regex  001-366
	private static String DOY_REGEX =
        	"[0-2]\\d[1-9]|0[1-9]0|1\\d0|2\\d0|3[0-5]\\d|36[0-6]";

	// Month regex 01-12
	private static String mm_REGEX =
        	"0[1-9]|1[012]";

	// Day of Month regex 01-31
	private static String dd_REGEX =
        	"(0[1-9]|[12]\\d|3[01])";

	// Hour regex 00-23
	private static String hh_REGEX =
        	"[0-1]\\d|2[0-3]";

	// Min regex 00-59
	private static String MM_REGEX =
        	"[0-5]\\d";

	// Seconds regex 00-59.nnnnnnn
	private static String ss_REGEX =
        	"[0-5]\\d\\.?[0-9]*";

	private static String tz_REGEX =
			"[-+]\\d{4}";

	private static String NUMBER_ANY_REGEX =
        	"[-+]?[0-9]*\\.?[0-9]+(?:[eEdD][-+]?[0-9]+)?";

	private static String NUMBER_FLOATINGPOINT_REGEX =
			"[-+]?[0-9]*\\.[0-9]+(?:[eEdD][-+]?[0-9]+)?";

    private static String NUMBER_INTEGER_REGEX =
            "[+-]?\\d+";

    private static String VECTOR_REGEX1 = 
            "\\[\\s*("+NUMBER_FLOATINGPOINT_REGEX+")\\s*,\\s*+("+NUMBER_FLOATINGPOINT_REGEX+")\\s*,\\s*("+NUMBER_FLOATINGPOINT_REGEX+")\\s*\\]";

    private static String VECTOR_REGEX2 = 
            "\\[\\s*("+
            NUMBER_ANY_REGEX +
            ")\\s*,\\s*+("+
            NUMBER_ANY_REGEX +
            ")\\s*,\\s*(" +
            NUMBER_ANY_REGEX +
            ")\\s*\\]";

    private static String DATE_ISO_REGEX =
    		"("+ yyyy_REGEX + "){1}?-(((" + mm_REGEX + "){1}?-("+ dd_REGEX + "){1}?)|(" + DOY_REGEX + "){1}?)T(" + 
                 hh_REGEX + "){1}?:(" + MM_REGEX + "){1}?:(" + ss_REGEX + "){1}?(" + tz_REGEX + ")?";

    private static String r0 = "[3198022.67,2901879.73,+5142928.95e+00]";
    private static String v0 = "[-6129.640631,4489.647187,1284.511245]";
    private static String date0 = "2011-12-02T23:01:02.0000+0400";
    
    /*
     * Problems:
     * 	1) + is rendered as a space by Tomcat, replacing + with %2B in URL works but that's not natural
     *  2) space in URL creates an invalid KEY for memcached.  Perhaps I should delete spaces!
     *  3) using %2B when using curl is ok for memecached, because it converts it to + !!!
     *  4) curl requires escaping [ and ], i.e. \[ and \]
     */
    public static void main(String[] args) {
    	String REGEX   = VECTOR_REGEX2;
    	REGEX          = DATE_ISO_REGEX;
    	String toMatch = date0;
    	
        Pattern pattern = Pattern.compile(REGEX);        
        Matcher matcher = pattern.matcher(toMatch);
        		
    	System.out.println(REGEX);
    	
        if (!matcher.find()) {
        	
            System.out.println("Couldn't match "+ toMatch + " the position parameter");
            
        } else {
        	
            System.out.println("Matched " + toMatch + " the position parameter");
            
        }

//        System.out.println(toMatch);
   
        String _URL="http://localhost:8080/SFDaaS/orekit/propagate2?cf=1&t0=2010-05-28T12:00:00.000&tf=2011-05-28T12:00:00.000&r0=[+3198022.67,2901879.73,%2B5142928.95e%2B00]&v0=[-6129.640631,4489.647187,1284.511245]";
        _URL="http://localhost:8080/SFDaaS/orekit/propagate2?cf=1&t0=2010-05-28T12:00:00.000&tf=2011-05-28T12:00:00.000&r0=[+3198022.67,2901879.73,%2B5142928.95e%2B00]&v0=[-6129.640631,4489.647187,1284.511245]";
        String encoded_URL=URLEncoder.encode(_URL);
        String decoded_URL=URLDecoder.decode(_URL);
        System.out.println(_URL);
        System.out.println(encoded_URL);
        System.out.println(decoded_URL);
        
    }
    
} 

