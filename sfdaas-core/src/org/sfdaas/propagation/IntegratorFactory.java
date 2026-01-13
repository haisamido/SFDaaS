package org.sfdaas.propagation;

import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.ode.nonstiff.AdamsBashforthIntegrator;
import org.hipparchus.ode.nonstiff.AdamsMoultonIntegrator;

/**
 * Factory class for creating numerical integrators based on PropagatorType.
 * Provides adaptive and fixed step integrators for orbit propagation.
 *
 * @author SFDaaS
 */
public class IntegratorFactory {

    /**
     * Create an integrator with full control over step sizes and tolerances.
     *
     * @param type The propagator type
     * @param stepSize The step size in seconds (for fixed-step integrators)
     * @param minStep Minimum step size for adaptive integrators
     * @param maxStep Maximum step size for adaptive integrators
     * @param positionTolerance Position tolerance in meters
     * @param velocityTolerance Velocity tolerance in m/s
     * @return An appropriate AbstractIntegrator instance
     */
    public static AbstractIntegrator createIntegrator(
            PropagatorType type,
            double stepSize,
            double minStep,
            double maxStep,
            double positionTolerance,
            double velocityTolerance) {

        switch (type) {
            case RUNGE_KUTTA:
                return new ClassicalRungeKuttaIntegrator(stepSize);

            case DORMAND_PRINCE:
                return new DormandPrince853Integrator(
                    minStep,
                    maxStep,
                    positionTolerance,
                    velocityTolerance
                );

            case ADAMS_BASHFORTH:
                return new AdamsBashforthIntegrator(
                    4,
                    minStep,
                    maxStep,
                    positionTolerance,
                    velocityTolerance
                );

            case ADAMS_MOULTON:
                return new AdamsMoultonIntegrator(
                    4,
                    minStep,
                    maxStep,
                    positionTolerance,
                    velocityTolerance
                );

            default:
                return new ClassicalRungeKuttaIntegrator(stepSize);
        }
    }
}
