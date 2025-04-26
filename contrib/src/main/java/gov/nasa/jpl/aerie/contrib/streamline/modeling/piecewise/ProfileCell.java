package gov.nasa.jpl.aerie.contrib.streamline.modeling.piecewise;

import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;

public class ProfileCell<D> {
    // TODO: Optimize stepping
    //   Keep a reference to an "unstepped" profile, and always step from there.
    //   To save on computation, if we step past a segment boundary, record that as the new stepped profile instead.
    public ErrorCatching<Profile<D>> profile;

    public ProfileCell(ErrorCatching<Profile<D>> profile) {
        this.profile = profile;
    }
}
