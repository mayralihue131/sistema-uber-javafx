package movilidad.modelo;

public class Ubicacion {

    private double latitud;
    private double longitud;

    public Ubicacion() {
    }

    public Ubicacion(double latitud, double longitud) {
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    public double calcularDistancia(Ubicacion ubicacion) {
        if (ubicacion == null) {
            throw new IllegalArgumentException(
                    "La ubicación no puede ser null");
        }

        double radioTierraKm = 6371.0;

        double latitudOrigen = Math.toRadians(this.latitud);
        double latitudDestino = Math.toRadians(ubicacion.latitud);

        double diferenciaLatitud =
                Math.toRadians(ubicacion.latitud - this.latitud);
        double diferenciaLongitud =
                Math.toRadians(ubicacion.longitud - this.longitud);

        double a = Math.pow(Math.sin(diferenciaLatitud / 2), 2)
                + Math.cos(latitudOrigen) * Math.cos(latitudDestino)
                * Math.pow(Math.sin(diferenciaLongitud / 2), 2);

        a = Math.max(0.0, Math.min(1.0, a));

        double distanciaAngular =
                2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return radioTierraKm * distanciaAngular;
    }

    @Override
    public String toString() {
        return "Ubicacion{" +
                "latitud=" + latitud +
                ", longitud=" + longitud +
                '}';
    }
}
