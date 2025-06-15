package org.example.proyectoso.models;
import javafx.application.Platform;
import org.example.proyectoso.models.EstadoProceso;

public class Core {
    private int id;
    private boolean ocupado = false;
    private Proceso procesoActual;

    public Core(int id) {
        this.id = id;
    }

    // Método para ejecutar UN proceso específico
    public void ejecutarProceso(Proceso proceso, int quantum) {
        this.ocupado = true;
        this.procesoActual = proceso;
        proceso.setEstado(EstadoProceso.EJECUTANDO);

        // Simular ejecución
        for(int i = 0; i < quantum && proceso.getTiempoRestante() > 0; i++) {
            try {
                Thread.sleep(1000); // 1 segundo = 1 unidad tiempo
                proceso.decrementarTiempo();

                Platform.runLater(() -> actualizarUI());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Terminar ejecución
        if(proceso.getTiempoRestante() <= 0) {
            proceso.setEstado(EstadoProceso.TERMINADO);
        } else {
            proceso.setEstado(EstadoProceso.LISTO);
        }

        this.ocupado = false;
        this.procesoActual = null;
    }

    public boolean estaOcupado() {
        return ocupado;
    }

    public Core buscarCoreLibre() {
        for (Core core : CPU.getCores()) {
            if (!core.estaOcupado()) {
                return core;
            }
        }
        return null; // No hay núcleos libres
    }
}