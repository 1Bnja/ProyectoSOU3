package org.example.proyectoso.planificacion;
import org.example.proyectoso.models.*;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Clase que maneja la cola de procesos y su distribución
 * Actúa como intermediario entre el Controlador y el Planificador
 */
public class ManejoProcesos {
    // Cola principal de procesos
    private final Queue<Proceso> colaProcesos;
    private final List<Proceso> procesosCompletados;
    private final List<Proceso> procesosEnEjecucion;

    // Referencia al planificador
    private Planificacion planificador;

    // Control de estado
    private final Object lock = new Object();
    private volatile boolean activo;

    // Estadísticas
    private int procesosTotales;
    private int procesosCompletadosCount;
    private long tiempoInicioOperacion;
    private long tiempoTotalOperacion;

    /**
     * Constructor
     */
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

    /**
     * Establece el planificador a utilizar
     */
    public void setPlanificador(Planificacion planificador) {
        synchronized (lock) {
            this.planificador = planificador;
            System.out.println("⚙️ Planificador asignado: " + planificador.getClass().getSimpleName());
        }
    }

    /**
     * Agrega un proceso a la cola
     */
    public boolean agregarProceso(Proceso proceso) {
        if (proceso == null) {
            return false;
        }

        synchronized (lock) {
            colaProcesos.offer(proceso);
            procesosTotales++;
            proceso.setEstado(EstadoProceso.LISTO);

            System.out.println("➕ Proceso " + proceso.getId() + " agregado a la cola");
            return true;
        }
    }

    /**
     * Agrega múltiples procesos a la cola
     */
    public void agregarProcesos(List<Proceso> procesos) {
        if (procesos == null || procesos.isEmpty()) {
            return;
        }

        synchronized (lock) {
            for (Proceso proceso : procesos) {
                if (proceso != null) {
                    colaProcesos.offer(proceso);
                    procesosTotales++;
                    proceso.setEstado(EstadoProceso.LISTO);
                }
            }

            System.out.println("➕ " + procesos.size() + " procesos agregados a la cola");
        }
    }

    /**
     * Obtiene el siguiente proceso de la cola
     */
    public Proceso obtenerSiguienteProceso() {
        synchronized (lock) {
            return colaProcesos.poll();
        }
    }

    /**
     * Obtiene el siguiente proceso sin removerlo de la cola
     */
    public Proceso verSiguienteProceso() {
        synchronized (lock) {
            return colaProcesos.peek();
        }
    }

    /**
     * Obtiene todos los procesos de la cola
     */
    public List<Proceso> obtenerTodosLosProcesos() {
        synchronized (lock) {
            return new ArrayList<>(colaProcesos);
        }
    }

    /**
     * Inicia la ejecución de todos los procesos en cola
     */
    public void iniciarEjecucion() {
        synchronized (lock) {
            if (planificador == null) {
                System.err.println("❌ No se puede iniciar ejecución: No hay planificador asignado");
                return;
            }

            if (colaProcesos.isEmpty()) {
                System.out.println("⚠️ No hay procesos en cola para ejecutar");
                return;
            }

            activo = true;
            tiempoInicioOperacion = System.currentTimeMillis();

            System.out.println("🚀 Iniciando ejecución de " + colaProcesos.size() + " procesos");

            // Convertir cola a lista para el planificador
            List<Proceso> procesosParaEjecutar = new ArrayList<>(colaProcesos);
            colaProcesos.clear();

            // Mover procesos a ejecución
            procesosEnEjecucion.addAll(procesosParaEjecutar);

            // Ejecutar con el planificador
            planificador.ejecutarProcesos(procesosParaEjecutar);
        }
    }

    /**
     * Marca un proceso como completado
     */
    public void marcarProcesoCompletado(Proceso proceso) {
        if (proceso == null) {
            return;
        }

        synchronized (lock) {
            // Remover de ejecución y agregar a completados
            procesosEnEjecucion.remove(proceso);
            procesosCompletados.add(proceso);
            procesosCompletadosCount++;

            proceso.setEstado(EstadoProceso.TERMINADO);

            System.out.println("✅ Proceso " + proceso.getId() + " marcado como completado");

            // Verificar si todos los procesos han terminado
            if (procesosEnEjecucion.isEmpty() && colaProcesos.isEmpty()) {
                finalizarOperacion();
            }
        }
    }

