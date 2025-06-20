package org.example.proyectoso.planificacion;

import org.example.proyectoso.models.*;
import java.util.*;

/**
 * Implementación del algoritmo de planificación Round Robin
 * Ejecuta los procesos de manera cíclica con un quantum fijo
 */
public class RoundRobin extends Planificacion {

    // Quantum de tiempo para cada ejecución parcial (en ms)
    private final int quantum;
    private final Object rrLock = new Object();

    // Control de ejecución
    private volatile boolean pausado;
    private Thread hiloEjecucion;

    // Estadísticas específicas de Round Robin
    private int cambiosContexto;
    private long tiempoPromedioEspera;
    private long tiempoPromedioRespuesta;

    /**
     * Constructor de Round Robin con quantum por defecto de 100ms
     */
    public RoundRobin() {
        this(100);
    }

    /**
     * Constructor de Round Robin con quantum configurable
     * @param quantum Tiempo de ejecución por turno (ms)
     */
    public RoundRobin(int quantum) {
        super();
        this.quantum = quantum;
        this.pausado = false;
        this.cambiosContexto = 0;
        this.tiempoPromedioEspera = 0;
        this.tiempoPromedioRespuesta = 0;
        System.out.println("🔧 Round Robin inicializado - Quantum: " + quantum + "ms");
    }

    @Override
    public String getNombreAlgoritmo() {
        return "Round Robin (quantum=" + quantum + "ms)";
    }

    @Override
    protected List<Proceso> ordenarProcesos(List<Proceso> procesos) {
        return List.of();
    }

    @Override
    public void ejecutarProcesos(List<Proceso> procesos) {
        if (procesos == null || procesos.isEmpty()) {
            System.out.println("⚠️ No hay procesos para ejecutar en Round Robin");
            return;
        }

        iniciarEjecucion(procesos);
        hiloEjecucion = new Thread(() -> {
            try {
                ejecutarRoundRobin(new ArrayList<>(procesos));
            } catch (Exception e) {
                System.err.println("❌ Error en ejecución Round Robin: " + e.getMessage());
                e.printStackTrace();
            } finally {
                finalizarEjecucion();
            }
        });
        hiloEjecucion.setName("RR-Executor");
        hiloEjecucion.start();
    }

