Space Flight Dynamics as a Service (SFDaaS)
===========================================

Overview
--------
A web based implementation of an orbit propagation using the free and open 
source OreKit [http://orekit.org] a "A free low level space dynamics library".

Usage
-----

In a web browser (should be all one line, i.e. remove \ and linefeeds)
 http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000& \ 
                                               tf=2010-05-28T13:00:00.000& \
                                               r0=[3198022.67,2901879.73,5142928.95]& \ 
                                               v0=[-6129.640631,4489.647187,1284.511245]

Epochs are in the ISO-8601 format, i.e. http://en.wikipedia.org/wiki/ISO_8601

 Input:
  t0 = Initial epoch in this format yyyy-mm-ddThh:MM:ss.sss
  tf = Final   epoch in this format yyyy-mm-ddThh:MM:ss.sss
  r0 = [ x0,   y0,  z0 ] (initial radius vector in meters)
  v0 = [ vx0, vy0, vz0 ] (initial velocity vector in meters/second)

 Output:
  rf = [ xf,   yf,  zf ]
  vf = [ vxf, vyf, vzf ]

Epochs are assumed to be UTC and J2000 Earth-centered is the assumed frame. State can be propagated forwards or backwards in time.  

Expected Output
---------------
Run Start : 2011-10-19T09:04:54.819-0400
Run   End : 2011-10-19T09:04:54.826-0400

A priori state : 
  Epoch = 2010-05-28T12:00:00.000
  r = [3198022.67,2901879.73,5142928.95]
  v = [-6129.640631,4489.647187,1284.511245]

A posteriori state : 
  Epoch = 2010-05-28T13:00:00.000
  r = [2675781.697895,-4864168.991743,-3780046.7712272173]
  v = [6450.161074,288.065314,4204.487908334615]

The epochs are assumed to be UTC and r0 and v0 are in meters and in the J2000 Earth-centered frame.

Required packages
-----------------
* OreKit:       https://www.orekit.org/forge/attachments/download/48/orekit-5.0.3-sources.zip
* Commons Math: http://mirrors.gigenet.com/apache//commons/math/source/commons-math-2.2-src.tar.gz

Useful packages
---------------
* Tomcat 7:    http://mirrors.axint.net/apache/tomcat/tomcat-7/v7.0.22/bin/apache-tomcat-7.0.22.zip
* Eclipse IDE: http://eclipse.org/downloads/packages/eclipse-ide-java-developers/indigosr1

Licensing
---------
SFDaaS is free and is licensed by Haisam K. Ido under the GPL License.  A copy 
of this license is provided in the LGPL-LICENSE.txt file.

SFDaaS relies upon Orekit and the Apache commons-math library.

Orekit [http://www.orekit.org] is licensed by CS Communication & Systèmes under 
the Apache License Version 2.0. A copy of this license is provided in OreKit's LICENSE.txt file.

Orekit relies on the commons-math from the Apache Software Foundation 
http://commons.apache.org/math/ released under the Apache license, version 2