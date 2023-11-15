package gov.nasa.jpl.aerie.contrib.streamline.debugging;

import java.util.WeakHashMap;


/**
 * Allows anything that uses reference equality to be given a name.
 *
 * <p>
 *   By handling naming in a static auxiliary data structure, we achieve several goals:
 *   1) Naming doesn't bloat interfaces like Resource and DynamicsEffect.
 *   2) Names can be applied to classes and interfaces after-the-fact,
 *      including applying names to classes and interfaces that can't be modified, like library code.
 *   3) Naming is nevertheless globally-available to anyone who holds a reference a thing.
 *      (Unlike passing the name in a parallel parameter, for example.)
 * </p>
 */
public final class Naming {
  private Naming() {}

  // Use a WeakHashMap so that naming a thing doesn't prevent it from being garbage-collected.
  private static final WeakHashMap<Object, String> NAMES = new WeakHashMap<>();

  /**
   * Register a name for thing.
   * If thing has no name, this will be its primary name.
   * Otherwise, this will be an alias.
   */
  public static void registerName(Object thing, String name) {
    NAMES.put(thing, name);
  }

  /**
   * Get the name for thing.
   * If thing has no registered name, return resultIfAnonymous instead.
   */
  public static String getName(Object thing, String resultIfAnonymous) {
    return NAMES.getOrDefault(thing, resultIfAnonymous);
  }
}
