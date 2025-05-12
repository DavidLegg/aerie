package gov.nasa.jpl.aerie.contrib.streamline.modeling.time_systems;

// REVIEW:
//   It would be nice if the time were either completely transparent to the system being used,
//   or else enforce a particular system in the type signature.
//   Here, we get a time of a certain representation type, which isn't necessarily a system...
public record Time<T>(T time, TimeSystem<T> system) {
    public <S> Time<S> convertTo(TimeSystem<S> otherSystem) {
        return new Time<>(this.system.conversionTo(otherSystem).apply(time), otherSystem);
    }
}
