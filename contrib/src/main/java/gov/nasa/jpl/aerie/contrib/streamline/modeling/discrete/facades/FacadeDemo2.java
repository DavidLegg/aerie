package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.facades;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.increment;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.facades.SettableState2.settableState;

public class FacadeDemo2 {
  CellResource<Discrete<Integer>> explicitBase = cellResource(discrete(4));
  // This is a facade over the cell resource above; they "point to" the same resource, and will change in sync.
  SettableState2<Integer> explicitFacade = settableState(explicitBase);
  // This is a separate resource; it has its own cell resource inside.
  SettableState2<Integer> implicitFacade = settableState(4);

  {
    // Imagine this is activity code...

    // Regular set/get operation:
    explicitFacade.set(5);
    Integer x = explicitFacade.get();

    implicitFacade.set(5);
    Integer y = implicitFacade.get();

    // Irregular operation:

    // The only way to do irregular operations to the more restricted facade
    // is to do them to the base cell resource
    increment(explicitBase);
    // increment(explicitFacade); // Does not compile

    // This is no longer possible
    // increment(implicitFacade); // Does not compile
    // Since the implicit facade hides that cell resource, you have to do this instead:
    implicitFacade.set(implicitFacade.get() + 1);
    // Besides being uglier, this will misbehave in concurrent settings
  }
}
