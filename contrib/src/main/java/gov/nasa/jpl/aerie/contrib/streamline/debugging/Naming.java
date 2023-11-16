package gov.nasa.jpl.aerie.contrib.streamline.debugging;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
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
  private static final WeakHashMap<Object, List<WeakReference<Object>>> SYNONYMS = new WeakHashMap<>();

  /**
   * Register a name for thing.
   * If thing has no name, this will be its primary name.
   * Otherwise, this will be an alias.
   */
  public static void registerName(Object thing, String name) {
    NAMES.put(thing, name);
  }

  /**
   * Sets secondary as a synonym of primary.
   * When getting the name for secondary, if it's not directly named,
   * we'll look for a name for primary instead.
   */
  public static void registerSynonym(Object primary, Object secondary) {
    SYNONYMS.computeIfAbsent(secondary, $ -> new ArrayList<>()).add(new WeakReference<>(primary));
  }

  /**
   * Get the name for thing.
   * If thing has no registered name and no named synonyms,
   * return resultIfAnonymous instead.
   */
  public static String getName(Object thing, String resultIfAnonymous) {
    final var directName = NAMES.get(thing);
    if (directName != null) return directName;
    final var synonyms = SYNONYMS.get(thing);
    if (synonyms != null) {
      for (var synonymRef : synonyms) {
        // If synonym reference is null, that object was deleted.
        final var synonym = synonymRef.get();
        if (synonym == null) continue;
        // Take the first synonymous name we can find
        final var synonymousName = NAMES.get(synonym);
        if (synonymousName != null) return synonymousName;
      }
    }
    return resultIfAnonymous;
  }
}
