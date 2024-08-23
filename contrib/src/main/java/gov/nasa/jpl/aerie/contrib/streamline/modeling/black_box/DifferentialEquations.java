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
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear.linear;

public class DifferentialEquations {
    private DifferentialEquations() {}

    // TODO - Abstract this method, kind of like the approximation code.
    // Most of the machinery below is abstract first-order ODE solver code, and we ought to be able to swap out
    // just rk4Segment with another solver algorithm.
    public static List<Resource<Linear>> rungeKutta4(
            BiFunction<Duration, double[], double[]> fPrime,
            double[] initialConditions,
            Duration timeStep) {
        // Compute the initial segments
        var initialSegment = rk4Segment(fPrime, initialConditions, currentTime(), timeStep);
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
                var nextSegment = rk4Segment(fPrime, state$.data().extract(), currentTime(), timeStep);
                int i = 0;
                for (var result : results) {
                    set(result, nextSegment[i++]);
                }
            } catch (Throwable e) {
                // If we fail to compute the next segment, catch at the task level and re-throw in each resource.
                // This will propagate the failure at the resource level without crashing the simulation.
                // In particular, this will cause currentState to fail, thereby preventing future RK4 iterations.
                for (var result : results) {
                    result.emit("RK4 Failure", $ -> {throw e;});
                }
            }
        }));
        // Finally, remove the "Mutable" part from the type using a no-op
        return results.stream().<Resource<Linear>>map($ -> $).toList();
    }

    private static Linear[] rk4Segment(
            BiFunction<Duration, double[], double[]> fPrime,
            double[] y0,
            Duration t0,
            Duration timeStep) {
        double dt = timeStep.ratioOver(Duration.SECONDS);
        Duration t1 = t0.plus(timeStep.dividedBy(2));
        Duration t2 = t0.plus(timeStep);
        double[] k1 = fPrime.apply(t0, y0);
        double[] k2 = fPrime.apply(t1, add(y0, multiply(k1, dt/2)));
        double[] k3 = fPrime.apply(t1, add(y0, multiply(k2, dt/2)));
        double[] k4 = fPrime.apply(t2, add(y0, multiply(k3, dt)));
        Linear[] result = new Linear[y0.length];
        for (int i = 0; i < y0.length; i++) {
            result[i] = linear(y0[i], (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]) / 6);
        }
        return result;
    }

    private static double[] add(double[] x, double[] y) {
        var result = new double[x.length];
        // result is automatically initialized to all-0
        for (int i = 0; i < x.length; i++) {
            result[i] = x[i] + y[i];
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
}
