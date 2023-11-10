package gov.nasa.jpl.aerie.streamline_demo.models;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.EnumValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.StringValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.locks.Lock;
import gov.nasa.jpl.aerie.streamline_demo.Configuration;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;

public class LockModel {
  public Lock<Priority> lock = new Lock<>();
  public CellResource<Discrete<String>> lockHolder = cellResource(discrete(""));
  public CellResource<Discrete<Priority>> lockHolderPriority = cellResource(discrete(null));
  public CellResource<Discrete<Integer>> pendingLockRequests = cellResource(discrete(0));

  public LockModel(final Registrar registrar, final Configuration config) {
    registrar.discrete("lockHolder", lockHolder, new StringValueMapper());
    registrar.discrete("lockHolderPriority", lockHolderPriority, new EnumValueMapper<>(Priority.class));
    registrar.discrete("pendingLockRequests", pendingLockRequests, new IntegerValueMapper());
  }

  public enum Priority {
    LOW,
    MEDIUM,
    HIGH
  }
}
