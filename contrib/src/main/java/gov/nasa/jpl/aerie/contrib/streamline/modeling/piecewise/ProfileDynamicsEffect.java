package gov.nasa.jpl.aerie.contrib.streamline.modeling.piecewise;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;

/**
 * General interface for an effect applied to a {@link MutableResource}
 */
public interface ProfileDynamicsEffect<D extends Dynamics<?, D>> {
    ErrorCatching<Profile<D>> apply(ErrorCatching<Profile<D>> dynamics);
}
