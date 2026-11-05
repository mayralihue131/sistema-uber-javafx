package movilidad.rutas;

public record PuntoRuta(double latitud, double longitud) {

    public PuntoRuta {
        if (!Double.isFinite(latitud)
                || !Double.isFinite(longitud)
                || latitud < -90 || latitud > 90
                || longitud < -180 || longitud > 180) {
            throw new IllegalArgumentException(
                "Las coordenadas no son válidas."
            );
        }
    }
}//Guarda la latitud y longitud de un punto del mapa y verifica que sean válidas. 
//La usaremos para formar el recorrido.