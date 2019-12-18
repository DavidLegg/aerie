package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

public final class RemotePlanRepository implements PlanRepository {
  private final MongoCollection<Document> planCollection;
  private final MongoCollection<Document> activityCollection;

  public RemotePlanRepository(
      final URI serverAddress,
      final String databaseName,
      final String planCollectionName,
      final String activityCollectionName
  ) {
    final MongoDatabase database = MongoClients
        .create(serverAddress.toString())
        .getDatabase(databaseName);

    this.planCollection = database.getCollection(planCollectionName);
    this.activityCollection = database.getCollection(activityCollectionName);
  }

  public void clear() {
    this.planCollection.drop();
    this.activityCollection.drop();
  }

  @Override
  public Stream<Pair<String, Plan>> getAllPlans() {
    final var query = this.planCollection.find();

    return documentStream(query)
        .map(planDocument -> {
          final ObjectId planId = planDocument.getObjectId("_id");
          final FindIterable<Document> activityDocuments = this.activityCollection.find(
              activityByPlan(planId));
          final Plan plan = planFromDocuments(planDocument, activityDocuments);

          return Pair.of(planId.toString(), plan);
        });
  }

  @Override
  public Plan getPlan(final String planId) throws NoSuchPlanException {
    final Document planDocument = this.planCollection
        .find(planById(makePlanObjectId(planId)))
        .first();

    if (planDocument == null) {
      throw new NoSuchPlanException(planId);
    }

    final FindIterable<Document> activityDocuments = this.activityCollection
        .find(activityByPlan(makePlanObjectId(planId)));

    return planFromDocuments(planDocument, activityDocuments);
  }

  @Override
  public Stream<Pair<String, ActivityInstance>> getAllActivitiesInPlan(final String planId) throws NoSuchPlanException {
    ensurePlanExists(planId);

    return documentStream(this.activityCollection.find(activityByPlan(makePlanObjectId(planId))))
        .map(document -> Pair.of(
            document.getObjectId("_id").toString(),
            activityFromDocument(document)));
  }

  @Override
  public ActivityInstance getActivityInPlanById(final String planId, final String activityId) throws NoSuchPlanException, NoSuchActivityInstanceException {
    ensurePlanExists(planId);

    final Document document = this.activityCollection
        .find(activityById(makePlanObjectId(planId), makeActivityObjectId(planId, activityId)))
        .first();

    if (document == null) {
      throw new NoSuchActivityInstanceException(planId, activityId);
    }

    return activityFromDocument(document);
  }

  @Override
  public String createPlan(final NewPlan plan) {
    final String planId;
    {
      final Document planDocument = toDocument(plan);
      this.planCollection.insertOne(planDocument);
      planId = planDocument.getObjectId("_id").toString();
    }

    if (plan.activityInstances != null) {
      for (final var activity : plan.activityInstances) {
        this.activityCollection.insertOne(toDocument(planId, activity));
      }
    }

    return planId;
  }

  @Override
  public PlanTransaction updatePlan(final String planId) throws NoSuchPlanException {
    return new MongoPlanTransaction(makePlanObjectId(planId));
  }

  @Override
  public void replacePlan(final String planId, final NewPlan plan) throws NoSuchPlanException {
    {
      final var result = this.planCollection.replaceOne(planById(makePlanObjectId(planId)), toDocument(plan));
      if (result.getMatchedCount() <= 0) {
        throw new NoSuchPlanException(planId);
      }
    }

    this.activityCollection.deleteMany(activityByPlan(makePlanObjectId(planId)));
    if (plan.activityInstances != null) {
      for (final var activity : plan.activityInstances) {
        this.activityCollection.insertOne(toDocument(planId, activity));
      }
    }
  }

  @Override
  public void deletePlan(final String planId) throws NoSuchPlanException {
    this.deleteAllActivities(planId);

    final var result = this.planCollection.deleteOne(planById(makePlanObjectId(planId)));
    if (result.getDeletedCount() <= 0) {
      throw new NoSuchPlanException(planId);
    }
  }

  @Override
  public String createActivity(final String planId, final ActivityInstance activity) throws NoSuchPlanException {
    ensurePlanExists(planId);

    final Document activityDocument = toDocument(planId, activity);
    this.activityCollection.insertOne(activityDocument);
    final String activityId = activityDocument.getObjectId("_id").toString();

    return activityId;
  }

  @Override
  public ActivityTransaction updateActivity(final String planId, final String activityId) throws NoSuchPlanException, NoSuchActivityInstanceException {
    return new MongoActivityTransaction(
        makePlanObjectId(planId),
        makeActivityObjectId(planId, activityId));
  }

