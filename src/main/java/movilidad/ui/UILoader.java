package movilidad.ui;

import movilidad.dao.txt.CargaParametros;

/**
 * Fábrica sencilla que lee {@code app.ui} de {@code config.properties}
 * e instancia el motor de interfaz adecuado ({@link Interfaz} para CLI o {@link JavaFXUserInterface} para GUI).
 */
public final class UILoader {

    private UILoader() {
    }

    /**
     * Crea la instancia de {@link UIEngine} configurada.
     *
     * @return motor de interfaz listo para iniciar
     * @throws IllegalArgumentException si el valor configurado no es reconocido
     */
    public static UIEngine createUIEngine() {
        return switch (CargaParametros.ui()) {
            case "cli" -> new Interfaz();
            case "javafx" -> new JavaFXUserInterface();
            default -> throw new IllegalArgumentException(
                    "app.ui debe ser javafx o cli");
        };
    }
}