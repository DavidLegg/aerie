import type { APIRequestContext } from '@playwright/test';
import FastGlob from 'fast-glob';
import { createReadStream } from 'fs';
import { basename, resolve } from 'path';
import * as urls from '../utilities/urls.js';
import gql from './gql.js';
import time from './time.js';

/**
 * Aerie API request functions.
 */
const req = {
  async createMissionModel(request: APIRequestContext, model: MissionModelInsertInput, headers?: Record<string, string>): Promise<number> {
    const data = await req.hasura(request, gql.CREATE_MISSION_MODEL, { model: model }, headers);
    const { insert_mission_model_one } = data;
    const { id: mission_model_id } = insert_mission_model_one;

    return mission_model_id;
  },

  async deleteMissionModel(request: APIRequestContext, id: number): Promise<number> {
    const data = await req.hasura(request, gql.DELETE_MISSION_MODEL, { id: id });
    const { delete_mission_model_by_pk } = data;
    const { id: deleted_mission_model_id } = delete_mission_model_by_pk;
    return deleted_mission_model_id;
  },

  async hasura<T = any>(
    request: APIRequestContext,
    query: string,
    variables: Record<string, unknown> = {},
    headers: Record<string, string> = {'x-hasura-role': 'aerie_admin', 'x-hasura-user-id': 'Aerie Legacy'},
  ): Promise<T> {
    const hasuraAdminSecret = (process.env['HASURA_GRAPHQL_ADMIN_SECRET'] as string) ?? '';
    const option_headers = { 'x-hasura-admin-secret': hasuraAdminSecret }
    for (const key in headers) {
      option_headers[key] = headers[key];
    }
    const options = { headers: option_headers, data: { query, variables } };
    const response = await request.post(`${urls.HASURA_URL}/v1/graphql`, options);

    if (response.ok()) {
      const json = await response.json();

      if (json?.data) {
        const { data } = json;
        return data as T;
      } else if (json?.errors) {
        console.error(json.errors);
        const [{ message }] = json.errors;
        throw new Error(message);
      } else {
        throw new Error('An unexpected error ocurred');
      }
    } else {
      throw new Error(response.statusText());
    }
  },

  async healthGateway(request: APIRequestContext): Promise<boolean> {
    const response = await request.get(`${urls.GATEWAY_URL}/health`);
    return response.ok();
  },

  async healthHasura(request: APIRequestContext): Promise<boolean> {
    const response = await request.get(`${urls.HASURA_URL}/healthz`);
    return response.ok();
  },

  async healthUI(request: APIRequestContext): Promise<boolean> {
    const response = await request.get(`${urls.UI_URL}/health`);
    return response.ok();
  },

  async healthMerlin(request: APIRequestContext): Promise<boolean> {
    const response = await request.get(`${urls.MERLIN_URL}/health`);
    return response.ok();
  },

  async healthScheduler(request: APIRequestContext): Promise<boolean> {
    const response = await request.get(`${urls.SCHEDULER_URL}/health`);
    return response.ok();
  },

  async healthSequencing(request: APIRequestContext): Promise<boolean> {
    const response = await request.get(`${urls.SEQUENCING_URL}/health`);
    return response.ok();
  },

  async healthWorker(request: APIRequestContext): Promise<boolean> {
    const workerOneResponse = await request.get(`${urls.WORKER_1_URL}/health`);
    const workerTwoResponse = await request.get(`${urls.WORKER_2_URL}/health`);
    return workerOneResponse.ok() && workerTwoResponse.ok();
  },

  /**
   * @param searchPath is relative to the e2e-tests directory. Defaults to Banananation.
   */
  async uploadJarFile(
    request: APIRequestContext,
    searchPath: string = '../examples/banananation/build/libs/*',
  ): Promise<number> {
    const absoluteSearchPath = resolve(searchPath);
    const [jarPath = 'ERROR_JAR_NOT_FOUND'] = FastGlob.sync(absoluteSearchPath);

    const buffer = createReadStream(jarPath);
    const name = basename(jarPath);
    const mimeType = 'application/java-archive';
    const multipart = { buffer, mimeType, name };
    const response = await request.post(`${urls.GATEWAY_URL}/file`, { multipart });

    if (response.ok()) {
      const json = await response.json();

      if (json?.id !== undefined) {
        const { id: jar_id } = json;
        return jar_id;
      } else if (json?.success === false) {
        console.error(json);
        throw new Error(json.message);
      } else {
        throw new Error('An unexpected error ocurred');
      }
    } else {
      throw new Error(response.statusText());
    }
  },

  async createPlan(request: APIRequestContext, model: CreatePlanInput, headers?: Record<string, string>): Promise<number> {
    const data = await req.hasura(request, gql.CREATE_PLAN, { plan: model }, headers);
    const { insert_plan_one } = data;
    const { id: plan_id } = insert_plan_one;
    return plan_id;
  },

  async getSimulationId(request: APIRequestContext, planId: number) {
    const data = await req.hasura(request, gql.GET_SIMULATION_ID, { plan_id: planId });
    const { simulation } = data;
    const { id: simulationId } = simulation.pop();
    return simulationId as number;
  },

  async getSimulationDataset(request: APIRequestContext, simulationDatasetId: number) {
    const data = await req.hasura(request, gql.GET_SIMULATION_DATASET, { id: simulationDatasetId });
    const { simulationDataset } = data;
    return simulationDataset as SimulationDataset;
  },

  async getSimulationDatasetByDatasetId(request: APIRequestContext, simulationDatasetId: number) {
    const data = await req.hasura(request, gql.GET_SIMULATION_DATASET_BY_DATASET_ID, { id: simulationDatasetId });
    const { simulation_dataset } = data;
    return simulation_dataset[0] as SimulationDataset;
  },

  async insertSpan(
      request: APIRequestContext,
      parentId: number,
      duration: string,
      simulationDatasetId: number,
      type: string,
      startOffset: string,
      attributes: any){
    //note the empty headers: required to act as hasura admin role to be able to insert in these tables
    const data = await req.hasura(request, gql.INSERT_SPAN, {
      parentId: parentId,
      duration: duration,
      datasetId: simulationDatasetId,
      type: type,
      startOffset: startOffset,
      attributes: attributes},{});
    const { insert_span_one } = data;
    const { id } = insert_span_one;
    return id;
  },

  async insertSimulationDataset(
      request: APIRequestContext,
      simulationId: number,
      simulationStartTime: string,
      simulationEndTime:string,
      status:string,
      simulationArguments:ArgumentsMap,
      planRevision: number

  ){
    //note the empty headers: required to act as hasura admin role to be able to insert in these tables
    const data = await req.hasura(request, gql.INSERT_SIMULATION_DATASET, {
      simulationDatasetInsertInput : {
        simulation_id: simulationId,
        simulation_start_time:simulationStartTime,
        simulation_end_time: simulationEndTime,
        status:status,
        arguments: simulationArguments,
        plan_revision: planRevision
      }
    },{});
    const { insert_simulation_dataset_one } = data;
    const { dataset_id : datasetId } = insert_simulation_dataset_one;
    return datasetId;
  },

  async insertAndAssociateSimulationTemplate(
    request: APIRequestContext,
    template: InsertSimulationTemplateInput,
    simulationId: number,
  ) {
    const data = await req.hasura(request, gql.INSERT_SIMULATION_TEMPLATE, { simulationTemplateInsertInput: template });
    const { insert_simulation_template_one } = data;
    const { id: template_id } = insert_simulation_template_one;
    await req.hasura(request, gql.ASSIGN_TEMPLATE_TO_SIMULATION, {
      simulation_id: simulationId,
      simulation_template_id: template_id,
    });
    return template_id;
  },

  async updateSimulationBounds(request: APIRequestContext, bounds: UpdateSimulationBoundsInput) {
    const { plan_id, simulation_start_time, simulation_end_time } = bounds;
    const data = await req.hasura(request, gql.UPDATE_SIMULATION_BOUNDS, {
      plan_id: plan_id,
      simulation_start_time: simulation_start_time,
      simulation_end_time: simulation_end_time,
    });
    const { update_simulation } = data;
    const { id } = update_simulation;
    return id;
  },

  async insertSchedulingGoal(request: APIRequestContext, schedulingInput: SchedulingGoalInsertInput) {
    const data = await req.hasura(request, gql.CREATE_SCHEDULING_GOAL, { goal: schedulingInput });
    const { insert_scheduling_goal_one } = data;
    const { id: goal_id } = insert_scheduling_goal_one;
    return goal_id;
  },
  async insertSchedulingSpecification(request: APIRequestContext, specificationInput: SchedulingSpecInsertInput) {
    const data = await req.hasura(request, gql.INSERT_SCHEDULING_SPECIFICATION, {
      scheduling_spec: specificationInput,
    });
    const { insert_scheduling_specification_one } = data;
    const { id: spec_id } = insert_scheduling_specification_one;
    return spec_id;
  },

  async getPlanRevision(request: APIRequestContext, id: number): Promise<number | null> {
    const data = await req.hasura(request, gql.GET_PLAN_REVISION, { id: id });
    const { plan } = data;
    const { revision } = plan;
    return revision;
  },

  async schedule(request: APIRequestContext, specificationId: number): Promise<SchedulingResponse> {
    const data = await req.hasura(request, gql.SCHEDULE, {
      specificationId: specificationId,
    });
    const { schedule } = data;
    return schedule;
  },

  async simulate(request: APIRequestContext, planId: number): Promise<SimulationResponse> {
    const data = await req.hasura(request, gql.SIMULATE, {
      plan_id: planId,
    });
    const { simulate } = data;
    return simulate;
  },

  async createSchedulingSpecGoal(request: APIRequestContext, spec_goal: SchedulingSpecGoalInsertInput): Promise<void> {
    const data = await req.hasura(request, gql.CREATE_SCHEDULING_SPEC_GOAL, { spec_goal });
    const { insert_scheduling_specification_goals_one } = data;
    const {
      goal_id: goal_id,
      priority: priority,
      specification_id: specification_id,
    } = insert_scheduling_specification_goals_one;
    return specification_id;
  },

  async getSchedulingDslTypeScript(
    request: APIRequestContext,
    missionModelId: number,
    planId?: number
  ): Promise<SchedulingDslTypesResponse> {
    const data = await req.hasura(request, gql.GET_SCHEDULING_DSL_TYPESCRIPT, { missionModelId: missionModelId, planId: planId });
    const { schedulingDslTypescript } = data;
    return schedulingDslTypescript;
  },

  async deletePlan(request: APIRequestContext, id: number) {
    const data = await req.hasura(request, gql.DELETE_PLAN, { id: id });
    const { deletePlan } = data;
    const { id: deletedPlan } = deletePlan;
    return deletedPlan;
  },

  async getPlan(request: APIRequestContext, id: number): Promise<Plan> {
    const data = await req.hasura<{ plan: Plan }>(request, gql.GET_PLAN, { id: id });
    const { plan } = data;
    const startTime = new Date(plan.startTime);
    return {
      ...plan,
      activity_directives: plan.activity_directives.map((activity: any) => toActivity(activity, startTime)),
      endTime: time.getDoyTimeFromDuration(startTime, plan.duration),
      startTime: time.getDoyTime(startTime),
    };
  },

  async insertActivity(request: APIRequestContext, activityInsertInput: ActivityInsertInput) {
    const data = await req.hasura(request, gql.CREATE_ACTIVITY_DIRECTIVE, {
      activityDirectiveInsertInput: activityInsertInput,
    });
    const { createActivityDirective } = data;
    const { id: idCreatedActivity } = createActivityDirective;
    return idCreatedActivity;
  },

  async deleteActivity(request: APIRequestContext, plan_id: number, activity_id: number): Promise<number> {
    const data = await req.hasura(request, gql.DELETE_ACTIVITY_DIRECTIVE, { plan_id, id: activity_id });
    const { delete_activity_directive_by_pk } = data;
    const { id } = delete_activity_directive_by_pk;
    return id;
  },

  async insertExternalDataset(request: APIRequestContext, input: ExternalDatasetInsertInput): Promise<number> {
    const data = await req.hasura(request, gql.ADD_EXTERNAL_DATASET, input);
    const { addExternalDataset } = data;
    const { datasetId } = addExternalDataset;

    return datasetId;
  },

  async extendExternalDataset(request: APIRequestContext, input: ExternalDatasetExtendInput): Promise<number> {
    const data = await req.hasura(request, gql.EXTEND_EXTERNAL_DATASET, input);
    const { extendExternalDataset } = data;
    const { datasetId } = extendExternalDataset;

    return datasetId;
  },

  async getExternalDataset(request: APIRequestContext, input: ExternalDatasetQueryInput) {
    return await req.hasura(request, gql.GET_EXTERNAL_DATASET, input);
  },

  async deleteExternalDataset(request: APIRequestContext, input: ExternalDatasetQueryInput) {
    const data = await req.hasura(request, gql.DELETE_EXTERNAL_DATASET, input);
    const { delete_plan_dataset_by_pk } = data;
    const { dataset_id } = delete_plan_dataset_by_pk;
    return dataset_id;
  },

  async getProfiles(request: APIRequestContext, datasetId: number) {
    const data = await req.hasura(request, gql.GET_PROFILES, { datasetId: datasetId });
    const { profile } = data;
    return profile;
  },

  async getTopicsEvents(request: APIRequestContext, datasetId: number) {
    const data = await req.hasura(request, gql.GET_TOPIC_EVENTS, { datasetId: datasetId });
    const { topic } = data;
    return topic;
  },

  async getResourceTypes(request: APIRequestContext, missionModelId: number): Promise<ResourceType[]> {
    const data = await req.hasura(request, gql.GET_RESOURCE_TYPES, { missionModelId: missionModelId });
    const { resource_type } = data;
    return <ResourceType[]>resource_type;
  },

  async getActivityTypes(request: APIRequestContext, missionModelId: number): Promise<ActivityType[]> {
    const data = await req.hasura(request, gql.GET_ACTIVITY_TYPES, { missionModelId: missionModelId });
    const { activity_type } = data;
    return <ActivityType[]>activity_type;
  },

  async insertConstraint(request: APIRequestContext, constraint: ConstraintInsertInput): Promise<number> {
    const data = await req.hasura(request, gql.INSERT_CONSTRAINT, { constraint });
    const { insert_constraint_one } = data;
    const { id } = insert_constraint_one;
    return id;
  },

  async insertProfile(request: APIRequestContext, datasetId: number, duration:string, name: string, type:object): Promise<number> {
    //note the empty headers: required to act as hasura admin role to be able to insert in these tables
    const data = await req.hasura(request, gql.INSERT_PROFILE, { datasetId, duration, name, type }, {});
    const { insert_profile_one } = data;
    const { id } = insert_profile_one;
    return id;
  },

  async insertProfileSegment(request: APIRequestContext, datasetId: number, dynamics:number, isGap: boolean, profileId:number, startOffset:string): Promise<number> {
    //note the empty headers: required to act as hasura admin role to be able to insert in these tables
    const data = await req.hasura(request, gql.INSERT_PROFILE_SEGMENT, { datasetId, dynamics, isGap, profileId, startOffset }, {});
    const { insert_profile_segment_one } = data;
    const { dataset_id } = insert_profile_segment_one;
    return dataset_id;
  },

  async updateConstraint(
    request: APIRequestContext,
    constraintId: number,
    constraintDefinition: string,
  ): Promise<string> {
    const data = await req.hasura(request, gql.UPDATE_CONSTRAINT, { constraintId, constraintDefinition });
    const { update_constraint } = data;
    const { definition } = update_constraint;
    return definition;
  },

  async checkConstraints(
    request: APIRequestContext,
    planId: number,
    simulationDatasetId?: number,
  ): Promise<ConstraintResult[]> {
    const data = await req.hasura(request, gql.CHECK_CONSTRAINTS, { planId, simulationDatasetId });
    const { constraintViolations } = data;
    return <ConstraintResult[]> constraintViolations;
  },

  async deleteConstraint(request: APIRequestContext, constraintId: number): Promise<number> {
    const data = await req.hasura(request, gql.DELETE_CONSTRAINT, { id: constraintId });
    const { delete_constraint_by_pk } = data;
    const { id } = delete_constraint_by_pk;
    return id;
  },

  async getEffectiveArguments(
    request: APIRequestContext,
    modelId: number,
    activityTypeName: string,
    activityArguments: any
  ): Promise<EffectiveArguments> {
    const data = await req.hasura(request, gql.GET_EFFECTIVE_ACTIVITY_ARGUMENTS, { modelId, activityTypeName, activityArguments });
    const { getActivityEffectiveArguments } = data;
    return getActivityEffectiveArguments;
  },

  async getEffectiveArgumentsBulk(
    request: APIRequestContext,
    modelId: number,
    activities: EffectiveArgumentItem[]
  ): Promise<EffectiveArguments[]> {
    const data = await req.hasura(request, gql.GET_EFFECTIVE_ACTIVITY_ARGUMENTS_BULK, { modelId, activities });
    const { getActivityEffectiveArgumentsBulk } = data;
    return <EffectiveArguments[]> getActivityEffectiveArgumentsBulk;
  },

  async getConstraintRuns(request: APIRequestContext, simulationDatasetId: number): Promise<ConstraintRun[]> {
    const data = await req.hasura(request, gql.GET_CONSTRAINT_RUNS, { simulationDatasetId });
    const { constraint_run } = data;
    return constraint_run;
  },

  // User-related requests
  async createUser(request: APIRequestContext, user: User): Promise<void> {
    const userInput: UserInsert = {username: user.username, default_role: user.default_role};
    const allowedRolesInput: UserAllowedRole[] = user.allowed_roles.map(role => { return {username: user.username, allowed_role: role}})
    await req.hasura(request, gql.CREATE_USER, { user: userInput, allowed_roles: allowedRolesInput } )
  },

  async deleteUser(request: APIRequestContext, username: string): Promise<void> {
    await req.hasura(request, gql.DELETE_USER, { username })
  },

  async addPlanCollaborator(request: APIRequestContext, username: string, planId: number): Promise<void> {
    const planCollaboratorInsertInput = {planId: planId, collaborator: username};
    await req.hasura(request, gql.ADD_PLAN_COLLABORATOR, { planCollaboratorInsertInput });
  },

  async getActionPermissionsForRole(request: APIRequestContext, role: string): Promise<ActionPermissionSet> {
    const data = await req.hasura(request, gql.GET_ROLE_ACTION_PERMISSIONS, { role });
    const { permissions } = data;
    const { action_permissions } = permissions;
    return action_permissions;
  },

  async updateActionPermissionsForRole(request: APIRequestContext, role: string, permissions: ActionPermissionSet): Promise<void> {
    const strippedPermissions = Object.fromEntries(Object.entries(permissions).filter(([_, v]) => v != null));
    await req.hasura(request, gql.UPDATE_ROLE_ACTION_PERMISSIONS, { role: role, action_permissions: strippedPermissions });
  }
};
/**
 * Converts any activity to an Activity.
 */
