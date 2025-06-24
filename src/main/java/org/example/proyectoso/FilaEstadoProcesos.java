package org.example.proyectoso;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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



    

    
    public String getNuevo() {
        return nuevo.get();
    }

    
    public void setNuevo(String value) {
        nuevo.set(value != null ? value : "");
    }

    


    
    public String getListo() {
        return listo.get();
    }

    
    public void setListo(String value) {
        listo.set(value != null ? value : "");
    }

    


    
    public String getEspera() {
        return espera.get();
    }

    
    public void setEspera(String value) {
        espera.set(value != null ? value : "");
    }



    
    public String getTerminado() {
        return terminado.get();
    }

    
    public void setTerminado(String value) {
        terminado.set(value != null ? value : "");
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