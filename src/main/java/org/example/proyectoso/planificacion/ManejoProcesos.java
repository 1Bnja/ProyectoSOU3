package org.example.proyectoso.planificacion;
import org.example.proyectoso.models.*;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;


public class ManejoProcesos {
    
    private final Queue<Proceso> colaProcesos;
    private final List<Proceso> procesosCompletados;
    private final List<Proceso> procesosEnEjecucion;

    
    private Planificacion planificador;

    
    private final Object lock = new Object();
    private volatile boolean activo;

    
    private int procesosTotales;
    private int procesosCompletadosCount;
    private long tiempoInicioOperacion;
    private long tiempoTotalOperacion;

    
    public ManejoProcesos() {
        this.colaProcesos = new ConcurrentLinkedQueue<>();
        this.procesosCompletados = new ArrayList<>();
        this.procesosEnEjecucion = new ArrayList<>();
        this.activo = false;
        this.procesosTotales = 0;
        this.procesosCompletadosCount = 0;
        this.tiempoInicioOperacion = 0;
        this.tiempoTotalOperacion = 0;

        System.out.println("📋 ManejoProcesos inicializado");
    }

    
    public void setPlanificador(Planificacion planificador) {
        synchronized (lock) {
            this.planificador = planificador;
            System.out.println("⚙️ Planificador asignado: " + planificador.getClass().getSimpleName());
        }
    }

    public void detenerEjecucion() {
        synchronized (lock) {
            activo = false;

            
            for (Proceso proceso : procesosEnEjecucion) {
                if (!proceso.haTerminado()) {
                    proceso.pausar();
                    colaProcesos.offer(proceso);
                }
            }

            procesosEnEjecucion.clear();

            if (planificador != null) {
                planificador.detener();
            }

            System.out.println("🛑 Ejecución detenida");
        }
    }

    


    
    private void finalizarOperacion() {
        activo = false;
        tiempoTotalOperacion = System.currentTimeMillis() - tiempoInicioOperacion;

        System.out.println("🏁 Operación finalizada en " + tiempoTotalOperacion + "ms");
        System.out.println("📊 Procesos completados: " + procesosCompletadosCount + "/" + procesosTotales);
    }

    
    public String getEstadisticas() {
        synchronized (lock) {
            StringBuilder stats = new StringBuilder();
            stats.append("=== ESTADÍSTICAS MANEJO PROCESOS ===\n");
            stats.append("Procesos totales: ").append(procesosTotales).append("\n");
            stats.append("En cola: ").append(colaProcesos.size()).append("\n");
            stats.append("En ejecución: ").append(procesosEnEjecucion.size()).append("\n");
            stats.append("Completados: ").append(procesosCompletadosCount).append("\n");
            stats.append("Estado: ").append(activo ? "ACTIVO" : "INACTIVO").append("\n");

            if (tiempoTotalOperacion > 0) {
                stats.append("Tiempo total operación: ").append(tiempoTotalOperacion).append("ms\n");

                if (procesosCompletadosCount > 0) {
                    double promedioTiempoEspera = procesosCompletados.stream()
                            .mapToInt(Proceso::getTiempoEspera)
                            .average()
                            .orElse(0.0);

                    double promedioTiempoRespuesta = procesosCompletados.stream()
                            .mapToInt(Proceso::getTiempoRespuesta)
                            .average()
                            .orElse(0.0);

                    double promedioTiempoRetorno = procesosCompletados.stream()
                            .mapToInt(Proceso::getTiempoRetorno)
                            .average()
                            .orElse(0.0);

                    stats.append("Promedio tiempo espera: ").append(String.format("%.1f", promedioTiempoEspera)).append("ms\n");
                    stats.append("Promedio tiempo respuesta: ").append(String.format("%.1f", promedioTiempoRespuesta)).append("ms\n");
                    stats.append("Promedio tiempo retorno: ").append(String.format("%.1f", promedioTiempoRetorno)).append("ms\n");
                }
            }

            return stats.toString();
        }
    }

    

    







    

    @Override
    public String toString() {
        synchronized (lock) {
            return String.format("ManejoProcesos[Cola:%d, Ejecución:%d, Completados:%d, %s]",
                    colaProcesos.size(), procesosEnEjecucion.size(),
                    procesosCompletadosCount, activo ? "ACTIVO" : "INACTIVO");
        }
    }
}