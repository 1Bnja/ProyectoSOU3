package org.example.proyectoso;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.example.proyectoso.models.EstadoProceso;
import org.example.proyectoso.models.Proceso;

/**
 * Clase que representa una fila en la tabla de estados de procesos
 * Cada fila contiene información de procesos en diferentes estados:
 * Nuevo, Listo, Espera y Terminado
 */
public class FilaEstadoProcesos {

    // Propiedades observables para JavaFX
    private final StringProperty nuevo;
    private final StringProperty listo;
    private final StringProperty espera;
    private final StringProperty terminado;

    /**
     * Constructor principal
     * @param nuevo Texto a mostrar en la columna "Nuevo"
     * @param listo Texto a mostrar en la columna "Listo"
     * @param espera Texto a mostrar en la columna "Espera"
     * @param terminado Texto a mostrar en la columna "Terminado"
     */
    public FilaEstadoProcesos(String nuevo, String listo, String espera, String terminado) {
        this.nuevo = new SimpleStringProperty(nuevo != null ? nuevo : "");
        this.listo = new SimpleStringProperty(listo != null ? listo : "");
        this.espera = new SimpleStringProperty(espera != null ? espera : "");
        this.terminado = new SimpleStringProperty(terminado != null ? terminado : "");
    }

    /**
     * Constructor que crea una fila vacía
     */
    public FilaEstadoProcesos() {
        this("", "", "", "");
    }

    /**
     * Constructor que crea una fila con solo un proceso en estado específico
     * @param estado El estado del proceso
     * @param contenido El contenido a mostrar
     */
    public FilaEstadoProcesos(EstadoProceso estado, String contenido) {
        this();

        switch (estado) {
            case NUEVO:
                setNuevo(contenido);
                break;
            case LISTO:
                setListo(contenido);
                break;
            case ESPERANDO:
                setEspera(contenido);
                break;
            case TERMINADO:
                setTerminado(contenido);
                break;
            case EJECUTANDO:
                // Los procesos ejecutándose se muestran en la columna "Listo"
                setListo(contenido);
                break;
            default:
                break;
        }
    }

    // === GETTERS Y SETTERS PARA JAVAFX ===

    /**
     * Obtiene el contenido de la columna "Nuevo"
     */
    public String getNuevo() {
        return nuevo.get();
    }

    /**
     * Establece el contenido de la columna "Nuevo"
     */
    public void setNuevo(String value) {
        nuevo.set(value != null ? value : "");
    }

    /**
     * Obtiene la propiedad observable de la columna "Nuevo"
     */
    public StringProperty nuevoProperty() {
        return nuevo;
    }

    /**
     * Obtiene el contenido de la columna "Listo"
     */
    public String getListo() {
        return listo.get();
    }

    /**
     * Establece el contenido de la columna "Listo"
     */
    public void setListo(String value) {
        listo.set(value != null ? value : "");
    }

    /**
     * Obtiene la propiedad observable de la columna "Listo"
     */
    public StringProperty listoProperty() {
        return listo;
    }

    /**
     * Obtiene el contenido de la columna "Espera"
     */
    public String getEspera() {
        return espera.get();
    }

    /**
     * Establece el contenido de la columna "Espera"
     */
    public void setEspera(String value) {
        espera.set(value != null ? value : "");
    }

    /**
     * Obtiene la propiedad observable de la columna "Espera"
     */
    public StringProperty esperaProperty() {
        return espera;
    }

    /**
     * Obtiene el contenido de la columna "Terminado"
     */
    public String getTerminado() {
        return terminado.get();
    }

    /**
     * Establece el contenido de la columna "Terminado"
     */
    public void setTerminado(String value) {
        terminado.set(value != null ? value : "");
    }

    /**
     * Obtiene la propiedad observable de la columna "Terminado"
     */
    public StringProperty terminadoProperty() {
        return terminado;
    }

    // === MÉTODOS DE UTILIDAD ===

    /**
     * Verifica si la fila está completamente vacía
     */
    public boolean estaVacia() {
        return (getNuevo().isEmpty() && getListo().isEmpty() &&
                getEspera().isEmpty() && getTerminado().isEmpty());
    }

