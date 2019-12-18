package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.utilities;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.AdaptationContractException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Objects;
import java.util.ServiceLoader;

public final class AdaptationLoader {
    public static MerlinAdaptation<?> loadAdaptation(final Path adaptationPath) throws AdaptationContractException {
      Objects.requireNonNull(adaptationPath);

      final URL adaptationURL;
      try {
          // Construct a ClassLoader with access to classes in the adaptation location.
          adaptationURL = adaptationPath.toUri().toURL();
      } catch (final MalformedURLException ex) {
          // This exception only happens if there is no URL protocol handler available to represent a Path.
          // This is highly unexpected, and indicates a fundamental problem with the system environment.
          throw new Error(ex);
      }

      final ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
      final ClassLoader classLoader = new URLClassLoader(new URL[]{adaptationURL}, parentClassLoader);

      // Look for MerlinAdaptation implementors in the adaptation.
      final ServiceLoader<MerlinAdaptation> serviceLoader =
          ServiceLoader.load(MerlinAdaptation.class, classLoader);

      // Return the first we come across. (This may not be deterministic, so for correctness
      // we're assuming there's only one MerlinAdaptation in any given location.
      return serviceLoader
          .findFirst()
          .orElseThrow(() -> new AdaptationContractException("No implementation found for `" + MerlinAdaptation.class.getSimpleName() + "`"));
  }
}