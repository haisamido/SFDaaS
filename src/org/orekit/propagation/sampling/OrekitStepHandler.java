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
package org.orekit.propagation.sampling;

import java.io.Serializable;

import org.orekit.errors.PropagationException;

/** This interface is a space-dynamics aware step handler.
 *
 * <p>It mirrors the <code>StepHandler</code> interface from <a
 * href="http://commons.apache.org/math/"> commons-math</a> but
 * provides a space-dynamics interface to the methods.</p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public interface OrekitStepHandler extends Serializable {

    /** Handle the current step.
     * @param interpolator interpolator set up for the current step
     * @param isLast if true, this is the last integration step
     * @exception PropagationException if step cannot be handled
     */
    void handleStep(OrekitStepInterpolator interpolator, boolean isLast)
        throws PropagationException;

    /** Determines whether this handler needs dense output.
     * @return true if the handler needs dense output
     */
    boolean requiresDenseOutput();

    /** Reset the step handler.
     */
    void reset();

}
