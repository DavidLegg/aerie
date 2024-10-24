@MissionModel(model = Mission.class)

@WithConfiguration(Configuration.class)

@WithMappers(BasicValueMappers.class)

@WithActivityType(PWR_Turn_On_Heater.class)
@WithActivityType(PWR_Turn_Off_Heater.class)
@WithActivityType(Warm_Up.class)

package gov.nasa.jpl.aerie.command_expansion;

import gov.nasa.jpl.aerie.command_expansion.command_activities.PWR_Turn_Off_Heater;
import gov.nasa.jpl.aerie.command_expansion.command_activities.PWR_Turn_On_Heater;
import gov.nasa.jpl.aerie.command_expansion.model.Mission;
import gov.nasa.jpl.aerie.command_expansion.planning_activities.Warm_Up;
import gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithConfiguration;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithMappers;