  @Override
  public void replaceActivity(final String planId, final String activityId, final ActivityInstance activity) throws NoSuchPlanException, NoSuchActivityInstanceException {
    ensurePlanExists(planId);

    final var result = this.activityCollection.replaceOne(
        activityById(makePlanObjectId(planId), makeActivityObjectId(planId, activityId)),
        toDocument(planId, activity));
    if (result.getMatchedCount() <= 0) {
      throw new NoSuchActivityInstanceException(planId, activityId);
    }
  }

  @Override
  public void deleteActivity(final String planId, final String activityId) throws NoSuchPlanException, NoSuchActivityInstanceException {
    ensurePlanExists(planId);

    final var result = this.activityCollection.deleteOne(activityById(
        makePlanObjectId(planId),
        makeActivityObjectId(planId, activityId)));
    if (result.getDeletedCount() <= 0) {
      throw new NoSuchActivityInstanceException(planId, activityId);
    }
  }

  @Override
  public void deleteAllActivities(final String planId) throws NoSuchPlanException {
    ensurePlanExists(planId);
    this.activityCollection.deleteMany(activityByPlan(makePlanObjectId(planId)));
  }

  private Bson activityByPlan(final ObjectId planId) {
    return eq("planId", planId);
  }

  private Bson activityById(final ObjectId planId, ObjectId activityId) {
    return and(
        eq("_id", activityId),
        activityByPlan(planId));
  }

  private Bson planById(final ObjectId planId) {
    return eq("_id", planId);
  }

  private ObjectId makePlanObjectId(final String planId) throws NoSuchPlanException {
    return objectIdOrElse(planId, () -> new NoSuchPlanException(planId));
  }

  private ObjectId makeActivityObjectId(final String planId, final String activityId) throws NoSuchActivityInstanceException {
    return objectIdOrElse(activityId, () -> new NoSuchActivityInstanceException(planId, activityId));
  }

  private <T extends Throwable> ObjectId objectIdOrElse(final String id, Supplier<T> except) throws T {
    try {
      return new ObjectId(id);
    } catch (final IllegalArgumentException ex) {
      throw except.get();
    }
  }

  private void ensurePlanExists(final String planId) throws NoSuchPlanException {
    if (this.planCollection.countDocuments(planById(makePlanObjectId(planId))) == 0) {
      throw new NoSuchPlanException(planId);
    }
  }

  private SerializedParameter parameterFromDocument(final Document document) {
    switch (document.getString("type")) {
      case "null":
        return SerializedParameter.NULL;
      case "string":
        return SerializedParameter.of(document.getString("value"));
      case "int":
        return SerializedParameter.of(document.getLong("value"));
      case "double":
        return SerializedParameter.of(document.getDouble("value"));
      case "boolean":
        return SerializedParameter.of(document.getBoolean("value"));
      case "list":
        final List<Document> itemDocuments = document.getList("value", Document.class);

        final List<SerializedParameter> items = new ArrayList<>();
        for (final var itemDocument : itemDocuments) {
          items.add(parameterFromDocument(itemDocument));
        }

        return SerializedParameter.of(items);
      case "map":
        final Document fieldsDocument = document.get("value", Document.class);

        final Map<String, SerializedParameter> fields = new HashMap<>();
        for (final var entry : fieldsDocument.entrySet()) {
          fields.put(entry.getKey(), parameterFromDocument((Document)entry.getValue()));
        }

        return SerializedParameter.of(fields);
      default:
        throw new Error("unexpected bad data in database");
    }
  }

  private ActivityInstance activityFromDocument(final Document document) {
    final Document parametersDocument = document.get("parameters", Document.class);

    final Map<String, SerializedParameter> parameters = new HashMap<>();
    for (final var entry : parametersDocument.entrySet()) {
      parameters.put(entry.getKey(), parameterFromDocument((Document)entry.getValue()));
    }

    final ActivityInstance activity = new ActivityInstance();
    activity.type = document.getString("type");
    activity.startTimestamp = document.getString("startTimestamp");
    activity.parameters = parameters;

    return activity;
  }

  private Plan planFromDocuments(final Document planDocument, final FindIterable<Document> activityDocuments) {
    final Plan plan = new Plan();
    plan.name = planDocument.getString("name");
    plan.startTimestamp = planDocument.getString("startTimestamp");
    plan.endTimestamp = planDocument.getString("endTimestamp");
    plan.adaptationId = planDocument.getString("adaptationId");
    plan.activityInstances = documentStream(activityDocuments)
        .map(doc -> Pair.of(doc.getObjectId("_id").toString(), activityFromDocument(doc)))
        .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

    return plan;
  }

