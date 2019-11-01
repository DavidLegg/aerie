package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.SettableState;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A bare-bones state that can be set and got, but only supports non-current forward
 * simulation
 * <p>
 * The basic state only keeps track of one value object at a time: that provided by the of
 * the most recent set() call, in actual java execution order, irrespective of actual
 * simulation time of the calls. This is enough to support the bare bones single-threaded
 * simulation engine that only steps forward in time in lockstep with the individual
 * activity effect models.
 *
 * <p>
 * Example creation of a BasicState object:
 * <code>
 * SettableState<Integer> state = new SettableState<>("my name", value);
 * </code>
 *
 * @param <T> the datatype of both the observable value of the state and the values that
 *            can be assigned to the state
 */


public class BasicState<T> implements SettableState<T> {

    /**
     * creates a new named state with an initial value
     * <p>
     * The default value is used absent any additional information about the state's
     * values as may be provided by an initial condition file, etc. This prevents a get()
     * call from returning null on an uninitialized state.
     *
     * @param name  human-legible name of state, as may be used for output. may include
     *              scoping for subsystems, eg: <code>clipper.uvs.power.heaterOpMode</code>
     * @param value default value of the state absent any initial conditions
     */
    public BasicState(String name, T value) {
        this.value = value;
    }


    /**
     * returns the most recent value this state has been set() to
     * <p>
     * only works in a non-concurrent forward simulation: it literally returns the most
     * recent (in java execution order) assigned value, irrespective of the simulation
     * time of those sets() and this get()
     *
     * @return most recently assigned value of state, in java execution order
     */
    @Override
    public T get() {
        return value;
    }

    /**
     * assigns a new value to the state, replacing the old value
     *
     * @param value new value to assign to state
     */
    @Override
    public void set(T value) {
        this.value = value;
        //this variety of state version is only used for the time being
        //this will instead integrate with Amruta's work
        //this is necessary for sim engine to work
        stateHistory.put(engine.getCurrentSimulationTime(), value);
    }


    /**
     * This is a temporary method used to enable the current SimulationEngine unit tests
     * to compile and run without modification (yet). It is used to time-tag state
     * assignments so that the history can be reported to the unit tests for the
     * simulation engine.
     *
     * @param engine the controlling simulation engine
     */
    @Override
    public void setEngine(SimulationEngine<?> engine) {
        this.engine = engine;
    }

    /**
     * this is a temporary method in order to integrate w/ the current SimulationEngine
     * unit tests
     * <p>
     * the unit tests currently need a way to inspect the state history directly
     */
    @Override
    public Map<Time, T> getHistory() {
        return stateHistory;
    }


    /**
     * the current value assigned to the state
     */
    private T value;

    /**
     * the engine that this state is registered to
     * <p>
     * used temporarily to support the initial implementation of the simulation engine
     */
    private SimulationEngine<?> engine;

    /**
     * history of values that the state has been assigned to, indexec by the simulation
     * timestamp of each set() call
     */
    private Map<Time, T> stateHistory = new LinkedHashMap<>();


}