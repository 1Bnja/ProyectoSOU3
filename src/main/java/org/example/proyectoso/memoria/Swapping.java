package org.example.proyectoso.memoria;
import org.example.proyectoso.models.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Swapping {
    private final Queue<Proceso> colaSwapping;     // Cola de procesos esperando memoria
    private final Object lock = new Object();      // Para sincronización
    private int procesosSwappeados = 0;            // Contador de procesos en swap

    /**
     * Constructor que inicializa la cola de swapping
     */
    public Swapping() {
        this.colaSwapping = new ConcurrentLinkedQueue<>();
    }

    /**
     * Mueve un proceso al área de swapping
     * @param proceso El proceso a mover a swap
     */
    public void moverASwap(Proceso proceso) {
        synchronized (lock) {
            colaSwapping.offer(proceso);
            proceso.setEstado(EstadoProceso.ESPERANDO);
            procesosSwappeados++;

            System.out.println("💾 Proceso " + proceso.getId() +
                    " movido a swapping (Total en swap: " + procesosSwappeados + ")");
        }
    }

    /**
     * Intenta sacar procesos del swapping y asignarles memoria
     * @param memoria La instancia de memoria para intentar asignación
     * @return Lista de procesos que pudieron salir del swapping
     */
    public List<Proceso> procesarCola(Memoria memoria) {
        List<Proceso> procesosLiberados = new ArrayList<>();

        synchronized (lock) {
            Iterator<Proceso> iterator = colaSwapping.iterator();

            while (iterator.hasNext()) {
                Proceso proceso = iterator.next();

                // Intentar asignar memoria al proceso
                if (memoria.asignarMemoria(proceso)) {
                    proceso.setEstado(EstadoProceso.LISTO);
                    iterator.remove();
                    procesosSwappeados--;
                    procesosLiberados.add(proceso);

                    System.out.println("🔄 Proceso " + proceso.getId() +
                            " salió de swapping y está LISTO");
                }
            }
        }

        return procesosLiberados;
    }

    /**
     * Remueve un proceso específico del swapping (si existe)
     * @param procesoId ID del proceso a remover
     * @return true si se removió, false si no estaba
     */
    public boolean removerProceso(int procesoId) {
        synchronized (lock) {
            Iterator<Proceso> iterator = colaSwapping.iterator();

            while (iterator.hasNext()) {
                Proceso proceso = iterator.next();
                if (proceso.getId() == procesoId) {
                    iterator.remove();
                    procesosSwappeados--;
                    System.out.println("❌ Proceso " + procesoId + " removido del swapping");
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Obtiene el siguiente proceso en la cola sin removerlo
     * @return El próximo proceso a procesar, o null si la cola está vacía
     */
    public Proceso verSiguiente() {
        return colaSwapping.peek();
    }

    /**
     * Verifica si un proceso específico está en swapping
     * @param procesoId ID del proceso a buscar
     * @return true si está en swapping
     */
    public boolean estaEnSwapping(int procesoId) {
        synchronized (lock) {
            return colaSwapping.stream()
                    .anyMatch(p -> p.getId() == procesoId);
        }
    }


    public List<Proceso> getProcesosEnSwapping() {
        synchronized (lock) {
            return new ArrayList<>(colaSwapping);
        }
    }


    public int getMemoriaRequerida() {
        synchronized (lock) {
            return colaSwapping.stream()
                    .mapToInt(Proceso::getTamanoMemoria)
                    .sum();
        }
    }


    public Proceso getProcesoMenorMemoria() {
        synchronized (lock) {
            return colaSwapping.stream()
                    .min(Comparator.comparingInt(Proceso::getTamanoMemoria))
                    .orElse(null);
        }
    }


    public void limpiar() {
        synchronized (lock) {
            colaSwapping.clear();
            procesosSwappeados = 0;
            System.out.println("🧹 Swapping limpiado");
        }
    }


    public String getEstadisticas() {
        synchronized (lock) {
            return String.format("Procesos: %d, Memoria requerida: %dMB",
                    procesosSwappeados, getMemoriaRequerida());
        }
    }


    public void imprimirEstado() {
        synchronized (lock) {
            System.out.println("\n=== ESTADO SWAPPING ===");
            System.out.println("Procesos en swapping: " + procesosSwappeados);
            System.out.println("Memoria total requerida: " + getMemoriaRequerida() + "MB");

            if (!colaSwapping.isEmpty()) {
                System.out.println("Cola de procesos:");
                int posicion = 1;
                for (Proceso proceso : colaSwapping) {
                    System.out.printf("%d. Proceso %d (%dMB)%n",
                            posicion++, proceso.getId(), proceso.getTamanoMemoria());
                }
            } else {
                System.out.println("Cola vacía");
            }
            System.out.println("=======================\n");
        }
    }

    // Getters básicos
    public int getCantidadProcesos() {
        return procesosSwappeados;
    }

    public boolean estaVacio() {
        return colaSwapping.isEmpty();
    }

    public int getTamañoCola() {
        return colaSwapping.size();
    }
}