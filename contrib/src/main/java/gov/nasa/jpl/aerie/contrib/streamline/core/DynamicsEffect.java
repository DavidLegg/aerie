package gov.nasa.jpl.aerie.contrib.streamline.core;

public interface DynamicsEffect<D extends Dynamics<?, D>> {
    Expiring<D> apply(Expiring<D> dynamics);
}
