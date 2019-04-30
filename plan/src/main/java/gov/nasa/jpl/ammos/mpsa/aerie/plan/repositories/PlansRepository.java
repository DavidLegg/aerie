package gov.nasa.jpl.ammos.mpsa.aerie.plan.repositories;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.PlanDetail;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlansRepository extends MongoRepository<Plan, String> {
    PlanDetail findPlanDetailById(String id);
}
