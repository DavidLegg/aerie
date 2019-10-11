package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controllers;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.InvalidAdaptationJARException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchActivityTypeException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes.AdaptationRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.utilities.AdaptationLoader;
import gov.nasa.jpl.ammos.mpsa.aerie.aeriesdk.MissingAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class AdaptationController implements IAdaptationController {
    private final AdaptationRepository adaptationRepository;

    public AdaptationController(final AdaptationRepository adaptationRepository) {
        this.adaptationRepository = adaptationRepository;
    }

    @Override
    public Stream<Pair<String, Adaptation>> getAdaptations() {
        return this.adaptationRepository.getAllAdaptations();
    }

    @Override
    public Adaptation getAdaptationById(String id) throws NoSuchAdaptationException {
        return this.adaptationRepository.getAdaptation(id);
    }

    @Override
    public String addAdaptation(NewAdaptation adaptation) throws ValidationException {
        validateAdaptation(adaptation);
        return this.adaptationRepository.createAdaptation(adaptation);
    }

    @Override
    public void removeAdaptation(String id) throws NoSuchAdaptationException {
        this.adaptationRepository.deleteAdaptation(id);
    }

    @Override
    public Stream<Pair<String, ActivityType>> getActivityTypes(String adaptationId) throws NoSuchAdaptationException {
        try {
            return this.adaptationRepository.getAllActivityTypesInAdaptation(adaptationId);
        } catch (InvalidAdaptationJARException e) {
            throw new Error(e);
        }
    }

    @Override
    public ActivityType getActivityType(String adaptationId, String activityTypeId) throws NoSuchAdaptationException, NoSuchActivityTypeException {
        try {
            return this.adaptationRepository.getActivityTypeInAdaptation(adaptationId, activityTypeId);
        } catch (InvalidAdaptationJARException e) {
            throw new Error(e);
        }
    }

    @Override
    public Map<String, ParameterSchema> getActivityTypeParameters(String adaptationId, String activityTypeId) throws NoSuchAdaptationException, NoSuchActivityTypeException {
        try {
            return this.adaptationRepository.getActivityTypeParameters(adaptationId, activityTypeId);
        } catch (InvalidAdaptationJARException e) {
            throw new Error(e);
        }
    }

    private void validateAdaptation(final NewAdaptation adaptation) throws ValidationException {
        final List<String> validationErrors = new ArrayList<>();

        if (adaptation.name == null) validationErrors.add("name must be non-null");
        if (adaptation.version == null) validationErrors.add("version must be non-null");

        if (adaptation.path == null) {
            validationErrors.add("path must be non-null");
        } else {
            Map<String, ActivityType> activities = null;
            try {
                activities = AdaptationLoader.loadActivities(adaptation.path);
            } catch (final MissingAdaptationException e) {
                validationErrors.add("Adaptation JAR does not contain a class implementing the Adaptation interface");
            }

            if (activities != null && activities.size() < 1) validationErrors.add("No activities found. Must include at least one activity");
        }


        if (validationErrors.size() > 0) {
            throw new ValidationException("invalid adaptation", validationErrors);
        }
    }
}