package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.facades;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.increment;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.facades.SettableState1.settableState;

public class FacadeDemo1 {
  CellResource<Discrete<Integer>> explicitBase = cellResource(discrete(4));
  // This is a facade over the cell resource above; they "point to" the same resource, and will change in sync.
  SettableState1<Integer> explicitFacade = settableState(explicitBase);
  // This is a separate resource; it has its own cell resource inside.
  SettableState1<Integer> implicitFacade = settableState(4);

  {
    // Imagine this is activity code...

    // Regular set/get operation:
    explicitFacade.set(5);
    Integer x = explicitFacade.get();

    implicitFacade.set(5);
    Integer y = implicitFacade.get();

    // Irregular operation:

    // These two are exactly equivalent:
    increment(explicitBase);
    increment(explicitFacade);

    // This is also possible:
    increment(implicitFacade);
  }
}
