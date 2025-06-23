package org.example.proyectoso.models;


public class Core {
    // Identificación
    private final int id;
    private final String nombre;

    // Estado del core
    private boolean ocupado;
    private Proceso procesoActual;
    private long tiempoInicioEjecucion;
    private long tiempoTotalEjecucion;

    // Configuración
    private int quantum;
    private final Object lock = new Object();

    // Estadísticas
    private int procesosEjecutados;
    private long tiempoInactivo;
    private long ultimoCambioEstado;


    public Core(int id) {
        this.id = id;
        this.nombre = "Core-" + id;
        this.ocupado = false;
        this.procesoActual = null;
        this.tiempoInicioEjecucion = 0;
        this.tiempoTotalEjecucion = 0;
        this.quantum = 100; // Quantum por defecto en ms
        this.procesosEjecutados = 0;
        this.tiempoInactivo = 0;
        this.ultimoCambioEstado = System.currentTimeMillis();
    }


    public boolean asignarProceso(Proceso proceso) {
        synchronized (lock) {
            if (ocupado || proceso == null) {
                return false;
            }

            this.procesoActual = proceso;
            this.ocupado = true;
            this.tiempoInicioEjecucion = System.currentTimeMillis();

            // Actualizar estadísticas de inactividad
            long tiempoActual = System.currentTimeMillis();
            if (!ocupado) {
                tiempoInactivo += (tiempoActual - ultimoCambioEstado);
            }
            ultimoCambioEstado = tiempoActual;

            // Iniciar ejecución del proceso
            proceso.iniciarEjecucion(tiempoActual);

            System.out.println("🔧 " + nombre + " asignado a Proceso " + proceso.getId());
            return true;
        }
    }


    public boolean ejecutarQuantum() {
        synchronized (lock) {
            if (!ocupado || procesoActual == null) {
                return false;
            }

            long tiempoActual = System.currentTimeMillis();
            boolean procesoTerminado = procesoActual.ejecutar(quantum, tiempoActual);

            if (procesoTerminado) {
                finalizarProceso();
                return true;
            }

            // Si no terminó, pausar el proceso (fin de quantum)
            procesoActual.pausar();
            return false;
        }
    }


    public boolean ejecutarHastaCompletar() {
        synchronized (lock) {
            if (!ocupado || procesoActual == null) {
                return false;
            }

            long tiempoActual = System.currentTimeMillis();

            // Ejecutar hasta que termine
            while (!procesoActual.haTerminado()) {
                boolean terminado = procesoActual.ejecutar(
                        procesoActual.getTiempoRestante(),
                        System.currentTimeMillis()
                );

                if (terminado) {
                    break;
                }
            }

            finalizarProceso();
            return true;
        }
    }


    public Proceso interrumpir() {
        synchronized (lock) {
            if (!ocupado || procesoActual == null) {
                return null;
            }

            Proceso proceso = procesoActual;
            proceso.pausar();

            liberarCore();

            System.out.println("⏸️ " + nombre + " interrumpido, Proceso " +
                    proceso.getId() + " pausado");
            return proceso;
        }
    }


    private void finalizarProceso() {
        if (procesoActual != null) {
            long tiempoEjecucion = System.currentTimeMillis() - tiempoInicioEjecucion;
            tiempoTotalEjecucion += tiempoEjecucion;
            procesosEjecutados++;

            System.out.println("✅ " + nombre + " finalizó Proceso " +
                    procesoActual.getId() + " (Tiempo: " + tiempoEjecucion + "ms)");
        }

        liberarCore();
    }


    public void liberarCore() {
        this.procesoActual = null;
        this.ocupado = false;
        this.tiempoInicioEjecucion = 0;
        this.ultimoCambioEstado = System.currentTimeMillis();
    }


    public void forzarLiberacion() {
        synchronized (lock) {
            if (procesoActual != null) {
                procesoActual.pausar();
            }
            liberarCore();
            System.out.println("🔓 " + nombre + " liberado forzosamente");
        }
    }


    public void setQuantum(int quantum) {
        synchronized (lock) {
            this.quantum = Math.max(1, quantum); // Mínimo 1ms
        }
    }

    public String getEstadoActual() {
        synchronized (lock) {
            if (!ocupado) {
                return nombre + ": LIBRE";
            } else {
                return String.format("%s: Ejecutando Proceso %d (%.1f%%)",
                        nombre, procesoActual.getId(),
                        procesoActual.getPorcentajeCompletitud());
            }
        }
    }


    public String getEstadisticas() {
        synchronized (lock) {
            long tiempoTotal = tiempoTotalEjecucion + tiempoInactivo;
            double porcentajeUso = tiempoTotal > 0 ?
                    (double) tiempoTotalEjecucion / tiempoTotal * 100 : 0;

            return String.format(
                    "%s - Procesos: %d, Uso: %.1f%%, Tiempo total: %dms",
                    nombre, procesosEjecutados, porcentajeUso, tiempoTotalEjecucion
            );
        }
    }


    public void reiniciarEstadisticas() {
        synchronized (lock) {
            procesosEjecutados = 0;
            tiempoTotalEjecucion = 0;
            tiempoInactivo = 0;
            ultimoCambioEstado = System.currentTimeMillis();
        }
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isOcupado() {
        synchronized (lock) {
            return ocupado;
        }
    }

    public boolean isLibre() {
        synchronized (lock) {
            return !ocupado;
        }
    }




    public double getPorcentajeUso() {
        synchronized (lock) {
            long tiempoTotal = tiempoTotalEjecucion + tiempoInactivo;
            return tiempoTotal > 0 ? (double) tiempoTotalEjecucion / tiempoTotal * 100 : 0;
        }
    }

    public long getTiempoInicioEjecucion() {
        synchronized (lock) {
            return tiempoInicioEjecucion;
        }
    }

    @Override
    public String toString() {
        synchronized (lock) {
            if (ocupado && procesoActual != null) {
                return String.format("%s[Proceso %d - %.1f%%]",
                        nombre, procesoActual.getId(),
                        procesoActual.getPorcentajeCompletitud());
            } else {
                return nombre + "[LIBRE]";
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Core core = (Core) obj;
        return id == core.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}