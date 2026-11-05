package movilidad.rutas;

import java.nio.file.Files;
import java.nio.file.Path;

import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.util.GHUtility;

public final class FabricaGraphHopper {

    private FabricaGraphHopper() {
    }

    public static GraphHopper crear() {
        Path archivo = Path.of(
            "datos", "mapas", "puerto_madryn_ampliado.osm"
        );

        if (!Files.isRegularFile(archivo)) {
            throw new IllegalStateException(
                "No se encontró el mapa: "
                    + archivo.toAbsolutePath()
            );
        }

        GraphHopper motor = new GraphHopper();

        try {
            motor.setOSMFile(archivo.toString());

            motor.setGraphHopperLocation(
                "datos/mapas/cache-auto"
            );

            motor.setEncodedValuesString(
                "car_access, car_average_speed"
            );

            Profile perfil = new Profile("car")
                .setCustomModel(
                    GHUtility.loadCustomModelFromJar("car.json")
                );

            motor.setProfiles(perfil);

            motor.getCHPreparationHandler()
                .setCHProfiles(new CHProfile("car"));

            motor.importOrLoad();

            return motor;

        } catch (RuntimeException e) {
            motor.close();
            throw e;
        }
    }
}