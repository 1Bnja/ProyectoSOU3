package org.example.proyectoso;

import org.example.proyectoso.models.*;
import org.example.proyectoso.memoria.*;
import org.example.proyectoso.planificacion.*;

import java.util.ArrayList;
import java.util.List;

public class TestMain {
    public static void main(String[] args) {
        System.out.println("=== PRUEBA DEL ALGORITMO SJF ===\n");

        try {
            // 1. Crear la CPU con 2 cores
            CPU cpu = new CPU(2);
            System.out.println("✅ CPU creada: " + cpu.getNombre());

            // 2. Crear memoria del sistema (1GB para la prueba)
            Memoria memoria = new Memoria(1024); // 1024 MB = 1GB
            System.out.println("✅ Memoria creada: " + memoria.getTamañoTotal() + "MB\n");

            // 3. Crear procesos de prueba con diferentes duraciones
            List<Proceso> procesos = crearProcesosDePrueba();

            System.out.println("--- PROCESOS CREADOS ---");
            for (Proceso p : procesos) {
                System.out.println("Proceso " + p.getId() + ": " +
                        p.getNombre() + " | Duración: " + p.getDuracion() + "ms | " +
                        "Memoria: " + p.getTamanoMemoria() + "MB | Estado: " + p.getEstado());
            }

            // 4. Asignar memoria a los procesos
            System.out.println("\n--- ASIGNANDO MEMORIA ---");
            List<Proceso> procesosListos = new ArrayList<>();

            for (Proceso proceso : procesos) {
                boolean memoriaAsignada = memoria.asignarMemoria(proceso);
                if (memoriaAsignada) {
                    proceso.setEstado(EstadoProceso.LISTO);
                    procesosListos.add(proceso);
                    System.out.println("✅ Memoria asignada a Proceso " + proceso.getId());
                } else {
                    memoria.moverASwapping(proceso);
                    System.out.println("💾 Proceso " + proceso.getId() + " movido a swapping");
                }
            }

            // Mostrar estado inicial de memoria
            memoria.imprimirEstado();

            // 5. Crear y configurar el planificador SJF
            System.out.println("\n--- CONFIGURANDO SJF ---");
            SJF sjf = new SJF(false); // SJF no preemptivo
            sjf.setCpu(cpu);
            System.out.println("✅ Planificador SJF configurado: " + sjf.getNombreAlgoritmo());

            // Mostrar orden esperado de ejecución (ordenado por duración)
            System.out.println("\n--- ORDEN ESPERADO DE EJECUCIÓN (SJF) ---");
            List<Proceso> procesosOrdenados = new ArrayList<>(procesosListos);
            procesosOrdenados.sort((p1, p2) -> Integer.compare(p1.getDuracion(), p2.getDuracion()));

            for (int i = 0; i < procesosOrdenados.size(); i++) {
                Proceso p = procesosOrdenados.get(i);
                System.out.println((i + 1) + ". Proceso " + p.getId() +
                        " (" + p.getNombre() + ") - Duración: " + p.getDuracion() + "ms");
            }

            // 6. Ejecutar procesos con SJF
            if (!procesosListos.isEmpty()) {
                System.out.println("\n--- INICIANDO EJECUCIÓN SJF ---");
                System.out.println("Procesos a ejecutar: " + procesosListos.size());

                // Ejecutar
                sjf.ejecutarProcesos(procesosListos);

                // Esperar a que termine la ejecución
                System.out.println("\n--- ESPERANDO FINALIZACIÓN ---");
                Thread.sleep(3000); // Dar tiempo suficiente para que terminen

            } else {
                System.out.println("⚠️ No hay procesos listos para ejecutar (todos en swapping)");
            }

            // 7. Mostrar resultados
            System.out.println("\n--- RESULTADOS DE LA EJECUCIÓN ---");

            // Estado final de procesos
            System.out.println("\nEstado final de procesos:");
            for (Proceso p : procesos) {
                System.out.println("Proceso " + p.getId() + ": " + p.getEstado() +
                        " | Ejecutado: " + p.getTiempoEjecutado() + "/" + p.getDuracion() + "ms" +
                        " | Progreso: " + String.format("%.1f%%", p.getPorcentajeCompletitud()));
            }

            // Estadísticas del planificador
            System.out.println("\n" + sjf.getEstadisticas());

            // Estado de la CPU
            System.out.println(cpu.getEstadoActual());

            // 8. Liberar memoria de procesos terminados
            System.out.println("\n--- LIBERANDO MEMORIA ---");
            int procesosLiberados = 0;
            for (Proceso proceso : procesos) {
                if (proceso.haTerminado()) {
                    boolean liberado = memoria.liberarMemoria(proceso);
                    if (liberado) {
                        procesosLiberados++;
                    }
                }
            }
            System.out.println("✅ Memoria liberada de " + procesosLiberados + " procesos");

            // Estado final de memoria
            System.out.println("\n--- ESTADO FINAL ---");
            memoria.imprimirEstado();

            // Detener el planificador
            sjf.detener();
            cpu.detener();

        } catch (InterruptedException e) {
            System.err.println("❌ Ejecución interrumpida: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ Error durante la ejecución: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n=== FIN DE LA PRUEBA ===");
    }

    /**
     * Crea una lista de procesos de prueba con diferentes duraciones
     * para demostrar el funcionamiento de SJF
     */
    private static List<Proceso> crearProcesosDePrueba() {
        List<Proceso> procesos = new ArrayList<>();

        // Proceso largo (debería ejecutarse último)
        procesos.add(new Proceso("Proceso Largo", 1000, 200, 0));

        // Proceso muy corto (debería ejecutarse primero)
        procesos.add(new Proceso("Proceso Corto", 200, 100, 50));

        // Proceso mediano
        procesos.add(new Proceso("Proceso Mediano", 600, 150, 100));

        // Proceso muy largo (debería ejecutarse último)
        procesos.add(new Proceso("Proceso Muy Largo", 1500, 300, 150));

        // Proceso rápido (debería ejecutarse segundo)
        procesos.add(new Proceso("Proceso Rápido", 300, 80, 200));

        // Proceso medio-corto
        procesos.add(new Proceso("Proceso Medio-Corto", 400, 120, 250));

        return procesos;
    }
}