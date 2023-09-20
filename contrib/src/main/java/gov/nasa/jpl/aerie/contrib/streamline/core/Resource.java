package gov.nasa.jpl.aerie.contrib.streamline.core;

public interface Resource<D> {
  ErrorCatching<Expiring<D>> getDynamics();
}
