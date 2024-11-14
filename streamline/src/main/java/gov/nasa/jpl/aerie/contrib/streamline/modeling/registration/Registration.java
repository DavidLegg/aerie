package gov.nasa.jpl.aerie.contrib.streamline.modeling.registration;

public final class Registration {
    private Registration() {}

    private static Registrar registrar;

    public static Registrar registrar() {
        return registrar;
    }

    public static void init(Registrar registrar) {
        Registration.registrar = registrar;
    }
}
