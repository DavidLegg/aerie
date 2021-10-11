// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package gov.nasa.jpl.aerie.scheduler.aerie.type;

import com.apollographql.apollo.api.InputType;
import com.apollographql.apollo.api.internal.InputFieldMarshaller;
import com.apollographql.apollo.api.internal.InputFieldWriter;
import com.apollographql.apollo.api.internal.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public final class CreateActivityInstance implements InputType {
  private final @NotNull List<ActivityInstanceParameterInput> parameters;

  private final @NotNull String startTimestamp;

  private final @NotNull String type;

  private transient volatile int $hashCode;

  private transient volatile boolean $hashCodeMemoized;

  CreateActivityInstance(@NotNull List<ActivityInstanceParameterInput> parameters,
      @NotNull String startTimestamp, @NotNull String type) {
    this.parameters = parameters;
    this.startTimestamp = startTimestamp;
    this.type = type;
  }

  public @NotNull List<ActivityInstanceParameterInput> parameters() {
    return this.parameters;
  }

  public @NotNull String startTimestamp() {
    return this.startTimestamp;
  }

  public @NotNull String type() {
    return this.type;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public InputFieldMarshaller marshaller() {
    return new InputFieldMarshaller() {
      @Override
      public void marshal(InputFieldWriter writer) throws IOException {
        writer.writeList("parameters", new InputFieldWriter.ListWriter() {
          @Override
          public void write(InputFieldWriter.ListItemWriter listItemWriter) throws IOException {
            for (final ActivityInstanceParameterInput $item : parameters) {
              listItemWriter.writeObject($item != null ? $item.marshaller() : null);
            }
          }
        });
        writer.writeString("startTimestamp", startTimestamp);
        writer.writeString("type", type);
      }
    };
  }

  @Override
  public int hashCode() {
    if (!$hashCodeMemoized) {
      int h = 1;
      h *= 1000003;
      h ^= parameters.hashCode();
      h *= 1000003;
      h ^= startTimestamp.hashCode();
      h *= 1000003;
      h ^= type.hashCode();
      $hashCode = h;
      $hashCodeMemoized = true;
    }
    return $hashCode;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof CreateActivityInstance) {
      CreateActivityInstance that = (CreateActivityInstance) o;
      return this.parameters.equals(that.parameters)
       && this.startTimestamp.equals(that.startTimestamp)
       && this.type.equals(that.type);
    }
    return false;
  }

  public static final class Builder {
    private @NotNull List<ActivityInstanceParameterInput> parameters;

    private @NotNull String startTimestamp;

    private @NotNull String type;

    Builder() {
    }

    public Builder parameters(@NotNull List<ActivityInstanceParameterInput> parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder startTimestamp(@NotNull String startTimestamp) {
      this.startTimestamp = startTimestamp;
      return this;
    }

    public Builder type(@NotNull String type) {
      this.type = type;
      return this;
    }

    public CreateActivityInstance build() {
      Utils.checkNotNull(parameters, "parameters == null");
      Utils.checkNotNull(startTimestamp, "startTimestamp == null");
      Utils.checkNotNull(type, "type == null");
      return new CreateActivityInstance(parameters, startTimestamp, type);
    }
  }
}