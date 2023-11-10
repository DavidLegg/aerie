@MissionModel(model = Mission.class)

@WithConfiguration(Configuration.class)

@WithMappers(BasicValueMappers.class)

@WithActivityType(ChangeDesiredRate.class)
@WithActivityType(CauseError.class)
@WithActivityType(HoldLock.class)

package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithConfiguration;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithMappers;
import gov.nasa.jpl.aerie.streamline_demo.activities.CauseError;
import gov.nasa.jpl.aerie.streamline_demo.activities.ChangeDesiredRate;
import gov.nasa.jpl.aerie.streamline_demo.activities.HoldLock;
