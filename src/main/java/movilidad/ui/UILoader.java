package movilidad.ui;

import movilidad.dao.txt.CargaParametros;

public final class UILoader {

    private UILoader() {
    }

    public static UIEngine createUIEngine() {
        return switch (CargaParametros.ui()) {
            case "cli" -> new Interfaz();
            case "javafx" -> new JavaFXUserInterface();
            default -> throw new IllegalArgumentException(
                    "app.ui debe ser javafx o cli");
        };
    }
}