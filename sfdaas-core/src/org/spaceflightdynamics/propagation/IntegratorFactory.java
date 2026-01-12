package org.spaceflightdynamics.propagation;

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

    // Default tolerances for adaptive integrators
    private static final double DEFAULT_MIN_STEP = 0.001;
    private static final double DEFAULT_MAX_STEP = 1000.0;
    private static final double DEFAULT_POSITION_TOLERANCE = 10.0; // 10 meters
    private static final double DEFAULT_VELOCITY_TOLERANCE = 0.01; // 0.01 m/s

    /**
     * Create an integrator based on the specified type and step size.
     *
     * @param type The propagator type
     * @param stepSize The step size in seconds (used for fixed-step integrators)
     * @return An appropriate AbstractIntegrator instance
     */
    public static AbstractIntegrator createIntegrator(PropagatorType type, double stepSize) {
        switch (type) {
            case RUNGE_KUTTA:
                return new ClassicalRungeKuttaIntegrator(stepSize);

            case DORMAND_PRINCE:
                // Adaptive step integrator with tolerances
                return new DormandPrince853Integrator(
                    DEFAULT_MIN_STEP,
                    DEFAULT_MAX_STEP,
                    DEFAULT_POSITION_TOLERANCE,
                    DEFAULT_VELOCITY_TOLERANCE
                );

            case ADAMS_BASHFORTH:
                // Adams-Bashforth with 4 steps (order 4)
                return new AdamsBashforthIntegrator(
                    4, // nSteps
                    DEFAULT_MIN_STEP,
                    DEFAULT_MAX_STEP,
                    DEFAULT_POSITION_TOLERANCE,
                    DEFAULT_VELOCITY_TOLERANCE
                );

            case ADAMS_MOULTON:
                // Adams-Moulton with 4 steps (order 4)
                return new AdamsMoultonIntegrator(
                    4, // nSteps
                    DEFAULT_MIN_STEP,
                    DEFAULT_MAX_STEP,
                    DEFAULT_POSITION_TOLERANCE,
                    DEFAULT_VELOCITY_TOLERANCE
                );

            default:
                // Fallback to Runge-Kutta
                return new ClassicalRungeKuttaIntegrator(stepSize);
        }
    }

    /**
     * Create an integrator with custom tolerances for adaptive integrators.
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
