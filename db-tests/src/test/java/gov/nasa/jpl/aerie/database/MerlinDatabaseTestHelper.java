package gov.nasa.jpl.aerie.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

final class MerlinDatabaseTestHelper {
  private final Connection connection;
  final User admin;
  final User user;
  final User viewer;

  MerlinDatabaseTestHelper(Connection connection) throws SQLException {
    this.connection = connection;
    admin = insertUser("MerlinAdmin");
    user = insertUser("MerlinUser", "user");
    viewer = insertUser("MerlinViewer", "viewer");
  }

  record User(String name, String defaultRole, String session) {}

  User insertUser(final String username) throws SQLException {
    return insertUser(username, "aerie_admin");
  }

  User insertUser(final String username, final String defaultRole) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.execute(
              """
              INSERT INTO metadata.users (username, default_role)
              VALUES ('%s', '%s');
              """.formatted(username, defaultRole)
          );
    }
    return new User(
        username,
        defaultRole,
        "{ \"x-hasura-user-id\": \"%s\", \"x-hasura-default-role\": \"%s\" }"
            .formatted(username, defaultRole));
  }

  int insertFileUpload() throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              """
                  INSERT INTO uploaded_file (path, name)
                  VALUES ('test-path', 'test-name-%s')
                  RETURNING id;"""
                  .formatted(UUID.randomUUID().toString())
          );
      res.next();
      return res.getInt("id");
    }
  }

  int insertMissionModel(final int fileId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              """
                  INSERT INTO mission_model (name, mission, owner, version, jar_id)
                  VALUES ('test-mission-model-%s', 'test-mission', 'MerlinAdmin', '0', %s)
                  RETURNING id;"""
                  .formatted(UUID.randomUUID().toString(), fileId)
          );
      res.next();
      return res.getInt("id");
    }
  }

  int insertPlan(final int missionModelId) throws SQLException {
    return insertPlan(missionModelId, admin.name);
  }

  int insertPlan(final int missionModelId, final String username) throws SQLException {
    return insertPlan(missionModelId, username, "2020-1-1 00:00:00+00");
  }

  int insertPlan(final int missionModelId, final String username, final String start_time) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              """
                  INSERT INTO plan (name, model_id, duration, start_time, owner)
                  VALUES ('test-plan-%s', '%s', '0', '%s', '%s')
                  RETURNING id;"""
                  .formatted(UUID.randomUUID().toString(), missionModelId, start_time, username)
          );
      res.next();
      return res.getInt("id");
    }
  }

  int insertActivity(final int planId) throws SQLException {
    return insertActivity(planId, "00:00:00");
  }

  int insertActivity(final int planId, final String startOffset) throws SQLException {
    return insertActivity(planId, startOffset, "{}");
  }

  int insertActivity(final int planId, final String startOffset, final String arguments) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              """
                  INSERT INTO activity_directive (type, plan_id, start_offset, arguments)
                  VALUES ('test-activity', '%s', '%s', '%s')
                  RETURNING id;"""
                  .formatted(planId, startOffset, arguments)
          );

      res.next();
      return res.getInt("id");
    }
  }

  /**
   * To anchor an activity to the plan, set "anchorId" equal to -1.
   */
  void setAnchor(int anchorId, boolean anchoredToStart, int activityId, int planId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      if (anchorId == -1) {
        statement.execute(
            """
                update activity_directive
                set anchor_id = null,
                    anchored_to_start = %b
                where id = %d and plan_id = %d;
                """.formatted(anchoredToStart, activityId, planId));
      } else {
        statement.execute(
            """
                update activity_directive
                set anchor_id = %d,
                    anchored_to_start = %b
                where id = %d and plan_id = %d;
                """.formatted(anchorId, anchoredToStart, activityId, planId));
      }
    }
  }

  void insertActivityType(final int modelId, final String name) throws SQLException {
    try(final var statement = connection.createStatement()) {
      statement.execute(
          """
          INSERT INTO activity_type (model_id, name, parameters, required_parameters, computed_attributes_value_schema)
          VALUES (%d, '%s', '{}', '[]', '{}');
          """.formatted(modelId, name)
      );
    }
  }

  int insertConstraintPlan(int plan_id, String name, String definition, User user) throws SQLException {
    try(final var statement = connection.createStatement()) {
      final var res = statement.executeQuery(
          """
          INSERT INTO public.constraint
            (name, description, definition, plan_id, owner, updated_by)
          VALUES ('%s', 'Merlin DB Test Constraint', '%s', %d, '%s', '%s')
          RETURNING id;
          """.formatted(name, definition, plan_id, user.name, user.name));
      res.next();
      return res.getInt("id");
    }
  }
}