    /**
     * Limpia todo el contenido de la fila
     */
    public void limpiar() {
        setNuevo("");
        setListo("");
        setEspera("");
        setTerminado("");
    }

    /**
     * Cuenta cuántas columnas tienen contenido
     */
    public int contarColumnasConContenido() {
        int count = 0;
        if (!getNuevo().isEmpty()) count++;
        if (!getListo().isEmpty()) count++;
        if (!getEspera().isEmpty()) count++;
        if (!getTerminado().isEmpty()) count++;
        return count;
    }

    /**
     * Obtiene una representación del contenido de la fila
     */
    public String getContenidoCompleto() {
        StringBuilder sb = new StringBuilder();
        if (!getNuevo().isEmpty()) {
            sb.append("Nuevo: ").append(getNuevo()).append(" | ");
        }
        if (!getListo().isEmpty()) {
            sb.append("Listo: ").append(getListo()).append(" | ");
        }
        if (!getEspera().isEmpty()) {
            sb.append("Espera: ").append(getEspera()).append(" | ");
        }
        if (!getTerminado().isEmpty()) {
            sb.append("Terminado: ").append(getTerminado()).append(" | ");
        }

        String resultado = sb.toString();
        return resultado.endsWith(" | ") ? resultado.substring(0, resultado.length() - 3) : resultado;
    }

    /**
     * Establece el contenido de una columna específica según el estado
     */
    public void setContenidoPorEstado(EstadoProceso estado, String contenido) {
        switch (estado) {
            case NUEVO:
                setNuevo(contenido);
                break;
            case LISTO:
                setListo(contenido);
                break;
            case ESPERANDO:
                setEspera(contenido);
                break;
            case TERMINADO:
                setTerminado(contenido);
                break;
            case EJECUTANDO:
                // Los procesos ejecutándose se muestran en "Listo" con indicador especial
                setListo(contenido);
                break;
            default:
                System.err.println("Estado no reconocido: " + estado);
                break;
        }
    }

    /**
     * Obtiene el contenido de una columna específica según el estado
     */
    public String getContenidoPorEstado(EstadoProceso estado) {
        switch (estado) {
            case NUEVO:
                return getNuevo();
            case LISTO:
            case EJECUTANDO:
                return getListo();
            case ESPERANDO:
                return getEspera();
            case TERMINADO:
                return getTerminado();
            default:
                return "";
        }
    }

    @Override
    public String toString() {
        return String.format("FilaEstadoProcesos[Nuevo:'%s', Listo:'%s', Espera:'%s', Terminado:'%s']",
                getNuevo(), getListo(), getEspera(), getTerminado());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        FilaEstadoProcesos that = (FilaEstadoProcesos) obj;

        return getNuevo().equals(that.getNuevo()) &&
                getListo().equals(that.getListo()) &&
                getEspera().equals(that.getEspera()) &&
                getTerminado().equals(that.getTerminado());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getNuevo(), getListo(), getEspera(), getTerminado());
    }

    /**
     * Crea una copia de esta fila
     */
    public FilaEstadoProcesos clonar() {
        return new FilaEstadoProcesos(getNuevo(), getListo(), getEspera(), getTerminado());
    }

    /**
     * Método estático para crear una fila desde un proceso específico
     */
    public static FilaEstadoProcesos desdeProceso(Proceso proceso) {
        if (proceso == null) {
            return new FilaEstadoProcesos();
        }

        String contenido = formatearProceso(proceso);
        return new FilaEstadoProcesos(proceso.getEstado(), contenido);
    }

    /**
     * Método auxiliar para formatear un proceso
     */
    private static String formatearProceso(Proceso proceso) {
        if (proceso == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("P").append(proceso.getId());

        // Agregar información adicional según el estado
        switch (proceso.getEstado()) {
            case NUEVO:
                sb.append(" (").append(proceso.getTamanoMemoria()).append("MB)");
                break;
            case LISTO:
                sb.append(" (").append(proceso.getTiempoRestante()).append("ms)");
                break;
            case EJECUTANDO:
                sb.append(" [EXEC] (").append(proceso.getTiempoRestante()).append("ms)");
                break;
            case ESPERANDO:
                sb.append(" (I/O)");
                break;
            case TERMINADO:
                sb.append(" ✓");
                break;
        }

        return sb.toString();
    }
}