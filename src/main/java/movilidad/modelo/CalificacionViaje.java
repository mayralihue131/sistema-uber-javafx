package movilidad.modelo;

public enum CalificacionViaje {

    NO_CALIFICADO(0),
    MALO(1),
    REGULAR(2),
    BUENO(3),
    MUY_BUENO(4),
    EXCELENTE(5);

    private final int valor;

    CalificacionViaje(int valor) {
        this.valor = valor;
    }

    public int getValor() {
        return valor;
    }

    @Override
    public String toString() {
        return switch (this) {
            case NO_CALIFICADO -> "Sin calificar";
            case MALO -> "Malo";
            case REGULAR -> "Regular";
            case BUENO -> "Bueno";
            case MUY_BUENO -> "Muy bueno";
            case EXCELENTE -> "Excelente";
        };
    }
}