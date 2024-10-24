package gov.nasa.jpl.aerie.command_expansion.command_activities;

import gov.nasa.jpl.aerie.command_expansion.model.Mission;

import java.util.List;

public interface Command {
    default String stem() {
        return this.getClass().getSimpleName();
    }

    default List<Object> args() {
        return List.of();
    }

    // By default, command activities are not modeled.
    default void run(Mission mission) {}

    // Visitor-like pattern, defers spawn call to each concrete activity,
    // which can statically call the ActivityActions.spawn overload for that concrete activity type.
    void call(Mission mission);
}
