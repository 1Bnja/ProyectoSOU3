package org.example.proyectoso;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.example.proyectoso.models.EstadoProceso;
import org.example.proyectoso.models.Proceso;


public class FilaEstadoProcesos {

    
    private final StringProperty nuevo;
    private final StringProperty listo;
    private final StringProperty espera;
    private final StringProperty terminado;

    
    public FilaEstadoProcesos(String nuevo, String listo, String espera, String terminado) {
        this.nuevo = new SimpleStringProperty(nuevo != null ? nuevo : "");
        this.listo = new SimpleStringProperty(listo != null ? listo : "");
        this.espera = new SimpleStringProperty(espera != null ? espera : "");
        this.terminado = new SimpleStringProperty(terminado != null ? terminado : "");
    }

    
    public FilaEstadoProcesos() {
        this("", "", "", "");
    }

    
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
                
                setListo(contenido);
                break;
            default:
                break;
        }
    }

    

    
    public String getNuevo() {
        return nuevo.get();
    }

    
    public void setNuevo(String value) {
        nuevo.set(value != null ? value : "");
    }

    
    public StringProperty nuevoProperty() {
        return nuevo;
    }

    
    public String getListo() {
        return listo.get();
    }

    
    public void setListo(String value) {
        listo.set(value != null ? value : "");
    }

    
    public StringProperty listoProperty() {
        return listo;
    }

    
    public String getEspera() {
        return espera.get();
    }

    
    public void setEspera(String value) {
        espera.set(value != null ? value : "");
    }

    
    public StringProperty esperaProperty() {
        return espera;
    }

    
    public String getTerminado() {
        return terminado.get();
    }

    
    public void setTerminado(String value) {
        terminado.set(value != null ? value : "");
    }

    
    public StringProperty terminadoProperty() {
        return terminado;
    }

    

    
    public boolean estaVacia() {
        return (getNuevo().isEmpty() && getListo().isEmpty() &&
                getEspera().isEmpty() && getTerminado().isEmpty());
    }

    
    public void limpiar() {
        setNuevo("");
        setListo("");
        setEspera("");
        setTerminado("");
    }

    
    public int contarColumnasConContenido() {
        int count = 0;
        if (!getNuevo().isEmpty()) count++;
        if (!getListo().isEmpty()) count++;
        if (!getEspera().isEmpty()) count++;
        if (!getTerminado().isEmpty()) count++;
        return count;
    }

    
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
                
                setListo(contenido);
                break;
            default:
                System.err.println("Estado no reconocido: " + estado);
                break;
        }
    }

    
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

    
    public FilaEstadoProcesos clonar() {
        return new FilaEstadoProcesos(getNuevo(), getListo(), getEspera(), getTerminado());
    }

    
    public static FilaEstadoProcesos desdeProceso(Proceso proceso) {
        if (proceso == null) {
            return new FilaEstadoProcesos();
        }

        String contenido = formatearProceso(proceso);
        return new FilaEstadoProcesos(proceso.getEstado(), contenido);
    }

    
    private static String formatearProceso(Proceso proceso) {
        if (proceso == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("P").append(proceso.getId());

        
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