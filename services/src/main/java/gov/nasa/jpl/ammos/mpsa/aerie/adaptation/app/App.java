package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchActivityTypeException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.UnconstructableActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationJar;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.utilities.AdaptationLoader;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;

import java.util.List;
import java.util.Map;

public interface App {
    Map<String, AdaptationJar> getAdaptations();

    String addAdaptation(NewAdaptation adaptation)
        throws AdaptationRejectedException;
    AdaptationJar getAdaptationById(String adaptationId)
        throws NoSuchAdaptationException;
    void removeAdaptation(String adaptationId)
        throws NoSuchAdaptationException;

    Map<String, ActivityType> getActivityTypes(String adaptationId)
        throws NoSuchAdaptationException, AdaptationLoader.AdaptationLoadException, Adaptation.AdaptationContractException;
    ActivityType getActivityType(String adaptationId, String activityTypeId)
        throws NoSuchAdaptationException, AdaptationLoader.AdaptationLoadException, Adaptation.AdaptationContractException,
        NoSuchActivityTypeException;
    List<String> validateActivityParameters(String adaptationId, SerializedActivity activityParameters)
        throws NoSuchAdaptationException, AdaptationLoader.AdaptationLoadException, Adaptation.AdaptationContractException,
        NoSuchActivityTypeException, UnconstructableActivityInstanceException;

    class AdaptationRejectedException extends Exception {
        public AdaptationRejectedException(final String message) { super(message); }
        public AdaptationRejectedException(final Throwable cause) { super(cause); }
    }

    class NoSuchAdaptationException extends Exception {
        private final String id;

        public NoSuchAdaptationException(final String id, final Throwable cause) {
            super("No adaptation exists with id `" + id + "`", cause);
            this.id = id;
        }

        public NoSuchAdaptationException(final String id) { this(id, null); }

        public String getInvalidAdaptationId() { return this.id; }
    }
}