    /**
     * Devuelve un proceso a la cola (para algoritmos como Round Robin)
     */
    public void devolverProcesoACola(Proceso proceso) {
        if (proceso == null || proceso.haTerminado()) {
            return;
        }

        synchronized (lock) {
            colaProcesos.offer(proceso);
            proceso.setEstado(EstadoProceso.LISTO);

            System.out.println("🔄 Proceso " + proceso.getId() + " devuelto a la cola");
        }
    }

    /**
     * Pausa la ejecución de todos los procesos
     */
    public void pausarEjecucion() {
        synchronized (lock) {
            activo = false;

            // Pausar procesos en ejecución
            for (Proceso proceso : procesosEnEjecucion) {
                if (proceso.estaEjecutando()) {
                    proceso.pausar();
                }
            }

            System.out.println("⏸️ Ejecución pausada");
        }
    }

    /**
     * Detiene completamente la ejecución
     */
    public void detenerEjecucion() {
        synchronized (lock) {
            activo = false;

            // Mover todos los procesos en ejecución de vuelta a la cola
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

    /**
     * Reinicia el manejador de procesos
     */
    public void reiniciar() {
        synchronized (lock) {
            // Detener ejecución actual
            detenerEjecucion();

            // Limpiar todas las colas
            colaProcesos.clear();
            procesosCompletados.clear();
            procesosEnEjecucion.clear();

            // Reiniciar estadísticas
            procesosTotales = 0;
            procesosCompletadosCount = 0;
            tiempoTotalOperacion = 0;
            activo = false;

            System.out.println("🔄 ManejoProcesos reiniciado");
        }
    }

    /**
     * Finaliza la operación actual
     */
    private void finalizarOperacion() {
        activo = false;
        tiempoTotalOperacion = System.currentTimeMillis() - tiempoInicioOperacion;

        System.out.println("🏁 Operación finalizada en " + tiempoTotalOperacion + "ms");
        System.out.println("📊 Procesos completados: " + procesosCompletadosCount + "/" + procesosTotales);
    }

    /**
     * Obtiene estadísticas del manejo de procesos
     */
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

    /**
     * Obtiene el estado actual del manejador
     */
    public String getEstadoActual() {
        synchronized (lock) {
            StringBuilder estado = new StringBuilder();
            estado.append("=== ESTADO MANEJO PROCESOS ===\n");
            estado.append("Activo: ").append(activo ? "SÍ" : "NO").append("\n");
            estado.append("Planificador: ").append(planificador != null ? planificador.getClass().getSimpleName() : "NINGUNO").append("\n");
            estado.append("Procesos en cola: ").append(colaProcesos.size()).append("\n");
            estado.append("Procesos en ejecución: ").append(procesosEnEjecucion.size()).append("\n");
            estado.append("Procesos completados: ").append(procesosCompletadosCount).append("\n");

            if (!colaProcesos.isEmpty()) {
                estado.append("\nPróximos en cola:\n");
                int count = 0;
                for (Proceso p : colaProcesos) {
                    if (count >= 5) break; // Mostrar solo los primeros 5
                    estado.append("  - ").append(p.toString()).append("\n");
                    count++;
                }
            }

            return estado.toString();
        }
    }

    // Getters
    public int getTamanoCola() {
        synchronized (lock) {
            return colaProcesos.size();
        }
    }

    public int getProcesosEnEjecucion() {
        synchronized (lock) {
            return procesosEnEjecucion.size();
        }
    }

    public int getProcesosCompletados() {
        synchronized (lock) {
            return procesosCompletadosCount;
        }
    }

    public int getProcesosTotales() {
        synchronized (lock) {
            return procesosTotales;
        }
    }

    public boolean isActivo() {
        synchronized (lock) {
            return activo;
        }
    }

    public boolean tieneProcesosPendientes() {
        synchronized (lock) {
            return !colaProcesos.isEmpty() || !procesosEnEjecucion.isEmpty();
        }
    }

    public List<Proceso> getProcesosCompletadosList() {
        synchronized (lock) {
            return new ArrayList<>(procesosCompletados);
        }
    }

    public List<Proceso> getProcesosEnEjecucionList() {
        synchronized (lock) {
            return new ArrayList<>(procesosEnEjecucion);
        }
    }

    public Planificacion getPlanificador() {
        synchronized (lock) {
            return planificador;
        }
    }

    public long getTiempoTotalOperacion() {
        synchronized (lock) {
            return tiempoTotalOperacion;
        }
    }

    /**
     * Verifica si la cola está vacía
     */
    public boolean colaVacia() {
        synchronized (lock) {
            return colaProcesos.isEmpty();
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