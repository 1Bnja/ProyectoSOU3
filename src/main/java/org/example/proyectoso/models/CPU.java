package org.example.proyectoso.models;

import java.util.*;
import java.util.concurrent.*;

/**
 * Clase que representa la CPU con múltiples cores
 * Gestiona la ejecución de procesos en paralelo
 */
public class CPU {
    // Configuración de la CPU
    private final List<Core> cores;
    private final int numeroCores;
    private final String nombre;

    // Control de ejecución
    private final Object lock = new Object();
    private volatile boolean ejecutando = false;
    private ExecutorService executorService;

    // Configuración de algoritmos
    private int quantumRoundRobin = 100; // ms
    private TipoAlgoritmo algoritmoActual = TipoAlgoritmo.ROUND_ROBIN;

    // Estadísticas generales
    private long tiempoInicioOperacion;
    private long tiempoTotalOperacion;
    private int procesosTotalesEjecutados;

    /**
     * Enumeración para tipos de algoritmos de planificación
     */
    public enum TipoAlgoritmo {
        ROUND_ROBIN,
        FCFS,           // First Come First Served
        SJF,            // Shortest Job First
        PRIORITY        // Basado en prioridades
    }

    /**
     * Constructor
     */
    public CPU(int numeroCores) {
        this.numeroCores = Math.max(1, numeroCores);
        this.nombre = "CPU-" + this.numeroCores + "Core";
        this.cores = new ArrayList<>();

        // Inicializar cores
        for (int i = 0; i < this.numeroCores; i++) {
            cores.add(new Core(i));
        }

        // Inicializar executor service
        this.executorService = Executors.newFixedThreadPool(this.numeroCores);

        System.out.println("🔧 " + nombre + " inicializada con " + numeroCores + " cores");
    }

    /**
     * Constructor con nombre personalizado
     */
    public CPU(int numeroCores, String nombre) {
        this(numeroCores);
        // No se puede cambiar el nombre después de la inicialización de cores,
        // pero se puede mostrar información personalizada
    }

    /**
     * Ejecuta un proceso en el primer core disponible
     */
    public boolean ejecutarProceso(Proceso proceso) {
        synchronized (lock) {
            Core coreLibre = obtenerCoreLibre();

            if (coreLibre != null) {
                return ejecutarEnCore(proceso, coreLibre);
            }

            return false; // No hay cores disponibles
        }
    }

    /**
     * Ejecuta un proceso en un core específico
     */
    public boolean ejecutarEnCore(Proceso proceso, Core core) {
        if (proceso == null || core == null) {
            return false;
        }

        // Configurar quantum según el algoritmo
        core.setQuantum(quantumRoundRobin);

        // Asignar proceso al core
        if (core.asignarProceso(proceso)) {
            // Ejecutar en un hilo separado
            executorService.submit(() -> {
                try {
                    switch (algoritmoActual) {
                        case ROUND_ROBIN:
                            core.ejecutarQuantum();
                            break;
                        case FCFS:
                        case SJF:
                        case PRIORITY:
                            core.ejecutarHastaCompletar();
                            break;
                    }
                } catch (Exception e) {
                    System.err.println("Error ejecutando proceso: " + e.getMessage());
                }
            });

            return true;
        }

        return false;
    }

    /**
     * Ejecuta una lista de procesos usando el algoritmo configurado
     */
    public void ejecutarProcesos(List<Proceso> procesos) {
        if (procesos == null || procesos.isEmpty()) {
            return;
        }

        synchronized (lock) {
            ejecutando = true;
            tiempoInicioOperacion = System.currentTimeMillis();
        }

        System.out.println("🚀 Iniciando ejecución de " + procesos.size() +
                " procesos con algoritmo " + algoritmoActual);

        try {
            switch (algoritmoActual) {
                case ROUND_ROBIN:
                    ejecutarRoundRobin(procesos);
                    break;
                case FCFS:
                    ejecutarFCFS(procesos);
                    break;
                case SJF:
                    ejecutarSJF(procesos);
                    break;
                case PRIORITY:
                    ejecutarPorPrioridad(procesos);
                    break;
            }
        } finally {
            synchronized (lock) {
                ejecutando = false;
                tiempoTotalOperacion = System.currentTimeMillis() - tiempoInicioOperacion;
                procesosTotalesEjecutados += procesos.size();
            }
        }

        System.out.println("✅ Ejecución completada en " + tiempoTotalOperacion + "ms");
    }

