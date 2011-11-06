package org.spaceflightdynamics.utils;

import java.io.IOException;

public class RegexPatterns {

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

	//
	private static String NUMBER_ANY_REGEX =
        	"[-+]?[0-9]*\\.?[0-9]+(?:[eEdD][-+]?[0-9]+)?";

	private static String NUMBER_FLOATINGPOINT_REGEX =
			"[-+]?[0-9]*\\.[0-9]+(?:[eEdD][-+]?[0-9]+)?";

    private static String NUMBER_INTEGER_REGEX =
            "[+-]?\\d+";

    /*
     * Vector Based Regexes
     */
    private static String VECTOR_DELIMITER="\\s*,\\s*" ;
    
    private static String VECTOR_ROW_DELIMITER="\\s*;\\s*" ;
    
    // [ number1, number2, number3 ] Floating point only
    private static String VECTOR3_NUMBER_FLOATINGPOINT_REGEX = 
    		"\\[\\s*("+NUMBER_FLOATINGPOINT_REGEX+")\\s*,\\s*+("+NUMBER_FLOATINGPOINT_REGEX+")\\s*,\\s*("+NUMBER_FLOATINGPOINT_REGEX+")\\s*\\]";

    // [ number1, number2, number3 ] Floating point or integers
    private static String VECTOR3_NUMBER_ANY_REGEX = 
    		"\\[\\s*("+NUMBER_ANY_REGEX+")\\s*,\\s*+("+NUMBER_ANY_REGEX+")\\s*,\\s*("+NUMBER_ANY_REGEX+")\\s*\\]";

    // [ number1, number2, number3 ] Floating point only
    private static String VECTOR6_NUMBER_FLOATINGPOINT_REGEX = 
    		"\\[\\s*("+NUMBER_FLOATINGPOINT_REGEX+")\\s*,\\s*+("+NUMBER_FLOATINGPOINT_REGEX+")\\s*,\\s*("+NUMBER_FLOATINGPOINT_REGEX+")\\s*\\]";

    // [ number1, number2, number3 ] Floating point or integers
    private static String VECTOR6_NUMBER_ANY_REGEX = 
    		"\\[\\s*"+
           "("+ NUMBER_ANY_REGEX + ")"+
    	    "\\s*,\\s*" +
            "(" + 
            NUMBER_ANY_REGEX +
            ")\\s*,\\s*("+NUMBER_ANY_REGEX+")\\s*\\]";
 
    private static String DATE_ISO_REGEX =
    		"("+ yyyy_REGEX + "){1}?-(((" + mm_REGEX + "){1}?-("+ dd_REGEX + "){1}?)|(" + DOY_REGEX + "){1}?)T(" + 
                 hh_REGEX + "){1}?:(" + MM_REGEX + "){1}?:(" + ss_REGEX + "){1}?(" + tz_REGEX + ")?";

//    $pattern = "(${yyyy}){1}?\-(((${mm}){1}?\-(${dd}){1}?)|(${doy}){1}?)T(${hh}){1}?:(${MM}){1}?:(${ss}){1}?(${tz})?"; 

    public static void main(String[] args) {	 
    	System.out.println(VECTOR6_NUMBER_ANY_REGEX);
    	System.out.println(DATE_ISO_REGEX);
    }
}
