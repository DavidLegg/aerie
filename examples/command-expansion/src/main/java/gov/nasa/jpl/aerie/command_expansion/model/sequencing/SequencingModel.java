package gov.nasa.jpl.aerie.command_expansion.model.sequencing;

import gov.nasa.jpl.aerie.command_expansion.expansion.Sequence;
import gov.nasa.jpl.aerie.command_expansion.model.Mission;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;

import java.util.List;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;

// Once again, this is a mission-specific model, but we can provide off-the-shelf versions for VML & FCPL.
public class SequencingModel {
    private final Mission mission;
    public List<FcplSequenceEngine> sequenceEngines;

    public SequencingModel(Registrar registrar, Mission mission) {
        this.mission = mission;
    }

    // TODO - complications like reserved engines, assigned engines, load vs. activate distinction, etc.
    public String modelSequence(Sequence sequence) {
        var engine = sequenceEngines.stream()
                .filter($ -> !currentValue($.isLoaded()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No empty sequence engines available!"));

        engine.load(sequence);
        engine.execute();

        return sequence.toSeqJson().serialize();
    }
}