    /**
     * Implementación de Round Robin
     */
    private void ejecutarRoundRobin(List<Proceso> procesos) {
        Queue<Proceso> colaProcesos = new LinkedList<>(procesos);

        while (!colaProcesos.isEmpty()) {
            // Asignar procesos a cores disponibles
            List<Future<Boolean>> futures = new ArrayList<>();

            for (Core core : cores) {
                if (core.isLibre() && !colaProcesos.isEmpty()) {
                    Proceso proceso = colaProcesos.poll();

                    Future<Boolean> future = executorService.submit(() -> {
                        core.asignarProceso(proceso);
                        boolean terminado = core.ejecutarQuantum();

                        // Si no terminó, regresar a la cola
                        if (!terminado && !proceso.haTerminado()) {
                            synchronized (colaProcesos) {
                                colaProcesos.offer(proceso);
                            }
                        }

                        return terminado;
                    });

                    futures.add(future);
                }
            }

            // Esperar a que terminen los quantum actuales
            for (Future<Boolean> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("Error en ejecución: " + e.getMessage());
                }
            }

            // Pequeña pausa para evitar busy waiting
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Implementación de First Come First Served
     */
    private void ejecutarFCFS(List<Proceso> procesos) {
        // Ordenar por tiempo de llegada
        List<Proceso> procesosOrdenados = new ArrayList<>(procesos);
        procesosOrdenados.sort(Comparator.comparingInt(Proceso::getTiempoLlegada));

        for (Proceso proceso : procesosOrdenados) {
            // Esperar un core libre
            Core coreLibre = esperarCoreLibre();
            ejecutarEnCore(proceso, coreLibre);

            // Esperar a que termine
            while (coreLibre.isOcupado()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * Implementación de Shortest Job First
     */
    private void ejecutarSJF(List<Proceso> procesos) {
        // Ordenar por duración (tiempo de ejecución)
        List<Proceso> procesosOrdenados = new ArrayList<>(procesos);
        procesosOrdenados.sort(Comparator.comparingInt(Proceso::getDuracion));

        ejecutarFCFS(procesosOrdenados); // Misma lógica pero diferente orden
    }

    /**
     * Implementación por prioridad (usando tamaño de memoria como prioridad)
     */
    private void ejecutarPorPrioridad(List<Proceso> procesos) {
        // Ordenar por tamaño de memoria (menor tamaño = mayor prioridad)
        List<Proceso> procesosOrdenados = new ArrayList<>(procesos);
        procesosOrdenados.sort(Comparator.comparingInt(Proceso::getTamanoMemoria));

        ejecutarFCFS(procesosOrdenados);
    }

    /**
     * Espera hasta que haya un core libre disponible
     */
    private Core esperarCoreLibre() {
        while (true) {
            Core coreLibre = obtenerCoreLibre();
            if (coreLibre != null) {
                return coreLibre;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return cores.get(0); // Retornar el primer core en caso de interrupción
            }
        }
    }

    /**
     * Obtiene el primer core libre disponible
     */
    private Core obtenerCoreLibre() {
        synchronized (lock) {
            return cores.stream()
                    .filter(Core::isLibre)
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * Obtiene todos los cores libres
     */
    public List<Core> getCoresLibres() {
        synchronized (lock) {
            return cores.stream()
                    .filter(Core::isLibre)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
    }

    /**
     * Obtiene todos los cores ocupados
     */
    public List<Core> getCoresOcupados() {
        synchronized (lock) {
            return cores.stream()
                    .filter(Core::isOcupado)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
    }

    /**
     * Interrumpe todos los procesos en ejecución
     */
    public List<Proceso> interrumpirTodos() {
        synchronized (lock) {
            List<Proceso> procesosInterrumpidos = new ArrayList<>();

            for (Core core : cores) {
                if (core.isOcupado()) {
                    Proceso proceso = core.interrumpir();
                    if (proceso != null) {
                        procesosInterrumpidos.add(proceso);
                    }
                }
            }

            System.out.println("⏸️ " + procesosInterrumpidos.size() + " procesos interrumpidos");
            return procesosInterrumpidos;
        }
    }

    /**
     * Detiene la CPU y libera recursos
     */
    public void detener() {
        synchronized (lock) {
            ejecutando = false;

            // Interrumpir todos los procesos
            interrumpirTodos();

            // Shutdown del executor service
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("🛑 " + nombre + " detenida");
        }
    }

    /**
     * Reinicia la CPU
     */
    public void reiniciar() {
        synchronized (lock) {
            detener();

            // Reiniciar executor service
            executorService = Executors.newFixedThreadPool(numeroCores);

            // Reiniciar estadísticas de cores
            for (Core core : cores) {
                core.forzarLiberacion();
                core.reiniciarEstadisticas();
            }

            // Reiniciar estadísticas generales
            tiempoTotalOperacion = 0;
            procesosTotalesEjecutados = 0;

            System.out.println("🔄 " + nombre + " reiniciada");
        }
    }

    /**
     * Configura el algoritmo de planificación
     */
    public void setAlgoritmo(TipoAlgoritmo algoritmo) {
        synchronized (lock) {
            this.algoritmoActual = algoritmo;
            System.out.println("⚙️ Algoritmo cambiado a: " + algoritmo);
        }
    }

    /**
     * Configura el quantum para Round Robin
     */
    public void setQuantumRoundRobin(int quantum) {
        synchronized (lock) {
            this.quantumRoundRobin = Math.max(1, quantum);

            // Actualizar quantum en todos los cores
            for (Core core : cores) {
                core.setQuantum(this.quantumRoundRobin);
            }

            System.out.println("⏱️ Quantum configurado a: " + quantum + "ms");
        }
    }

    /**
     * Obtiene el estado actual de la CPU
     */
    public String getEstadoActual() {
        synchronized (lock) {
            StringBuilder estado = new StringBuilder();
            estado.append("=== ESTADO CPU ===\n");
            estado.append("Nombre: ").append(nombre).append("\n");
            estado.append("Cores: ").append(numeroCores).append("\n");
            estado.append("Algoritmo: ").append(algoritmoActual).append("\n");
            estado.append("Quantum: ").append(quantumRoundRobin).append("ms\n");
            estado.append("Ejecutando: ").append(ejecutando ? "SÍ" : "NO").append("\n");
            estado.append("Cores libres: ").append(getCoresLibres().size()).append("/").append(numeroCores).append("\n");

            estado.append("\nEstado de cores:\n");
            for (Core core : cores) {
                estado.append("  ").append(core.getEstadoActual()).append("\n");
            }

            return estado.toString();
        }
    }

    /**
     * Obtiene estadísticas detalladas
     */
    public String getEstadisticas() {
        synchronized (lock) {
            StringBuilder stats = new StringBuilder();
            stats.append("=== ESTADÍSTICAS CPU ===\n");
            stats.append("Tiempo total operación: ").append(tiempoTotalOperacion).append("ms\n");
            stats.append("Procesos ejecutados: ").append(procesosTotalesEjecutados).append("\n");

            if (tiempoTotalOperacion > 0) {
                stats.append("Promedio por proceso: ")
                        .append(tiempoTotalOperacion / Math.max(1, procesosTotalesEjecutados))
                        .append("ms\n");
            }

            stats.append("\nEstadísticas por core:\n");
            for (Core core : cores) {
                stats.append("  ").append(core.getEstadisticas()).append("\n");
            }

            // Estadísticas generales
            double usoPromedio = cores.stream()
                    .mapToDouble(Core::getPorcentajeUso)
                    .average()
                    .orElse(0.0);

            stats.append("Uso promedio CPU: ").append(String.format("%.1f%%", usoPromedio)).append("\n");

            return stats.toString();
        }
    }

    /**
     * Imprime el estado actual en consola
     */
    public void imprimirEstado() {
        System.out.println(getEstadoActual());
    }

    /**
     * Imprime las estadísticas en consola
     */
    public void imprimirEstadisticas() {
        System.out.println(getEstadisticas());
    }

    // Getters principales
    public List<Core> getCores() {
        synchronized (lock) {
            return new ArrayList<>(cores);
        }
    }

    public int getNumeroCores() {
        return numeroCores;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isEjecutando() {
        synchronized (lock) {
            return ejecutando;
        }
    }

    public TipoAlgoritmo getAlgoritmoActual() {
        synchronized (lock) {
            return algoritmoActual;
        }
    }

    public int getQuantumRoundRobin() {
        synchronized (lock) {
            return quantumRoundRobin;
        }
    }

    public int getCoresLibresCount() {
        return getCoresLibres().size();
    }

    public int getCoresOcupadosCount() {
        return getCoresOcupados().size();
    }

    public long getTiempoTotalOperacion() {
        synchronized (lock) {
            return tiempoTotalOperacion;
        }
    }

    public int getProcesosTotalesEjecutados() {
        synchronized (lock) {
            return procesosTotalesEjecutados;
        }
    }

    public double getUsoPromedioCpu() {
        synchronized (lock) {
            return cores.stream()
                    .mapToDouble(Core::getPorcentajeUso)
                    .average()
                    .orElse(0.0);
        }
    }

    /**
     * Obtiene un core específico por ID
     */
    public Core getCore(int id) {
        synchronized (lock) {
            return cores.stream()
                    .filter(core -> core.getId() == id)
                    .findFirst()
                    .orElse(null);
        }
    }

    @Override
    public String toString() {
        synchronized (lock) {
            return String.format("%s[%d cores, %d libres, %s]",
                    nombre, numeroCores, getCoresLibresCount(), algoritmoActual);
        }
    }
}