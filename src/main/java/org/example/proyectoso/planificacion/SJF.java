package org.example.proyectoso.planificacion;

import org.example.proyectoso.models.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del algoritmo de planificación Shortest Job First (SJF)
 * Ejecuta los procesos en orden de menor a mayor tiempo de ráfaga
 */
public class SJF extends Planificacion {

    // Configuración del algoritmo
    private boolean preemptivo;
    private final Object sjfLock = new Object();

    // Control de ejecución
    private volatile boolean pausado;
    private Thread hiloEjecucion;

    // Estadísticas específicas de SJF
    private int cambiosContexto;
    private long tiempoPromedioEspera;
    private long tiempoPromedioRespuesta;

    /**
     * Constructor para SJF no preemptivo (por defecto)
     */
    public SJF() {
        this(false);
    }

    /**
     * Constructor que permite configurar si es preemptivo o no
     * @param preemptivo true para SJF preemptivo (SRTF), false para SJF no preemptivo
     */
    public SJF(boolean preemptivo) {
        super();
        this.preemptivo = preemptivo;
        this.pausado = false;
        this.cambiosContexto = 0;
        this.tiempoPromedioEspera = 0;
        this.tiempoPromedioRespuesta = 0;

        System.out.println("🔧 SJF inicializado - Modo: " +
                (preemptivo ? "Preemptivo (SRTF)" : "No Preemptivo"));
    }

    @Override
    public String getNombreAlgoritmo() {
        return preemptivo ? "SJF Preemptivo (SRTF)" : "SJF No Preemptivo";
    }

    @Override
    public void ejecutarProcesos(List<Proceso> procesos) {
        if (procesos == null || procesos.isEmpty()) {
            System.out.println("⚠️ No hay procesos para ejecutar en SJF");
            return;
        }

        iniciarEjecucion(procesos);

        // Crear hilo de ejecución
        hiloEjecucion = new Thread(() -> {
            try {
                if (preemptivo) {
                    ejecutarSJFPreemptivo(new ArrayList<>(procesos));
                } else {
                    ejecutarSJFNoPreemptivo(new ArrayList<>(procesos));
                }
            } catch (Exception e) {
                System.err.println("❌ Error en ejecución SJF: " + e.getMessage());
                e.printStackTrace();
            } finally {
                finalizarEjecucion();
            }
        });

        hiloEjecucion.setName("SJF-Executor");
        hiloEjecucion.start();
    }

