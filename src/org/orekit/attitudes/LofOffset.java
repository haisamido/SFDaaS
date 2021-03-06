/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.attitudes;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.RotationOrder;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.utils.PVCoordinates;


/**
 * This class provides a default attitude law.

 * <p>
 * The attitude law is defined as a rotation offset from local orbital frame.
 * This rotation can be defined by
 * NB : Local orbital frame is defined as follows :
 * </p>
 * <ul>
 *   <li>Z axis pointed towards central body,</li>
 *   <li>Y opposite to angular momentum</li>
 *   <li>X roughly along velocity (it would be perfectly aligned only for
 *       circular orbits or at perigee and apogee of non-circular orbits).</li>
 * </ul>
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class LofOffset implements AttitudeLaw {

    /** Dummy attitude law, perfectly aligned with the LOF frame. */
    public static final LofOffset LOF_ALIGNED =
        new LofOffset(RotationOrder.ZYX, 0., 0., 0.);

    /** Serializable UID. */
    private static final long serialVersionUID = -713570668596014285L;

    /** Rotation from local orbital frame.  */
    private final Rotation offset;

    /** Creates new instance.
     * @param order order of rotations to use for (alpha1, alpha2, alpha3) composition
     * @param alpha1 angle of the first elementary rotation
     * @param alpha2 angle of the second elementary rotation
     * @param alpha3 angle of the third elementary rotation
     */
    public LofOffset(final RotationOrder order, final double alpha1,
                     final double alpha2, final double alpha3) {
        this.offset = new Rotation(order, alpha1, alpha2, alpha3);
    }


    /** {@inheritDoc} */
    public Attitude getAttitude(final Orbit orbit) {

        final PVCoordinates pv = orbit.getPVCoordinates();
        final Frame frame = orbit.getFrame();


        // Construction of the local orbital frame
        final Vector3D p = pv.getPosition();
        final Vector3D momentum = pv.getMomentum();
        final double angularVelocity = momentum.getNorm() / p.getNormSq();

        final Rotation lofRot = new Rotation(p, momentum, Vector3D.MINUS_K, Vector3D.MINUS_J);
        final Vector3D spinAxis = new Vector3D(angularVelocity, Vector3D.MINUS_J);

        // Compose with offset rotation
        return new Attitude(orbit.getDate(), frame, offset.applyTo(lofRot), offset.applyTo(spinAxis));

    }

}
