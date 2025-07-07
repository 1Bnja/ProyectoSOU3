package org.example.proyectoso.memoria;
import org.example.proyectoso.models.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Swapping {
    private final Queue<Proceso> colaSwapping;
    private final Object lock = new Object();
    private int procesosSwappeados = 0;

    public Swapping() {
        this.colaSwapping = new ConcurrentLinkedQueue<>();
    }

    /**
     * Mueve un proceso al área de swapping
     */
    public void moverASwap(Proceso proceso) {
        synchronized (lock) {
            colaSwapping.offer(proceso);  // FIFO: agregar al final
            proceso.setEstado(EstadoProceso.ESPERANDO);
            procesosSwappeados++;

            System.out.println("💾 Proceso " + proceso.getId() +
                    " movido a swapping (Total en swap: " + procesosSwappeados + ")");
        }
    }

    /**
     * FIFO: Intenta sacar UN SOLO proceso del swapping
     * El PRIMERO que entró es el PRIMERO que sale
     */
    public List<Proceso> procesarCola(Memoria memoria) {
        List<Proceso> procesosLiberados = new ArrayList<>();

        synchronized (lock) {
            // FIFO simple: revisar solo el PRIMERO en la cola
            Proceso primero = colaSwapping.peek();

            if (primero != null) {
                System.out.println("🔍 Intentando sacar Proceso " + primero.getId() +
                        " del swap (primero en cola)");

                // Intentar asignar memoria al primer proceso
                if (memoria.asignarMemoria(primero)) {
                    // ✅ Consiguió memoria - sacarlo de la cola
                    colaSwapping.poll();  // FIFO: quitar el primero
                    procesosSwappeados--;
                    primero.setEstado(EstadoProceso.LISTO);
                    procesosLiberados.add(primero);

                    System.out.println("✅ Proceso " + primero.getId() +
                            " salió de swapping → LISTO (quedan " + procesosSwappeados + " en swap)");
                } else {
                    System.out.println("❌ Proceso " + primero.getId() +
                            " no pudo salir del swap (sin memoria disponible)");
                }
            } else {
                System.out.println("ℹ️ Cola de swapping vacía");
            }
        }

        return procesosLiberados;
    }

    /**
     * Obtiene una copia de los procesos en swapping (para visualización)
     */
    public List<Proceso> getProcesosEnSwapping() {
        synchronized (lock) {
            return new ArrayList<>(colaSwapping);
        }
    }

    /**
     * Calcula la memoria total requerida por procesos en swap
     */
    public int getMemoriaRequerida() {
        synchronized (lock) {
            return colaSwapping.stream()
                    .mapToInt(Proceso::getTamanoMemoria)
                    .sum();
        }
    }

    /**
     * Limpia completamente el swapping
     */
    public void limpiar() {
        synchronized (lock) {
            colaSwapping.clear();
            procesosSwappeados = 0;
            System.out.println("🧹 Swapping limpiado (FIFO)");
        }
    }

    /**
     * Imprime el estado actual del swapping
     */
    public void imprimirEstado() {
        synchronized (lock) {
            System.out.println("\n=== ESTADO SWAPPING (FIFO) ===");
            System.out.println("Procesos en swapping: " + procesosSwappeados);
            System.out.println("Memoria total requerida: " + getMemoriaRequerida() + "MB");

            if (!colaSwapping.isEmpty()) {
                System.out.println("Cola FIFO (primero → último):");
                int posicion = 1;
                for (Proceso proceso : colaSwapping) {
                    String indicador = (posicion == 1) ? " ← PRÓXIMO A SALIR" : "";
                    System.out.printf("%d. Proceso %d (%dMB)%s%n",
                            posicion++, proceso.getId(), proceso.getTamanoMemoria(), indicador);
                }
            } else {
                System.out.println("Cola FIFO vacía");
            }
            System.out.println("===============================\n");
        }
    }

    /**
     * Obtiene la cantidad de procesos en swapping
     */
    public int getCantidadProcesos() {
        return procesosSwappeados;
    }

    /**
     * Verifica si hay procesos en swapping
     */
    public boolean tieneProcesosPendientes() {
        return procesosSwappeados > 0;
    }

    /**
     * Obtiene el próximo proceso que saldría del swap (sin sacarlo)
     */
    public Proceso verProximoASalir() {
        synchronized (lock) {
            return colaSwapping.peek();
        }
    }
}