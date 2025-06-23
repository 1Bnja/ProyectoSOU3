package org.example.proyectoso.models;

import java.util.*;
import java.util.concurrent.*;


public class CPU {
    
    private final List<Core> cores;
    private final int numeroCores;
    private final String nombre;

    
    private final Object lock = new Object();
    private volatile boolean ejecutando = false;
    private ExecutorService executorService;

    
    private int quantumRoundRobin = 100; 
    private TipoAlgoritmo algoritmoActual = TipoAlgoritmo.ROUND_ROBIN;

    
    private long tiempoInicioOperacion;
    private long tiempoTotalOperacion;
    private int procesosTotalesEjecutados;

    
    public enum TipoAlgoritmo {
        ROUND_ROBIN,
        FCFS,           
        SJF,            
        PRIORITY        
    }

    
    public CPU(int numeroCores) {
        this.numeroCores = Math.max(1, numeroCores);
        this.nombre = "CPU-" + this.numeroCores + "Core";
        this.cores = new ArrayList<>();

        
        for (int i = 0; i < this.numeroCores; i++) {
            cores.add(new Core(i));
        }

        
        this.executorService = Executors.newFixedThreadPool(this.numeroCores);

        System.out.println("🔧 " + nombre + " inicializada con " + numeroCores + " cores");
    }

    
    public CPU(int numeroCores, String nombre) {
        this(numeroCores);
        
        
    }

    
    public boolean ejecutarProceso(Proceso proceso) {
        synchronized (lock) {
            Core coreLibre = obtenerCoreLibre();

            if (coreLibre != null) {
                return ejecutarEnCore(proceso, coreLibre);
            }

            return false; 
        }
    }

    
    public boolean ejecutarEnCore(Proceso proceso, Core core) {
        if (proceso == null || core == null) {
            return false;
        }

        
        core.setQuantum(quantumRoundRobin);

        
        if (core.asignarProceso(proceso)) {
            
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

    
    private void ejecutarRoundRobin(List<Proceso> procesos) {
        Queue<Proceso> colaProcesos = new LinkedList<>(procesos);

        while (!colaProcesos.isEmpty()) {
            
            List<Future<Boolean>> futures = new ArrayList<>();

            for (Core core : cores) {
                if (core.isLibre() && !colaProcesos.isEmpty()) {
                    Proceso proceso = colaProcesos.poll();

                    Future<Boolean> future = executorService.submit(() -> {
                        core.asignarProceso(proceso);
                        boolean terminado = core.ejecutarQuantum();

                        
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

            
            for (Future<Boolean> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("Error en ejecución: " + e.getMessage());
                }
            }

            
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    
    private void ejecutarFCFS(List<Proceso> procesos) {
        
        List<Proceso> procesosOrdenados = new ArrayList<>(procesos);
        procesosOrdenados.sort(Comparator.comparingInt(Proceso::getTiempoLlegada));

        for (Proceso proceso : procesosOrdenados) {
            
            Core coreLibre = esperarCoreLibre();
            ejecutarEnCore(proceso, coreLibre);

            
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

    
    private void ejecutarSJF(List<Proceso> procesos) {
        
        List<Proceso> procesosOrdenados = new ArrayList<>(procesos);
        procesosOrdenados.sort(Comparator.comparingInt(Proceso::getDuracion));

        ejecutarFCFS(procesosOrdenados); 
    }

    
    private void ejecutarPorPrioridad(List<Proceso> procesos) {
        
        List<Proceso> procesosOrdenados = new ArrayList<>(procesos);
        procesosOrdenados.sort(Comparator.comparingInt(Proceso::getTamanoMemoria));

        ejecutarFCFS(procesosOrdenados);
    }

    
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
                return cores.get(0); 
            }
        }
    }

    
    private Core obtenerCoreLibre() {
        synchronized (lock) {
            return cores.stream()
                    .filter(Core::isLibre)
                    .findFirst()
                    .orElse(null);
        }
    }

    
    public List<Core> getCoresLibres() {
        synchronized (lock) {
            return cores.stream()
                    .filter(Core::isLibre)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
    }

    
    public List<Core> getCoresOcupados() {
        synchronized (lock) {
            return cores.stream()
                    .filter(Core::isOcupado)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
    }

    
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

    
    public void detener() {
        synchronized (lock) {
            ejecutando = false;

            
            interrumpirTodos();

            
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

    
    public void reiniciar() {
        synchronized (lock) {
            detener();

            
            executorService = Executors.newFixedThreadPool(numeroCores);

            
            for (Core core : cores) {
                core.forzarLiberacion();
                core.reiniciarEstadisticas();
            }

            
            tiempoTotalOperacion = 0;
            procesosTotalesEjecutados = 0;

            System.out.println("🔄 " + nombre + " reiniciada");
        }
    }

    
    public void setAlgoritmo(TipoAlgoritmo algoritmo) {
        synchronized (lock) {
            this.algoritmoActual = algoritmo;
            System.out.println("⚙️ Algoritmo cambiado a: " + algoritmo);
        }
    }

    
    public void setQuantumRoundRobin(int quantum) {
        synchronized (lock) {
            this.quantumRoundRobin = Math.max(1, quantum);

            
            for (Core core : cores) {
                core.setQuantum(this.quantumRoundRobin);
            }

            System.out.println("⏱️ Quantum configurado a: " + quantum + "ms");
        }
    }

    
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

            
            double usoPromedio = cores.stream()
                    .mapToDouble(Core::getPorcentajeUso)
                    .average()
                    .orElse(0.0);

            stats.append("Uso promedio CPU: ").append(String.format("%.1f%%", usoPromedio)).append("\n");

            return stats.toString();
        }
    }

    
    public void imprimirEstado() {
        System.out.println(getEstadoActual());
    }

    
    public void imprimirEstadisticas() {
        System.out.println(getEstadisticas());
    }

    
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