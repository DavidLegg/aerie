package gov.nasa.jpl.aerie.contrib.streamline.core;

public interface Resource<D> {
  Expiring<D> getDynamics();
}
