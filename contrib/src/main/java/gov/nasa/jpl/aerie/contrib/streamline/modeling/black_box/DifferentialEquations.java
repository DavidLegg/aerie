package gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.monads.UnstructuredDynamicsApplicative;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import static gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching.failure;
import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.set;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.every;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.*;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.effect;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear.linear;

public class DifferentialEquations {
    private DifferentialEquations() {}

    /**
     * Defines a first-order ODE numerical integrator.
     * Given the initial conditions, returns Linear dynamics that start
     * at initialConditions and interpolate to the result of one iteration of
     * the chosen numerical integration algorithm.
     */
    @FunctionalInterface
    public interface FirstOrderODEIntegrator {
        Linear[] nextSegment(
                BiFunction<Duration, double[], double[]> fPrime,
                double[] y0,
                Duration t0,
                Duration timeStep);
    }

    /**
     * Defines a set of linear resources as the numerical integration of a system
     * of first-order ordinary differential equations.
     * @param solver The {@link FirstOrderODEIntegrator} to use for integration.
     *               Several are pre-built in {@link DifferentialEquations}.
     *               If you don't know which one to pick, try {@link DifferentialEquations#rungeKutta4()} first.
     * @param fPrime Computes y' given the current time (elapsed since simulation start) and a current state vector y.
     * @param initialConditions The initial state vector.
     * @param timeStep The length of time for one solver step.
     */
    public static List<Resource<Linear>> firstOrderODE(
            FirstOrderODEIntegrator solver,
            BiFunction<Duration, double[], double[]> fPrime,
            double[] initialConditions,
            Duration timeStep) {
        // Compute the initial segments
        var initialSegment = solver.nextSegment(fPrime, initialConditions, currentTime(), timeStep);
        var results = Arrays.stream(initialSegment).map(MutableResource::resource).toList();
        // TODO - it feels like something in ErrorCatching should be able to handle this try-catch for me...
        Resource<Unstructured<double[]>> currentState = () -> {
            try {
                var state = new double[results.size()];
                int i = 0;
                for (var result : results) state[i++] = currentValue(result);
                return UnstructuredDynamicsApplicative.pure(state);
            } catch (Throwable e) {
                return failure(e);
            }
        };
        // Every time step, try to get the current state and compute the next set of segments.
        every(timeStep, () -> currentState.getDynamics().asOptional().ifPresent(state$ -> {
            try {
                var nextSegment = solver.nextSegment(fPrime, state$.data().extract(), currentTime(), timeStep);
                int i = 0;
                for (var result : results) {
                    set(result, nextSegment[i++]);
                }
            } catch (Throwable e) {
                // If we fail to compute the next segment, catch at the task level and re-throw in each resource.
                // This will propagate the failure at the resource level without crashing the simulation.
                // In particular, this will cause currentState to fail, thereby preventing future RK4 iterations.
                for (var result : results) {
                    result.emit("ODE Solver Failure", effect($ -> {throw e;}));
                }
            }
        }));
        // Finally, remove the "Mutable" part from the type using a no-op
        return results.stream().<Resource<Linear>>map($ -> $).toList();
    }

    /**
     * Implements Euler integration, suitable for use with {@link DifferentialEquations#firstOrderODE}.
     * <p>
     *     Note: This is implemented primarily as an example for other developers to follow, if they
     *     would like to implement a {@link FirstOrderODEIntegrator}.
     *     Euler integration is numerically unstable in most problems, and {@link DifferentialEquations#rungeKutta4()}
     *     is usually a much better choice.
     * </p>
     */
    public static FirstOrderODEIntegrator euler() {
        return (fPrime, y0, t0, timeStep) -> linearArray(y0, fPrime.apply(t0, y0));
    }

    /**
     * Implements RK4 integration, suitable for use with {@link DifferentialEquations#firstOrderODE}
     * <p>
     *     Note: RK4 integration is fast, stable, and suitable for most problems.
     *     If you don't know which {@link FirstOrderODEIntegrator} to choose, choose this.
     * </p>
     */
    public static FirstOrderODEIntegrator rungeKutta4() {
        return (fPrime, y0, t0, timeStep) -> {
            double dt = timeStep.ratioOver(Duration.SECONDS);
            Duration t1 = t0.plus(timeStep.dividedBy(2));
            Duration t2 = t0.plus(timeStep);
            double[] k1 = fPrime.apply(t0, y0);
            double[] k2 = fPrime.apply(t1, add(y0, multiply(k1, dt/2)));
            double[] k3 = fPrime.apply(t1, add(y0, multiply(k2, dt/2)));
            double[] k4 = fPrime.apply(t2, add(y0, multiply(k3, dt)));
            double[] kFinal = multiply(add(k1, k2, k2, k3, k3 ,k4), 1.0/6);
            return linearArray(y0, kFinal);
        };
    }

    private static double[] add(double[]... xs) {
        int n = xs[0].length;
        var result = new double[n];
        // result is automatically initialized to all-0
        for (var x : xs) {
            for (int i = 0; i < x.length; i++) {
                result[i] += x[i];
            }
        }
        return result;
    }

    private static double[] multiply(double[] x, double y) {
        var result = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            result[i] = x[i] * y;
        }
        return result;
    }

    private static Linear[] linearArray(double[] y0, double[] k) {
        var results = new Linear[y0.length];
        for (int i = 0; i < y0.length; i++) {
            results[i] = linear(y0[i], k[i]);
        }
        return results;
    }
}