    /**
     * Implementación SJF No Preemptivo
     */
    private void ejecutarSJFNoPreemptivo(List<Proceso> procesos) {
        synchronized (sjfLock) {
            System.out.println("🚀 Iniciando SJF No Preemptivo con " + procesos.size() + " procesos");

            // Ordenar procesos por tiempo de ráfaga
            List<Proceso> procesosOrdenados = ordenarProcesos(procesos);

            for (Proceso proceso : procesosOrdenados) {
                if (!ejecutando || pausado) {
                    break;
                }

                ejecutarProcesoCompleto(proceso);
                procesosEjecutados++;

                // Pausa breve entre procesos
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            calcularEstadisticas(procesosOrdenados);
        }
    }

    /**
     * Implementación SJF Preemptivo (SRTF - Shortest Remaining Time First)
     */
    private void ejecutarSJFPreemptivo(List<Proceso> procesos) {
        synchronized (sjfLock) {
            System.out.println("🚀 Iniciando SJF Preemptivo (SRTF) con " + procesos.size() + " procesos");

            Queue<Proceso> colaListos = new PriorityQueue<>(
                    Comparator.comparingInt(Proceso::getTiempoRestante)
            );

            // Agregar procesos a la cola
            colaListos.addAll(procesos);

            while (!colaListos.isEmpty() && ejecutando && !pausado) {
                // Obtener proceso con menor tiempo restante
                Proceso procesoActual = colaListos.poll();

                if (procesoActual == null || procesoActual.haTerminado()) {
                    continue;
                }

                // Ejecutar por un quantum pequeño (simulando preempción)
                int tiempoEjecucion = Math.min(100, procesoActual.getTiempoRestante());

                ejecutarProcesoParcial(procesoActual, tiempoEjecucion);

                // Si el proceso no ha terminado, devolverlo a la cola
                if (!procesoActual.haTerminado()) {
                    colaListos.offer(procesoActual);
                    cambiosContexto++;
                } else {
                    procesosEjecutados++;
                }

                // Verificar si hay procesos con menor tiempo restante
                reordenarCola(colaListos);

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
     * Ejecuta un proceso completo sin preempción
     */
    private void ejecutarProcesoCompleto(Proceso proceso) {
        if (proceso == null || proceso.haTerminado()) {
            return;
        }

        // Buscar core libre
        Core coreLibre = esperarCoreLibre();
        if (coreLibre == null) {
            return;
        }

        try {
            System.out.println("▶️ Ejecutando proceso " + proceso.getId() +
                    " (Ráfaga: " + proceso.getDuracion() + "ms)");

            proceso.setEstado(EstadoProceso.EJECUTANDO);
            coreLibre.asignarProceso(proceso);

            // Simular ejecución completa
            int tiempoRestante = proceso.getTiempoRestante();
            while (tiempoRestante > 0 && ejecutando && !pausado) {
                int tiempoEjecutar = Math.min(50, tiempoRestante);

                try {
                    Thread.sleep(tiempoEjecutar);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                proceso.ejecutar(tiempoEjecutar, System.currentTimeMillis());
                tiempoRestante = proceso.getTiempoRestante();
            }

            if (proceso.haTerminado()) {
                proceso.setEstado(EstadoProceso.TERMINADO);
                System.out.println("✅ Proceso " + proceso.getId() + " completado");
            }

        } finally {
            coreLibre.liberarCore();
        }
    }

    /**
     * Ejecuta un proceso parcialmente (para modo preemptivo)
     */
    private void ejecutarProcesoParcial(Proceso proceso, int tiempoEjecucion) {
        if (proceso == null || proceso.haTerminado() || tiempoEjecucion <= 0) {
            return;
        }

        Core coreLibre = esperarCoreLibre();
        if (coreLibre == null) {
            return;
        }

        try {
            proceso.setEstado(EstadoProceso.EJECUTANDO);
            coreLibre.asignarProceso(proceso);

            System.out.println("▶️ Ejecutando proceso " + proceso.getId() +
                    " por " + tiempoEjecucion + "ms (Restante: " +
                    proceso.getTiempoRestante() + "ms)");

            // Simular ejecución parcial
            int tiempoEjecutado = 0;
            while (tiempoEjecutado < tiempoEjecucion &&
                    !proceso.haTerminado() && ejecutando && !pausado) {

                int paso = Math.min(10, tiempoEjecucion - tiempoEjecutado);

                try {
                    Thread.sleep(paso);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                proceso.ejecutar(paso, System.currentTimeMillis()  );
                tiempoEjecutado += paso;
            }

            if (proceso.haTerminado()) {
                proceso.setEstado(EstadoProceso.TERMINADO);
                System.out.println("✅ Proceso " + proceso.getId() + " completado");
            } else {
                proceso.setEstado(EstadoProceso.LISTO);
                System.out.println("⏸️ Proceso " + proceso.getId() + " suspendido");
            }

        } finally {
            coreLibre.liberarCore();
        }
    }

    @Override
    protected List<Proceso> ordenarProcesos(List<Proceso> procesos) {
        if (procesos == null) {
            return new ArrayList<>();
        }

        // Para SJF no preemptivo: ordenar por tiempo de ráfaga total
        // Para SJF preemptivo: ordenar por tiempo de ráfaga restante
        return procesos.stream()
                .filter(Objects::nonNull)
                .sorted((p1, p2) -> {
                    if (preemptivo) {
                        return Integer.compare(p1.getTiempoRestante(), p2.getTiempoRestante());
                    } else {
                        return Integer.compare(p1.getDuracion(), p2.getDuracion());
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Reordena la cola de procesos listos (para modo preemptivo)
     */
    private void reordenarCola(Queue<Proceso> cola) {
        if (cola.isEmpty()) {
            return;
        }

        List<Proceso> temp = new ArrayList<>(cola);
        cola.clear();

        temp.stream()
                .sorted(Comparator.comparingInt(Proceso::getTiempoRestante))
                .forEach(cola::offer);
    }

    /**
     * Calcula estadísticas específicas de SJF
     */
    private void calcularEstadisticas(List<Proceso> procesos) {
        List<Proceso> procesosTerminados = procesos.stream()
                .filter(Proceso::haTerminado)
                .collect(Collectors.toList());

        if (!procesosTerminados.isEmpty()) {
            tiempoPromedioEspera = (long) procesosTerminados.stream()
                    .mapToInt(Proceso::getTiempoEspera)
                    .average()
                    .orElse(0.0);

            tiempoPromedioRespuesta = (long) procesosTerminados.stream()
                    .mapToInt(Proceso::getTiempoRespuesta)
                    .average()
                    .orElse(0.0);
        }

        // En SJF no preemptivo, los cambios de contexto son igual al número de procesos - 1
        // (un cambio cada vez que termina un proceso y empieza otro, excepto el primero)
        if (!preemptivo && procesosTerminados.size() > 0) {
            cambiosContexto = procesosTerminados.size() - 1;
        }

        System.out.println("📊 Estadísticas SJF:");
        System.out.println("   - Cambios de contexto: " + cambiosContexto);
        System.out.println("   - Tiempo promedio de espera: " + tiempoPromedioEspera + "ms");
        System.out.println("   - Tiempo promedio de respuesta: " + tiempoPromedioRespuesta + "ms");
    }

    @Override
    public void pausar() {
        synchronized (sjfLock) {
            pausado = true;
            super.pausar();
        }
    }

    @Override
    public void reanudar() {
        synchronized (sjfLock) {
            pausado = false;
            super.reanudar();
        }
    }

    @Override
    public void detener() {
        synchronized (sjfLock) {
            super.detener();
            pausado = true;

            if (hiloEjecucion != null && hiloEjecucion.isAlive()) {
                hiloEjecucion.interrupt();
                try {
                    hiloEjecucion.join(1000); // Esperar máximo 1 segundo
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public String getEstadisticas() {
        StringBuilder stats = new StringBuilder(super.getEstadisticas());

        synchronized (sjfLock) {
            stats.append("Modo: ").append(preemptivo ? "Preemptivo (SRTF)" : "No Preemptivo").append("\n");
            stats.append("Cambios de contexto: ").append(cambiosContexto).append("\n");

            if (tiempoPromedioEspera > 0) {
                stats.append("Tiempo promedio espera: ").append(tiempoPromedioEspera).append("ms\n");
            }

            if (tiempoPromedioRespuesta > 0) {
                stats.append("Tiempo promedio respuesta: ").append(tiempoPromedioRespuesta).append("ms\n");
            }
        }

        return stats.toString();
    }

    @Override
    public String getEstadoActual() {
        StringBuilder estado = new StringBuilder(super.getEstadoActual());

        synchronized (sjfLock) {
            estado.append("Pausado: ").append(pausado ? "SÍ" : "NO").append("\n");
            estado.append("Cambios contexto: ").append(cambiosContexto).append("\n");

            if (hiloEjecucion != null) {
                estado.append("Hilo ejecución: ").append(hiloEjecucion.getState()).append("\n");
            }
        }

        return estado.toString();
    }

    // Getters específicos
    public boolean isPreemptivo() {
        return preemptivo;
    }

    public void setPreemptivo(boolean preemptivo) {
        synchronized (sjfLock) {
            this.preemptivo = preemptivo;
            System.out.println("🔄 SJF modo cambiado a: " +
                    (preemptivo ? "Preemptivo (SRTF)" : "No Preemptivo"));
        }
    }

    public boolean isPausado() {
        synchronized (sjfLock) {
            return pausado;
        }
    }

    public int getCambiosContexto() {
        synchronized (sjfLock) {
            return cambiosContexto;
        }
    }

    public long getTiempoPromedioEspera() {
        synchronized (sjfLock) {
            return tiempoPromedioEspera;
        }
    }

    public long getTiempoPromedioRespuesta() {
        synchronized (sjfLock) {
            return tiempoPromedioRespuesta;
        }
    }

    @Override
    public String toString() {
        synchronized (sjfLock) {
            return String.format("SJF[%s, %s, Procesos:%d, Contextos:%d]",
                    preemptivo ? "Preemptivo" : "No-Preemptivo",
                    ejecutando ? "EJECUTANDO" : "DETENIDO",
                    procesosEjecutados,
                    cambiosContexto);
        }
    }
}