package org.example.proyectoso.planificacion;
import org.example.proyectoso.models.*;

import java.util.List;


public abstract class Planificacion {
    
    protected List<Proceso> colaProcesos;

    
    protected CPU cpu;

    
    protected volatile boolean ejecutando;
    protected final Object lock = new Object();

    
    protected long tiempoInicioEjecucion;
    protected long tiempoFinEjecucion;
    protected int procesosEjecutados;

    
    public Planificacion() {
        this.ejecutando = false;
        this.procesosEjecutados = 0;
        this.tiempoInicioEjecucion = 0;
        this.tiempoFinEjecucion = 0;
    }

    
    public void setCpu(CPU cpu) {
        synchronized (lock) {
            this.cpu = cpu;
        }
    }

    
    public abstract void ejecutarProcesos(List<Proceso> procesos);

    
    public abstract String getNombreAlgoritmo();

    
    protected abstract List<Proceso> ordenarProcesos(List<Proceso> procesos);

    
    protected void iniciarEjecucion(List<Proceso> procesos) {
        synchronized (lock) {
            if (cpu == null) {
                throw new IllegalStateException("CPU no configurada");
            }

            if (procesos == null || procesos.isEmpty()) {
                System.out.println("⚠️ No hay procesos para ejecutar");
                return;
            }

            this.colaProcesos = procesos;
            this.ejecutando = true;
            this.tiempoInicioEjecucion = System.currentTimeMillis();
            this.procesosEjecutados = 0;

            System.out.println("🚀 Iniciando planificación " + getNombreAlgoritmo() +
                    " con " + procesos.size() + " procesos");
        }
    }

    
    protected void finalizarEjecucion() {
        synchronized (lock) {
            this.ejecutando = false;
            this.tiempoFinEjecucion = System.currentTimeMillis();

            long tiempoTotal = tiempoFinEjecucion - tiempoInicioEjecucion;

            System.out.println("✅ Planificación " + getNombreAlgoritmo() +
                    " completada en " + tiempoTotal + "ms");
            System.out.println("📊 Procesos ejecutados: " + procesosEjecutados);
        }
    }

    
    public void detener() {
        synchronized (lock) {
            ejecutando = false;

            if (cpu != null) {
                cpu.interrumpirTodos();
            }

            System.out.println("🛑 Planificación " + getNombreAlgoritmo() + " detenida");
        }
    }

    
    public void pausar() {
        synchronized (lock) {
            ejecutando = false;
            System.out.println("⏸️ Planificación " + getNombreAlgoritmo() + " pausada");
        }
    }

    
    public void reanudar() {
        synchronized (lock) {
            ejecutando = true;
            System.out.println("▶️ Planificación " + getNombreAlgoritmo() + " reanudada");
        }
    }

    
    protected Core esperarCoreLibre() {
        while (ejecutando) {
            Core coreLibre = cpu.getCoresLibres().stream().findFirst().orElse(null);

            if (coreLibre != null) {
                return coreLibre;
            }

            try {
                Thread.sleep(10); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        return null;
    }

    
    protected boolean todosProcesosTerminados(List<Proceso> procesos) {
        return procesos.stream().allMatch(Proceso::haTerminado);
    }

    
    protected List<Proceso> obtenerProcesosActivos(List<Proceso> procesos) {
        return procesos.stream()
                .filter(p -> !p.haTerminado())
                .collect(java.util.stream.Collectors.toList());
    }

    
    public String getEstadisticas() {
        synchronized (lock) {
            StringBuilder stats = new StringBuilder();
            stats.append("=== ESTADÍSTICAS ").append(getNombreAlgoritmo().toUpperCase()).append(" ===\n");
            stats.append("Algoritmo: ").append(getNombreAlgoritmo()).append("\n");
            stats.append("Estado: ").append(ejecutando ? "EJECUTANDO" : "DETENIDO").append("\n");
            stats.append("Procesos ejecutados: ").append(procesosEjecutados).append("\n");

            if (tiempoFinEjecucion > tiempoInicioEjecucion) {
                long tiempoTotal = tiempoFinEjecucion - tiempoInicioEjecucion;
                stats.append("Tiempo total: ").append(tiempoTotal).append("ms\n");

                if (procesosEjecutados > 0) {
                    stats.append("Promedio por proceso: ")
                            .append(tiempoTotal / procesosEjecutados)
                            .append("ms\n");
                }
            }

            return stats.toString();
        }
    }

    
    public String getEstadoActual() {
        synchronized (lock) {
            StringBuilder estado = new StringBuilder();
            estado.append("=== ESTADO ").append(getNombreAlgoritmo().toUpperCase()).append(" ===\n");
            estado.append("Algoritmo: ").append(getNombreAlgoritmo()).append("\n");
            estado.append("Ejecutando: ").append(ejecutando ? "SÍ" : "NO").append("\n");
            estado.append("CPU asignada: ").append(cpu != null ? cpu.getNombre() : "NINGUNA").append("\n");
            estado.append("Procesos en cola: ").append(colaProcesos != null ? colaProcesos.size() : 0).append("\n");

            if (cpu != null) {
                estado.append("Cores libres: ").append(cpu.getCoresLibresCount()).append("/").append(cpu.getNumeroCores()).append("\n");
            }

            return estado.toString();
        }
    }

    
    protected String calcularMetricas(List<Proceso> procesos) {
        if (procesos == null || procesos.isEmpty()) {
            return "No hay procesos para calcular métricas";
        }

        List<Proceso> procesosTerminados = procesos.stream()
                .filter(Proceso::haTerminado)
                .collect(java.util.stream.Collectors.toList());

        if (procesosTerminados.isEmpty()) {
            return "Ningún proceso ha terminado aún";
        }

        double promedioTiempoEspera = procesosTerminados.stream()
                .mapToInt(Proceso::getTiempoEspera)
                .average()
                .orElse(0.0);

        double promedioTiempoRespuesta = procesosTerminados.stream()
                .mapToInt(Proceso::getTiempoRespuesta)
                .average()
                .orElse(0.0);

        double promedioTiempoRetorno = procesosTerminados.stream()
                .mapToInt(Proceso::getTiempoRetorno)
                .average()
                .orElse(0.0);

        StringBuilder metricas = new StringBuilder();
        metricas.append("=== MÉTRICAS DE RENDIMIENTO ===\n");
        metricas.append("Procesos analizados: ").append(procesosTerminados.size()).append("\n");
        metricas.append("Tiempo promedio de espera: ").append(String.format("%.2f", promedioTiempoEspera)).append("ms\n");
        metricas.append("Tiempo promedio de respuesta: ").append(String.format("%.2f", promedioTiempoRespuesta)).append("ms\n");
        metricas.append("Tiempo promedio de retorno: ").append(String.format("%.2f", promedioTiempoRetorno)).append("ms\n");

        return metricas.toString();
    }

    
    public boolean isEjecutando() {
        synchronized (lock) {
            return ejecutando;
        }
    }

    public CPU getCpu() {
        synchronized (lock) {
            return cpu;
        }
    }

    public int getProcesosEjecutados() {
        synchronized (lock) {
            return procesosEjecutados;
        }
    }

    public long getTiempoEjecucion() {
        synchronized (lock) {
            if (tiempoFinEjecucion > tiempoInicioEjecucion) {
                return tiempoFinEjecucion - tiempoInicioEjecucion;
            } else if (ejecutando && tiempoInicioEjecucion > 0) {
                return System.currentTimeMillis() - tiempoInicioEjecucion;
            }
            return 0;
        }
    }

    @Override
    public String toString() {
        synchronized (lock) {
            return String.format("%s[%s, Procesos:%d]",
                    getNombreAlgoritmo(),
                    ejecutando ? "EJECUTANDO" : "DETENIDO",
                    procesosEjecutados);
        }
    }
}