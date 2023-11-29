package gov.nasa.jpl.aerie.contrib.streamline.debugging;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Allows anything that uses reference equality to be given a name.
 *
 * <p>
 *   By handling naming in a static auxiliary data structure, we achieve several goals:
 *   1) Naming doesn't bloat interfaces like Resource and DynamicsEffect.
 *   2) Names can be applied to classes and interfaces after-the-fact,
 *      including applying names to classes and interfaces that can't be modified, like library code.
 *   3) Naming is nevertheless globally available.
 *      (Unlike passing the name in a parallel parameter, for example.)
 * </p>
 */
public final class Naming {
  private Naming() {}

  // Use a WeakHashMap so that naming a thing doesn't prevent it from being garbage-collected.
  private static final WeakHashMap<Object, Supplier<Optional<String>>> NAMES = new WeakHashMap<>();

  /**
   * Register a name for thing, as a function of args' names.
   * If any of the args are anonymous, so is this thing.
   */
  public static void name(Object thing, String nameFormat, Object... args) {
    var args$ = Arrays.stream(args).map(WeakReference::new).toArray(WeakReference[]::new);
    NAMES.put(thing, () -> {
      Object[] argNames = new Object[args$.length];
      for (int i = 0; i < args$.length; ++i) {
        var argName$ = Optional.ofNullable(args$[i].get()).flatMap(Naming::getName);
        if (argName$.isEmpty()) return Optional.empty();
        argNames[i] = argName$.get();
      }
      return Optional.of(nameFormat.formatted(argNames));
    });
  }

  /**
   * Get the name for thing.
   * If thing has no registered name and no synonyms,
   * returns empty.
   */
  public static Optional<String> getName(Object thing) {
    return Optional.ofNullable(NAMES.get(thing)).flatMap(Supplier::get);
  }

  public static String argsFormat(Collection<?> collection) {
    return "(" + IntStream.range(0, collection.size()).mapToObj($ -> "%s").collect(Collectors.joining(", ")) + ")";
  }
}
