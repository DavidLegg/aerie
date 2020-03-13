package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;

class ActivityJob<T extends StateContainer> {

    // TODO: detach threads for garbage collection?

    public enum ActivityStatus {
        NotStarted, InProgress, Complete
    }

    /**
     * The activity instance that this job "owns"
     */
    private Activity<T> activity;

    /**
     * Serves as a bridge between this job and the engine. It is passed down to the activity's effect model as the
     * `SimulationContext` type to prevent adapter access to certain engine-related methods.
     */
    private SimulationEngine.JobContext ctx;

    /**
     * A means of synchronously passing execution control between this job's thread and others (primarily the engine).
     */
    private ControlChannel channel;

    /**
     * The adapter-provided state index structure
     */
    private StateContainer stateContainer;

    private ActivityStatus status = ActivityStatus.NotStarted;

    public ActivityJob(Activity<T> activityInstance) {
        this.activity = activityInstance;
    }

    /**
     * Executes the activity's effect model in coordination with the engine
     * 
     * This method will execute an activity's effect model, block until all child activities are complete, log the
     * duration of the effect model within an engine-level map, and notify the activity threads that are listening on
     * this target. A `ControlChannel` is used to ensure that the engine is not executing at the same time; execution
     * control is explicitly handed back-and-forth between the engine and activity job's thread via yields and takes.
     */
    public void execute() {
        this.channel.takeControl();

        this.status = ActivityStatus.InProgress;
        SimulationEffects.withEffects(this.ctx, () -> {
            activity.modelEffects((T) this.stateContainer);
            SimulationEffects.waitForChildren();
        });
        this.status = ActivityStatus.Complete;

        this.ctx.notifyActivityListeners();
        this.channel.yieldControl();
    }

    /**
     * Suspends the activity job's thread until execution control is explicitly given back to it
     */
    public synchronized void suspend() {
        this.channel.yieldControl();
        this.channel.takeControl();
    }

    public Activity<T> getActivity() {
        return this.activity;
    }

    public void setContext(SimulationEngine.JobContext ctx) {
        this.ctx = ctx;
    }

    public ControlChannel getChannel() {
        return this.channel;
    }

    public void setChannel(ControlChannel channel) {
        this.channel = channel;
    }

    public void setStates(StateContainer stateContainer) {
        this.stateContainer = stateContainer;
    }

    public String toString() {
        // TODO: map to actual name in the future
        return activity.getClass().getName();
    }

    public ActivityStatus getStatus() {
        return this.status;
    }

}
