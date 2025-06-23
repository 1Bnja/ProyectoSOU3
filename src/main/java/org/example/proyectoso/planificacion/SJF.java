package org.example.proyectoso.planificacion;

import org.example.proyectoso.models.*;
import java.util.*;
import java.util.stream.Collectors;


public class SJF extends Planificacion {

    
    private boolean preemptivo;
    private final Object sjfLock = new Object();

    
    private volatile boolean pausado;
    private Thread hiloEjecucion;

    
    private int cambiosContexto;
    private long tiempoPromedioEspera;
    private long tiempoPromedioRespuesta;

    
    public SJF() {
        this(false);
    }

    
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

    
    private void ejecutarSJFNoPreemptivo(List<Proceso> procesos) {
        synchronized (sjfLock) {
            System.out.println("🚀 Iniciando SJF No Preemptivo con " + procesos.size() + " procesos");

            
            List<Proceso> procesosOrdenados = ordenarProcesos(procesos);

            for (Proceso proceso : procesosOrdenados) {
                if (!ejecutando || pausado) {
                    break;
                }

                ejecutarProcesoCompleto(proceso);
                procesosEjecutados++;

                
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

    
    private void ejecutarSJFPreemptivo(List<Proceso> procesos) {
        synchronized (sjfLock) {
            System.out.println("🚀 Iniciando SJF Preemptivo (SRTF) con " + procesos.size() + " procesos");

            Queue<Proceso> colaListos = new PriorityQueue<>(
                    Comparator.comparingInt(Proceso::getTiempoRestante)
            );

            
            colaListos.addAll(procesos);

            while (!colaListos.isEmpty() && ejecutando && !pausado) {
                
                Proceso procesoActual = colaListos.poll();

                if (procesoActual == null || procesoActual.haTerminado()) {
                    continue;
                }

                
                int tiempoEjecucion = Math.min(100, procesoActual.getTiempoRestante());

                ejecutarProcesoParcial(procesoActual, tiempoEjecucion);

                
                if (!procesoActual.haTerminado()) {
                    colaListos.offer(procesoActual);
                    cambiosContexto++;
                } else {
                    procesosEjecutados++;
                }

                
                reordenarCola(colaListos);

                
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

    
    private void ejecutarProcesoCompleto(Proceso proceso) {
        if (proceso == null || proceso.haTerminado()) {
            return;
        }

        
        Core coreLibre = esperarCoreLibre();
        if (coreLibre == null) {
            return;
        }

        try {
            System.out.println("▶️ Ejecutando proceso " + proceso.getId() +
                    " (Ráfaga: " + proceso.getDuracion() + "ms)");

            proceso.setEstado(EstadoProceso.EJECUTANDO);
            coreLibre.asignarProceso(proceso);

            
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