    /**
     * Implementación de Round Robin
     */
    private void ejecutarRoundRobin(List<Proceso> procesos) {
        synchronized (rrLock) {
            System.out.println("🚀 Iniciando Round Robin con " + procesos.size() + " procesos (quantum=" + quantum + "ms)");
            Queue<Proceso> cola = new LinkedList<>(procesos);

            while (!cola.isEmpty() && ejecutando && !pausado) {
                Proceso proceso = cola.poll();
                if (proceso == null || proceso.haTerminado()) {
                    continue;
                }

                // Ejecutar por quantum o tiempo restante
                int tiempoEjecucion = Math.min(quantum, proceso.getTiempoRestante());
                ejecutarProcesoParcialRR(proceso, tiempoEjecucion);

                if (!proceso.haTerminado()) {
                    cola.offer(proceso);
                    cambiosContexto++;
                } else {
                    procesosEjecutados++;
                }

                // Pausa breve para simular cambio de contexto
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            calcularEstadisticas(procesos);
        }
    }

    /**
     * Ejecuta un proceso parcialmente en Round Robin
     */
    private void ejecutarProcesoParcialRR(Proceso proceso, int tiempoEjecucion) {
        if (proceso == null || proceso.haTerminado() || tiempoEjecucion <= 0) {
            return;
        }

        Core core = esperarCoreLibre();
        if (core == null) {
            return;
        }

        try {
            proceso.setEstado(EstadoProceso.EJECUTANDO);
            core.asignarProceso(proceso);
            System.out.println("▶️ RR ejecutando proceso " + proceso.getId() + " por " + tiempoEjecucion + "ms (Restante: " + proceso.getTiempoRestante() + "ms)");

            int ejecutado = 0;
            while (ejecutado < tiempoEjecucion && !proceso.haTerminado() && ejecutando && !pausado) {
                int paso = Math.min(10, tiempoEjecucion - ejecutado);
                try {
                    Thread.sleep(paso);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                proceso.ejecutar(paso, System.currentTimeMillis());
                ejecutado += paso;
            }

            if (proceso.haTerminado()) {
                proceso.setEstado(EstadoProceso.TERMINADO);
                System.out.println("✅ Proceso " + proceso.getId() + " completado");
            } else {
                proceso.setEstado(EstadoProceso.LISTO);
                System.out.println("⏸️ Proceso " + proceso.getId() + " suspendido");
            }
        } finally {
            core.liberarCore();
        }
    }

    /**
     * Calcula estadísticas de Round Robin
     */
    private void calcularEstadisticas(List<Proceso> procesos) {
        List<Proceso> terminados = procesos.stream().filter(Proceso::haTerminado).toList();
        if (!terminados.isEmpty()) {
            tiempoPromedioEspera = (long) terminados.stream().mapToInt(Proceso::getTiempoEspera).average().orElse(0.0);
            tiempoPromedioRespuesta = (long) terminados.stream().mapToInt(Proceso::getTiempoRespuesta).average().orElse(0.0);
        }
        System.out.println("📊 Estadísticas RR:");
        System.out.println("   - Cambios de contexto: " + cambiosContexto);
        System.out.println("   - Tiempo promedio espera: " + tiempoPromedioEspera + "ms");
        System.out.println("   - Tiempo promedio respuesta: " + tiempoPromedioRespuesta + "ms");
    }

    @Override
    public void pausar() {
        synchronized (rrLock) {
            pausado = true;
            super.pausar();
        }
    }

    @Override
    public void reanudar() {
        synchronized (rrLock) {
            pausado = false;
            super.reanudar();
        }
    }

    @Override
    public void detener() {
        synchronized (rrLock) {
            super.detener();
            pausado = true;
            if (hiloEjecucion != null && hiloEjecucion.isAlive()) {
                hiloEjecucion.interrupt();
                try {
                    hiloEjecucion.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public String getEstadisticas() {
        StringBuilder stats = new StringBuilder(super.getEstadisticas());
        synchronized (rrLock) {
            stats.append("Quantum: ").append(quantum).append("ms\n");
            stats.append("Cambios de contexto: ").append(cambiosContexto).append("\n");
            stats.append("T.promedio espera: ").append(tiempoPromedioEspera).append("ms\n");
            stats.append("T.promedio respuesta: ").append(tiempoPromedioRespuesta).append("ms\n");
        }
        return stats.toString();
    }

    @Override
    public String getEstadoActual() {
        StringBuilder estado = new StringBuilder(super.getEstadoActual());
        synchronized (rrLock) {
            estado.append("Pausado: ").append(pausado ? "SÍ" : "NO").append("\n");
            estado.append("Cambios contexto: ").append(cambiosContexto).append("\n");
            if (hiloEjecucion != null) {
                estado.append("Hilo ejecución: ").append(hiloEjecucion.getState()).append("\n");
            }
        }
        return estado.toString();
    }

    @Override
    public String toString() {
        synchronized (rrLock) {
            return String.format("RoundRobin[Q=%dms, %s, Procesos:%d, Contextos:%d]", quantum,
                    ejecutando ? "EJECUTANDO" : "DETENIDO", procesosEjecutados, cambiosContexto);
        }
    }

    public int getQuantum() {
        return quantum;
    }

    public boolean isPausado() {
        synchronized (rrLock) {
            return pausado;
        }
    }

    public int getCambiosContexto() {
        synchronized (rrLock) {
            return cambiosContexto;
        }
    }

    public long getTiempoPromedioEspera() {
        synchronized (rrLock) {
            return tiempoPromedioEspera;
        }
    }

    public long getTiempoPromedioRespuesta() {
        synchronized (rrLock) {
            return tiempoPromedioRespuesta;
        }
    }
}
