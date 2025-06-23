package org.example.proyectoso.utils;

import org.example.proyectoso.models.*;
import org.example.proyectoso.memoria.Memoria;


public class PruebaEstadisticasTiempo {

    public static void main(String[] args) {
        System.out.println("=== Iniciando prueba de Estadísticas y SimuladorTiempo ===");

        
        SimuladorTiempo.iniciar();
        System.out.println("Tiempo actual: " + SimuladorTiempo.formatearTiempo(SimuladorTiempo.getTiempoActual()));

        
        Estadisticas estadisticas = new Estadisticas();
        estadisticas.iniciar();

        try {
            
            Memoria memoria = new Memoria(1024); 

            
            System.out.println("\n1. Simulando llegada de procesos...");
            for (int i = 1; i <= 5; i++) {
                estadisticas.registrarNuevoProceso();
                System.out.println("  - Proceso #" + i + " registrado en tiempo " +
                        SimuladorTiempo.formatearTiempo(SimuladorTiempo.getTiempoActual()));
                SimuladorTiempo.esperar(500); 
            }

            System.out.println("\n2. Acelerando la simulación (2x)...");
            SimuladorTiempo.setFactorVelocidad(2.0);

            
            System.out.println("\n3. Simulando ejecución de procesos y cambios de contexto...");
            for (int i = 0; i < 3; i++) {
                estadisticas.registrarCambioContexto();
                estadisticas.actualizarUtilizacionCPU(0.75); 

                
                Proceso proceso = new Proceso("Proceso" + i, 1000, 100 + i*50); 
                memoria.asignarMemoria(proceso); 
                estadisticas.actualizarUtilizacionMemoria(memoria);

                System.out.println("  - Cambio de contexto #" + (i+1) + " en tiempo " +
                        SimuladorTiempo.formatearTiempo(SimuladorTiempo.getTiempoActual()));
                SimuladorTiempo.esperar(800);
            }

            
            System.out.println("\n4. Simulando finalización de procesos...");
            
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

            
            System.out.println("\n6. Pausando la simulación por 2 segundos reales...");
            SimuladorTiempo.pausar();
            long tiempoPausa = SimuladorTiempo.getTiempoActual();
            System.out.println("  - Tiempo en pausa: " +
                    SimuladorTiempo.formatearTiempo(tiempoPausa));

            
            Thread.sleep(2000);

            
            System.out.println("  - Tiempo después de esperar (debería ser igual): " +
                    SimuladorTiempo.formatearTiempo(SimuladorTiempo.getTiempoActual()));

            
            System.out.println("\n7. Reanudando la simulación...");
            SimuladorTiempo.reanudar();
            SimuladorTiempo.esperar(1000);
            System.out.println("  - Tiempo después de reanudar: " +
                    SimuladorTiempo.formatearTiempo(SimuladorTiempo.getTiempoActual()));

            
            System.out.println("\n8. Finalizando la simulación...");
            estadisticas.finalizar();

            
            System.out.println("\n===== RESULTADOS =====");

            
            estadisticas.mostrarResumen();

            
            System.out.println("\n===== INFORME DETALLADO =====");
            System.out.println(estadisticas.generarInforme());

        } catch (Exception e) {
            System.err.println("Error durante la prueba: " + e.getMessage());
            e.printStackTrace();
        }
    }

    
    private static void simularFinalizarProceso(Proceso proceso, long tiempoActual) {
        proceso.pausar(); 

        
        int tiempoEjecutado = proceso.getDuracion(); 
        int tiempoEspera = (int)(tiempoActual - proceso.getTiempoLlegada() - tiempoEjecutado);
        int tiempoRetorno = (int)(tiempoActual - proceso.getTiempoLlegada());

        
        tiempoEspera = Math.max(0, tiempoEspera);
        tiempoRetorno = Math.max(0, tiempoRetorno);

        
        try {
            
            java.lang.reflect.Field tiempoEsperaField = Proceso.class.getDeclaredField("tiempoEspera");
            tiempoEsperaField.setAccessible(true);
            tiempoEsperaField.set(proceso, tiempoEspera);

            
            java.lang.reflect.Field tiempoRetornoField = Proceso.class.getDeclaredField("tiempoRetorno");
            tiempoRetornoField.setAccessible(true);
            tiempoRetornoField.set(proceso, tiempoRetorno);

            
            java.lang.reflect.Field tiempoFinalizacionField = Proceso.class.getDeclaredField("tiempoFinalizacion");
            tiempoFinalizacionField.setAccessible(true);
            tiempoFinalizacionField.set(proceso, tiempoActual);

            
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
