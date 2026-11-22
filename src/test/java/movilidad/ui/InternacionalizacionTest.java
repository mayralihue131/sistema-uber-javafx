package movilidad.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import org.junit.jupiter.api.Test;

import movilidad.modelo.CategoriaVehiculo;
import movilidad.modelo.EstadoViaje;
import movilidad.modelo.TipoServicio;
import movilidad.modelo.TipoVehiculo;

/**
 * Pruebas unitarias de internacionalización (i18n):
 * Valida la paridad de claves entre español e inglés, la existencia de los bundles,
 * y la correcta traducción de enumerados y servicios de dominio.
 */
class InternacionalizacionTest {

    @Test
    void testCargaDeBundlesEspanolEIngles() {
        ResourceBundle es = ResourceBundle.getBundle("ui.i18n.mensajes", Locale.forLanguageTag("es-AR"));
        ResourceBundle en = ResourceBundle.getBundle("ui.i18n.mensajes", Locale.forLanguageTag("en-US"));

        assertNotNull(es, "El bundle en español debe cargar");
        assertNotNull(en, "El bundle en inglés debe cargar");

        assertTrue(es.containsKey("app.titulo"));
        assertTrue(en.containsKey("app.titulo"));
        assertTrue(es.containsKey("vista.solicitar.viaje"));
        assertTrue(en.containsKey("vista.solicitar.viaje"));
    }

    @Test
    void testParidadCompletaDeClavesEntreIdiomas() {
        ResourceBundle es = ResourceBundle.getBundle("ui.i18n.mensajes", Locale.forLanguageTag("es-AR"));
        ResourceBundle en = ResourceBundle.getBundle("ui.i18n.mensajes", Locale.forLanguageTag("en-US"));

        Set<String> clavesEs = es.keySet();
        Set<String> clavesEn = en.keySet();

        for (String clave : clavesEs) {
            assertTrue(clavesEn.contains(clave),
                    "Falta la clave en inglés para: " + clave);
            assertFalse(en.getString(clave).isBlank(),
                    "La traducción en inglés no debe ser vacía para: " + clave);
        }

        for (String clave : clavesEn) {
            assertTrue(clavesEs.contains(clave),
                    "Falta la clave en español para: " + clave);
            assertFalse(es.getString(clave).isBlank(),
                    "La traducción en español no debe ser vacía para: " + clave);
        }
    }

    @Test
    void testTraduccionDeEnumerados() {
        ResourceBundle en = ResourceBundle.getBundle("ui.i18n.mensajes", Locale.forLanguageTag("en-US"));

        // Estados de viaje
        for (EstadoViaje estado : EstadoViaje.values()) {
            String clave = "valor." + estado.name();
            assertTrue(en.containsKey(clave), "Falta traducción en inglés para estado: " + estado);
        }

        // Tipo de vehículo
        for (TipoVehiculo tipo : TipoVehiculo.values()) {
            String clave = "valor." + tipo.name();
            assertTrue(en.containsKey(clave), "Falta traducción en inglés para tipo vehiculo: " + tipo);
        }

        // Categoría de vehículo
        for (CategoriaVehiculo cat : CategoriaVehiculo.values()) {
            String clave = "valor." + cat.name();
            assertTrue(en.containsKey(clave), "Falta traducción en inglés para categoria: " + cat);
        }

        // Tipo de servicio
        for (TipoServicio ts : TipoServicio.values()) {
            String clave = "valor." + ts.name();
            assertTrue(en.containsKey(clave), "Falta traducción en inglés para tipo servicio: " + ts);
        }
    }

    @Test
    void testClavesCiudad() {
        ResourceBundle es = ResourceBundle.getBundle("ui.i18n.mensajes", Locale.forLanguageTag("es-AR"));
        ResourceBundle en = ResourceBundle.getBundle("ui.i18n.mensajes", Locale.forLanguageTag("en-US"));

        assertTrue(es.containsKey("vista.ciudad"));
        assertTrue(en.containsKey("vista.ciudad"));
        assertTrue(es.containsKey("vista.selecciona.ciudad"));
        assertTrue(en.containsKey("vista.selecciona.ciudad"));
    }
}
