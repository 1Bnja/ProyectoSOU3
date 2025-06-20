package org.example.proyectoso.utils;

import org.example.proyectoso.models.*;
import org.example.proyectoso.memoria.Memoria;

/**
 * Clase para probar el funcionamiento de las clases Estadisticas y SimuladorTiempo
 */
public class PruebaEstadisticasTiempo {

    public static void main(String[] args) {
        System.out.println("=== Iniciando prueba de Estadísticas y SimuladorTiempo ===");

        // Inicializar el simulador de tiempo
        SimuladorTiempo.iniciar();
        System.out.println("Tiempo actual: " + SimuladorTiempo.formatearTiempo(SimuladorTiempo.getTiempoActual()));

        // Crear instancia de estadísticas
        Estadisticas estadisticas = new Estadisticas();
        estadisticas.iniciar();

        try {
            // Crear una memoria para pruebas
            Memoria memoria = new Memoria(1024); // 1GB de memoria

            // Simular llegada de procesos
            System.out.println("\n1. Simulando llegada de procesos...");
            for (int i = 1; i <= 5; i++) {
                estadisticas.registrarNuevoProceso();
                System.out.println("  - Proceso #" + i + " registrado en tiempo " +
                        SimuladorTiempo.formatearTiempo(SimuladorTiempo.getTiempoActual()));
                SimuladorTiempo.esperar(500); // Esperar 500ms simulados entre procesos
            }

            System.out.println("\n2. Acelerando la simulación (2x)...");
            SimuladorTiempo.setFactorVelocidad(2.0);

            // Simular cambios de contexto y utilización de CPU
            System.out.println("\n3. Simulando ejecución de procesos y cambios de contexto...");
            for (int i = 0; i < 3; i++) {
                estadisticas.registrarCambioContexto();
                estadisticas.actualizarUtilizacionCPU(0.75); // 75% de CPU

                // Simular asignación de memoria para procesos
                Proceso proceso = new Proceso("Proceso" + i, 1000, 100 + i*50); // Procesos con diferentes tamaños
                memoria.asignarMemoria(proceso); // Asignar memoria al proceso
                estadisticas.actualizarUtilizacionMemoria(memoria);

                System.out.println("  - Cambio de contexto #" + (i+1) + " en tiempo " +
                        SimuladorTiempo.formatearTiempo(SimuladorTiempo.getTiempoActual()));
                SimuladorTiempo.esperar(800);
            }

            // Simular finalización de procesos
            System.out.println("\n4. Simulando finalización de procesos...");
            // Crear procesos de prueba
            Proceso p1 = new Proceso("Proceso1", 1000, 256, 0);
            SimuladorTiempo.esperar(300);
            p1.iniciarEjecucion(SimuladorTiempo.getTiempoActual());
            SimuladorTiempo.esperar(1000);
            simularFinalizarProceso(p1, SimuladorTiempo.getTiempoActual());
            estadisticas.registrarProcesoTerminado(p1);

            Proceso p2 = new Proceso("Proceso2", 1500, 512, 1000);
            SimuladorTiempo.esperar(500);
            p2.iniciarEjecucion(SimuladorTiempo.getTiempoActual());
            SimuladorTiempo.esperar(1500);
            simularFinalizarProceso(p2, SimuladorTiempo.getTiempoActual());
            estadisticas.registrarProcesoTerminado(p2);

            // Simular page faults y swapping
            System.out.println("\n5. Simulando page faults y swapping...");
            for (int i = 0; i < 3; i++) {
                estadisticas.registrarPageFault();
                System.out.println("  - Page fault #" + (i+1) + " registrado");
                if (i % 2 == 0) {
                    estadisticas.registrarSwapping();
                    System.out.println("  - Operación de swapping registrada");
                }
                SimuladorTiempo.esperar(400);
            }

            // Pausar la simulación
            System.out.println("\n6. Pausando la simulación por 2 segundos reales...");
            SimuladorTiempo.pausar();
            long tiempoPausa = SimuladorTiempo.getTiempoActual();
            System.out.println("  - Tiempo en pausa: " +
                    SimuladorTiempo.formatearTiempo(tiempoPausa));

            // Esperar tiempo real (no simulado)
            Thread.sleep(2000);

            // Verificar que el tiempo no avanzó durante la pausa
            System.out.println("  - Tiempo después de esperar (debería ser igual): " +
                    SimuladorTiempo.formatearTiempo(SimuladorTiempo.getTiempoActual()));

            // Reanudar
            System.out.println("\n7. Reanudando la simulación...");
            SimuladorTiempo.reanudar();
            SimuladorTiempo.esperar(1000);
            System.out.println("  - Tiempo después de reanudar: " +
                    SimuladorTiempo.formatearTiempo(SimuladorTiempo.getTiempoActual()));

            // Finalizar simulación
            System.out.println("\n8. Finalizando la simulación...");
            estadisticas.finalizar();

            // Mostrar resultados
            System.out.println("\n===== RESULTADOS =====");

            // Mostrar estadísticas resumidas
            estadisticas.mostrarResumen();

            // Mostrar informe detallado
            System.out.println("\n===== INFORME DETALLADO =====");
            System.out.println(estadisticas.generarInforme());

        } catch (Exception e) {
            System.err.println("Error durante la prueba: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Método auxiliar para simular la finalización de un proceso
     * ya que la implementación real podría variar
     */
    private static void simularFinalizarProceso(Proceso proceso, long tiempoActual) {
        proceso.pausar(); // Cambiar el estado del proceso

        // Calcular tiempos correctamente
        int tiempoEjecutado = proceso.getDuracion(); // Asumimos que se ejecutó completamente
        int tiempoEspera = (int)(tiempoActual - proceso.getTiempoLlegada() - tiempoEjecutado);
        int tiempoRetorno = (int)(tiempoActual - proceso.getTiempoLlegada());

        // Asegurarnos de que los tiempos no sean negativos
        tiempoEspera = Math.max(0, tiempoEspera);
        tiempoRetorno = Math.max(0, tiempoRetorno);

        // Usando reflection para establecer los valores correctamente
        try {
            // Establecer tiempo de espera
            java.lang.reflect.Field tiempoEsperaField = Proceso.class.getDeclaredField("tiempoEspera");
            tiempoEsperaField.setAccessible(true);
            tiempoEsperaField.set(proceso, tiempoEspera);

            // Establecer tiempo de retorno
            java.lang.reflect.Field tiempoRetornoField = Proceso.class.getDeclaredField("tiempoRetorno");
            tiempoRetornoField.setAccessible(true);
            tiempoRetornoField.set(proceso, tiempoRetorno);

            // Establecer tiempo de finalización
            java.lang.reflect.Field tiempoFinalizacionField = Proceso.class.getDeclaredField("tiempoFinalizacion");
            tiempoFinalizacionField.setAccessible(true);
            tiempoFinalizacionField.set(proceso, tiempoActual);

            // Cambiar el estado a terminado
            java.lang.reflect.Field estadoField = Proceso.class.getDeclaredField("estado");
            estadoField.setAccessible(true);
            estadoField.set(proceso, EstadoProceso.TERMINADO);
        } catch (Exception e) {
            System.out.println("No se pudieron establecer todos los campos del proceso: " + e.getMessage());
        }

        System.out.println("✅ Proceso " + proceso.getId() + " finalizado en tiempo: " +
                SimuladorTiempo.formatearTiempo(tiempoActual) +
                " | Tiempo espera: " + tiempoEspera +
                " | Tiempo retorno: " + tiempoRetorno);
    }
}
