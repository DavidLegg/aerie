package gov.nasa.jpl.aerie.contrib.streamline.modeling.logs;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Lightweight immutable linked-list style message log.
 * Suitable for use in a {@link CellResource} with {@link Discrete} dynamics.
 */
public interface Log {
  Stream<String> stream();
  int size();

  private static Log of(Supplier<Stream<String>> s, int z) {
    return new Log() {
      @Override
      public Stream<String> stream() {
        return s.get();
      }

      @Override
      public int size() {
        return z;
      }

      // WARNING! This class needs an equals/hashCode to work properly.
      // Consider re-thinking the log type, or removing it altogether.
    };
  }

  static Log empty() {
    return of(Stream::empty, 0);
  }

  default Log append(String message) {
    return of(() -> Stream.concat(stream(), Stream.of(message)), size() + 1);
  }
}
