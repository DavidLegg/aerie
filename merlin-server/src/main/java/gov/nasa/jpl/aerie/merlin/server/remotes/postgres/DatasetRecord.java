package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;

public final record DatasetRecord(
    long id,
    long revision,
    String state,
    String reason,
    boolean canceled,
    long planId,
    Duration offsetFromPlanStart,
    String profileSegmentPartitionTable,
    String spanPartitionTable) {}