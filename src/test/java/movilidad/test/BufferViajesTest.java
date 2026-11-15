package movilidad.test;

import org.junit.jupiter.api.Test;

import movilidad.simulacion.*;


import static org.junit.jupiter.api.Assertions.*;

public class BufferViajesTest {

    @Test
    public void testAgregarYTomarViaje() throws InterruptedException {
        BufferViajes buffer = new BufferViajes(3);
        
        // Agregamos un viaje de prueba
        buffer.agregarViaje("Viaje Test 1");
        
        // Verificamos que se pueda tomar correctamente
        String viajeTomado = buffer.tomarViaje();
        assertNotNull(viajeTomado);
        assertEquals("Viaje Test 1", viajeTomado);
    }
}