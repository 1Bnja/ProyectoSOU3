package org.example.proyectoso;

import org.example.proyectoso.models.*;
import org.example.proyectoso.memoria.*;
import org.example.proyectoso.planificacion.*;
import org.example.proyectoso.utils.*;

import java.util.ArrayList;
import java.util.List;

public class TestMain {
    public static void main(String[] args) {
        System.out.println("=== PRUEBA DEL ALGORITMO SJF CON ESTADÍSTICAS Y SIMULACIÓN DE TIEMPO ===\n");

        try {
            // Inicializar el simulador de tiempo
            SimuladorTiempo.iniciar();
            System.out.println("⏱️ Simulador de tiempo iniciado en: " +
                    SimuladorTiempo.formatearTiempo(SimuladorTiempo.getTiempoActual()));

            // Inicializar estadísticas
            Estadisticas estadisticas = new Estadisticas();
            estadisticas.iniciar();
            System.out.println("📊 Sistema de estadísticas iniciado\n");

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
                estadisticas.registrarNuevoProceso(); // Registrar cada proceso en estadísticas
                System.out.println("Proceso " + p.getId() + ": " +
                        p.getNombre() + " | Duración: " + p.getDuracion() + "ms | " +
                        "Memoria: " + p.getTamanoMemoria() + "MB | Estado: " + p.getEstado());

                // Simular tiempo entre creaciones
                SimuladorTiempo.esperar(100);
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
                    estadisticas.registrarSwapping(); // Registrar operación de swapping
                    System.out.println("💾 Proceso " + proceso.getId() + " movido a swapping");
                }

                // Actualizar estadísticas de memoria
                estadisticas.actualizarUtilizacionMemoria(memoria);

                // Simular tiempo entre asignaciones
                SimuladorTiempo.esperar(200);
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
                System.out.println("Tiempo de inicio: " +
                        SimuladorTiempo.formatearTiempo(SimuladorTiempo.getTiempoActual()));

                // Acelerar la simulación para la ejecución
                SimuladorTiempo.setFactorVelocidad(3.0);
                System.out.println("⏱️ Velocidad de simulación aumentada (3x)");

                // Ejecutar
                sjf.ejecutarProcesos(procesosListos);

                // Esperar a que termine la ejecución
                System.out.println("\n--- ESPERANDO FINALIZACIÓN ---");

                // Simulamos la observación del progreso
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(500); // Tiempo real de espera
                    System.out.println("Progreso en tiempo: " +
                            SimuladorTiempo.formatearTiempo(SimuladorTiempo.getTiempoActual()));

                    // Registrar cambios de contexto y utilización de CPU (simulado)
                    estadisticas.registrarCambioContexto();
                    estadisticas.actualizarUtilizacionCPU(0.7 + (Math.random() * 0.2)); // Entre 70% y 90%

                    // Simular algunos page faults aleatorios
                    if (Math.random() > 0.6) {
                        estadisticas.registrarPageFault();
                        System.out.println("📉 Page fault detectado");
                    }
                }

                // Volver a velocidad normal
                SimuladorTiempo.setFactorVelocidad(1.0);

            } else {
                System.out.println("⚠️ No hay procesos listos para ejecutar (todos en swapping)");
            }

            // 7. Mostrar resultados
            System.out.println("\n--- RESULTADOS DE LA EJECUCIÓN ---");
            System.out.println("Tiempo de finalización: " +
                    SimuladorTiempo.formatearTiempo(SimuladorTiempo.getTiempoActual()));

            // Esperar a que todos los procesos terminen completamente
            boolean todosProcesosTerminados = false;
            int intentos = 0;
            int maxIntentos = 10;

            while (!todosProcesosTerminados && intentos < maxIntentos) {
                todosProcesosTerminados = true;
                for (Proceso p : procesos) {
                    if (!p.haTerminado()) {
                        todosProcesosTerminados = false;
                        System.out.println("⏳ Esperando finalización del proceso " + p.getId() +
                                " (" + String.format("%.1f%%", p.getPorcentajeCompletitud()) + " completado)");
                        Thread.sleep(300);
                        break;
                    }
                }
                intentos++;
            }

            System.out.println("\n✅ Verificación de procesos terminada");

            // Estado final de procesos
            System.out.println("\nEstado final de procesos:");
            for (Proceso p : procesos) {
                System.out.println("Proceso " + p.getId() + ": " + p.getEstado() +
                        " | Ejecutado: " + p.getTiempoEjecutado() + "/" + p.getDuracion() + "ms" +
                        " | Progreso: " + String.format("%.1f%%", p.getPorcentajeCompletitud()));

                // Registrar procesos terminados en estadísticas
                if (p.haTerminado()) {
                    // Corregir los tiempos del proceso antes de registrarlo
                    corregirTiemposProceso(p, SimuladorTiempo.getTiempoActual());
                    estadisticas.registrarProcesoTerminado(p);
                }
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
                        estadisticas.actualizarUtilizacionMemoria(memoria);
                    }
                }
            }
            System.out.println("✅ Memoria liberada de " + procesosLiberados + " procesos");

            // Estado final de memoria
            System.out.println("\n--- ESTADO FINAL ---");
            memoria.imprimirEstado();

            // Finalizar la recolección de estadísticas
            estadisticas.finalizar();

            // Mostrar resumen de estadísticas recopiladas
            System.out.println("\n--- ESTADÍSTICAS DE LA SIMULACIÓN ---");
            estadisticas.mostrarResumen();

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

    // Función para corregir los tiempos de un proceso antes de registrarlo en estadísticas
    private static void corregirTiemposProceso(Proceso proceso, long tiempoActual) {
        try {
            // Obtener campos mediante reflection
            java.lang.reflect.Field tiempoRetornoField = Proceso.class.getDeclaredField("tiempoRetorno");
            java.lang.reflect.Field tiempoEsperaField = Proceso.class.getDeclaredField("tiempoEspera");
            java.lang.reflect.Field tiempoRespuestaField = Proceso.class.getDeclaredField("tiempoRespuesta");

            // Hacer accesibles los campos
            tiempoRetornoField.setAccessible(true);
            tiempoEsperaField.setAccessible(true);
            tiempoRespuestaField.setAccessible(true);

            // Calcular tiempos correctamente
            int tiempoRetorno = (int) (tiempoActual - proceso.getTiempoLlegada());
            int tiempoEspera = Math.max(0, tiempoRetorno - proceso.getTiempoEjecutado());

            // Corregir el tiempo de respuesta (en SJF no preemptivo es igual al tiempo de espera)
            int tiempoRespuesta = tiempoEspera;

            // Asegurar valores válidos
            tiempoRetorno = Math.max(0, tiempoRetorno);

            // Establecer valores corregidos
            tiempoRetornoField.set(proceso, tiempoRetorno);
            tiempoEsperaField.set(proceso, tiempoEspera);
            tiempoRespuestaField.set(proceso, tiempoRespuesta);

            System.out.println("🔄 Corrigiendo tiempos para Proceso " + proceso.getId() +
                    " | Espera: " + tiempoEspera +
                    " | Retorno: " + tiempoRetorno +
                    " | Respuesta: " + tiempoRespuesta);

        } catch (Exception e) {
            System.err.println("❌ Error al corregir tiempos del proceso: " + e.getMessage());
        }
    }
}