export function toActivity(activity: any, startTime: Date): ActivityDirective {
  return {
    arguments: activity.arguments,
    children: [],
    duration: 0,
    id: activity.id,
    parent: null,
    startTime: time.getDoyTimeFromDuration(startTime, activity.startOffset),
    type: activity.type,
  };
}

export async function awaitSimulation(request: APIRequestContext, plan_id: number): Promise<SimulationResponse> {
  const max_iter = 10;
  for (let i = 0; i < max_iter; i++) {
    const resp = await req.simulate(request, plan_id);
    const { reason, status } = resp;

    switch (status) {
      case 'pending':
      case 'incomplete':
        await time.delay(1000);
        break;
      case 'complete':
        return resp;
      default:
        throw Error(`Simulation returned bad status: ${status} with reason ${reason}`);
    }
  }

  throw Error(`Simulation timed out after ${max_iter} iterations`);
}

export async function awaitScheduling(request: APIRequestContext, scheduling_specification_id: number): Promise<SchedulingResponse> {
  const max_iter = 10;
  for (let i = 0; i < max_iter; i++) {
    const resp = await req.schedule(request, scheduling_specification_id);
    const { reason, status } = resp;

    switch (status) {
      case 'pending':
      case 'incomplete':
        await time.delay(1000);
        break;
      case 'complete':
        return resp;
      default:
        throw Error(`Scheduling returned bad status: ${status} with reason ${reason}`);
    }
  }

  throw Error(`Scheduling timed out after ${max_iter} iterations`);
}

export default req;