  private Document toDocument(final SerializedParameter parameter) {
    return parameter.match(new SerializedParameter.Visitor<>() {
      @Override
      public Document onNull() {
        final Document document = new Document();
        document.put("type", "null");
        return document;
      }

      @Override
      public Document onReal(double value) {
        final Document document = new Document();
        document.put("type", "double");
        document.put("value", value);
        return document;
      }

      @Override
      public Document onInt(long value) {
        final Document document = new Document();
        document.put("type", "int");
        document.put("value", value);
        return document;
      }

      @Override
      public Document onBoolean(boolean value) {
        final Document document = new Document();
        document.put("type", "boolean");
        document.put("value", value);
        return document;
      }

      @Override
      public Document onString(String value) {
        final Document document = new Document();
        document.put("type", "string");
        document.put("value", value);
        return document;
      }

      @Override
      public Document onList(List<SerializedParameter> items) {
        final List<Document> itemDocuments = new ArrayList<>();
        for (final var item : items) {
          itemDocuments.add(item.match(this));
        }

        final Document document = new Document();
        document.put("type", "list");
        document.put("value", itemDocuments);
        return document;
      }

      @Override
      public Document onMap(Map<String, SerializedParameter> fields) {
        final Document fieldsDocument = new Document();
        for (final var field : fields.entrySet()) {
          fieldsDocument.put(field.getKey(), field.getValue().match(this));
        }

        final Document document = new Document();
        document.put("type", "map");
        document.put("value", fieldsDocument);
        return document;
      }
    });
  }

  private Document toDocument(final Map<String, SerializedParameter> parameters) {
    final Document parametersDocument = new Document();
    for (final var entry : parameters.entrySet()) {
      parametersDocument.put(entry.getKey(), toDocument(entry.getValue()));
    }
    return parametersDocument;
  }

  private Document toDocument(final String planId, final ActivityInstance activity) {
    final Document activityDocument = new Document();
    activityDocument.put("planId", new ObjectId(planId));
    activityDocument.put("type", activity.type);
    activityDocument.put("startTimestamp", activity.startTimestamp);
    activityDocument.put("parameters", toDocument(activity.parameters));

    return activityDocument;
  }

  private Document toDocument(final NewPlan newPlan) {
    final Document planDocument = new Document();
    planDocument.put("name", newPlan.name);
    planDocument.put("startTimestamp", newPlan.startTimestamp);
    planDocument.put("endTimestamp", newPlan.endTimestamp);
    planDocument.put("adaptationId", newPlan.adaptationId);

    return planDocument;
  }

  private static <T> Stream<T> documentStream(final MongoIterable<T> documents) {
    // Eagerly construct a new iterator so we can close it after the stream is done.
		final MongoCursor<T> cursor = documents.iterator();
		// Wrap the fresh cursor in an Iterable so we can convert it to a Stream.
		final Iterable<T> iterable = () -> cursor;
		// Create a sequential stream that propagates closure to the cursor.
		return StreamSupport
			.stream(iterable.spliterator(), false)
			.onClose(cursor::close);
  }

  private class MongoPlanTransaction implements PlanTransaction {
    private final ObjectId planId;
    private Bson patch = combine();

    public MongoPlanTransaction(final ObjectId planId) {
      this.planId = planId;
    }

    @Override
    public void commit() {
      RemotePlanRepository.this.planCollection.updateOne(planById(this.planId), this.patch);
    }

    @Override
    public PlanTransaction setName(final String name) {
      this.patch = combine(this.patch, set("name", name));
      return this;
    }

    @Override
    public PlanTransaction setStartTimestamp(final String timestamp) {
      this.patch = combine(this.patch, set("startTimestamp", timestamp));
      return this;
    }

    @Override
    public PlanTransaction setEndTimestamp(final String timestamp) {
      this.patch = combine(this.patch, set("endTimestamp", timestamp));
      return this;
    }

    @Override
    public PlanTransaction setAdaptationId(final String adaptationId) {
      this.patch = combine(this.patch, set("adaptationId", adaptationId));
      return this;
    }
  }

  private class MongoActivityTransaction implements ActivityTransaction {
    private final ObjectId planId;
    private final ObjectId activityId;
    private Bson patch = combine();

    public MongoActivityTransaction(final ObjectId planId, final ObjectId activityId) {
      this.planId = planId;
      this.activityId = activityId;
    }

    @Override
    public void commit() {
      RemotePlanRepository.this.activityCollection
          .updateOne(activityById(this.planId, this.activityId), this.patch);
    }

    @Override
    public ActivityTransaction setType(final String type) {
      this.patch = combine(this.patch, set("type", type));
      return this;
    }

    @Override
    public ActivityTransaction setStartTimestamp(final String startTimestamp) {
      this.patch = combine(this.patch, set("startTimestamp", startTimestamp));
      return this;
    }

    @Override
    public ActivityTransaction setParameters(final Map<String, SerializedParameter> parameters) {
      this.patch = combine(this.patch, set("parameters", toDocument(parameters)));
      return this;
    }
  }
}