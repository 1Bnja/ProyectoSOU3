package org.example.proyectoso;

import org.example.proyectoso.models.*;
import org.example.proyectoso.memoria.*;
import org.example.proyectoso.planificacion.*;
import org.example.proyectoso.utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TestMain {

    private static long STEP_DELAY_MS = 200;

    public static void main(String[] args) {
        System.out.println("=== PRUEBA DE ALGORITMOS DE PLANIFICACIÓN CON ESTADÍSTICAS Y SIMULACIÓN ===\n");

        
        if (args.length > 0) {
            try {
                long v = Long.parseLong(args[0]);
                if (v > 0) {
                    STEP_DELAY_MS = v;
                }
            } catch (NumberFormatException ignored) { }
        }

        Scanner scanner = new Scanner(System.in);
        String algoritmo;
        int rrQuantum = 100;

        
        do {
            System.out.print("Seleccione algoritmo ('sjf' o 'rr'): ");
            algoritmo = scanner.nextLine().trim().toLowerCase();
        } while (!algoritmo.equals("sjf") && !algoritmo.equals("rr"));

        
        if ("rr".equals(algoritmo)) {
            System.out.print("Ingrese quantum en ms (por defecto 100): ");
            String qStr = scanner.nextLine().trim();
            try {
                int q = Integer.parseInt(qStr);
                if (q > 0) rrQuantum = q;
            } catch (NumberFormatException ignored) { }
        }

        try {
            
            SimuladorTiempo.iniciar();
            System.out.println("⏱️ Simulador de tiempo iniciado: " +
                    SimuladorTiempo.formatearTiempo(SimuladorTiempo.getTiempoActual()));

            Estadisticas estadisticas = new Estadisticas();
            estadisticas.iniciar();
            System.out.println("📊 Estadísticas iniciadas\n");

            
            CPU cpu = new CPU(2);
            System.out.println("✅ CPU creada: " + cpu.getNombre());

            Memoria memoria = new Memoria(1024);
            System.out.println("✅ Memoria creada: " + memoria.getTamañoTotal() + "MB\n");

            
            List<Proceso> procesos = crearProcesosDePrueba();
            List<Proceso> procesosListos = new ArrayList<>();

            System.out.println("--- CREANDO Y ASIGNANDO MEMORIA A PROCESOS ---");
            for (Proceso p : procesos) {
                estadisticas.registrarNuevoProceso();
                System.out.println("Proceso " + p.getId() + ": " + p.getNombre() +
                        " | Duración: " + p.getDuracion() + "ms | Memoria: " + p.getTamanoMemoria() + "MB");
                SimuladorTiempo.esperar(100);

                if (memoria.asignarMemoria(p)) {
                    p.setEstado(EstadoProceso.LISTO);
                    procesosListos.add(p);
                    System.out.println("✅ Memoria asignada a Proceso " + p.getId());
                } else {
                    memoria.moverASwapping(p);
                    estadisticas.registrarSwapping();
                    System.out.println("💾 Proceso " + p.getId() + " movido a swapping");
                }
                estadisticas.actualizarUtilizacionMemoria(memoria);
                SimuladorTiempo.esperar(STEP_DELAY_MS);
            }
            memoria.imprimirEstado();

            
            Planificacion plan;
            if ("rr".equals(algoritmo)) {
                System.out.println("\n--- CONFIGURANDO ROUND ROBIN (quantum=" + rrQuantum + "ms) ---");
                RoundRobin rr = new RoundRobin(rrQuantum);
                rr.setCpu(cpu);
                plan = rr;
            } else {
                System.out.println("\n--- CONFIGURANDO SJF NO PREEMPTIVO ---");
                SJF sjf = new SJF(false);
                sjf.setCpu(cpu);
                plan = sjf;
            }

            System.out.println("✅ Planificador seleccionado: " + plan.getNombreAlgoritmo());

            
            System.out.println("\n--- ORDEN ESPERADO DE EJECUCIÓN ---");
            String finalAlgoritmo = algoritmo;
            procesosListos.sort((p1, p2) -> {
                if ("rr".equals(finalAlgoritmo)) {
                    return Integer.compare(p1.getTiempoLlegada(), p2.getTiempoLlegada());
                } else {
                    return Integer.compare(p1.getDuracion(), p2.getDuracion());
                }
            });
            for (int i = 0; i < procesosListos.size(); i++) {
                Proceso p = procesosListos.get(i);
                System.out.println((i+1) + ". Proceso " + p.getId() + " (" + p.getNombre() + ") - " +
                        ("rr".equals(algoritmo) ?
                                "Llegada:" + p.getTiempoLlegada() :
                                "Duración:" + p.getDuracion()) + "ms");
            }

            
            if (!procesosListos.isEmpty()) {
                System.out.println("\n--- INICIANDO EJECUCIÓN: " + plan.getNombreAlgoritmo() + " ---");
                SimuladorTiempo.setFactorVelocidad(3.0);
                plan.ejecutarProcesos(procesosListos);

                
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(500);
                    System.out.println("Progreso tiempo: " +
                            SimuladorTiempo.formatearTiempo(SimuladorTiempo.getTiempoActual()));
                    estadisticas.registrarCambioContexto();
                    estadisticas.actualizarUtilizacionCPU(0.7 + Math.random()*0.2);
                    if (Math.random() > 0.6) {
                        estadisticas.registrarPageFault();
                        System.out.println("📉 Page fault detectado");
                    }
                }
                SimuladorTiempo.setFactorVelocidad(1.0);
            } else {
                System.out.println("⚠️ No hay procesos listos para ejecutar");
            }

            
            System.out.println("\n--- RESULTADOS DE EJECUCIÓN ---");
            System.out.println(plan.getEstadisticas());
            System.out.println(cpu.getEstadoActual());

            System.out.println("\n--- LIBERANDO MEMORIA ---");
            int liberados = 0;
            for (Proceso p : procesos) {
                if (p.haTerminado() && memoria.liberarMemoria(p)) liberados++;
            }
            System.out.println("✅ Memoria liberada de " + liberados + " procesos");
            memoria.imprimirEstado();

            estadisticas.finalizar();
            System.out.println("\n--- ESTADÍSTICAS SIMULACIÓN ---");
            estadisticas.mostrarResumen();

            plan.detener();
            cpu.detener();
        } catch (InterruptedException e) {
            System.err.println("❌ Ejecución interrumpida: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n=== FIN PRUEBA ===");
    }

    private static List<Proceso> crearProcesosDePrueba() {
        List<Proceso> procesos = new ArrayList<>();
        procesos.add(new Proceso("Proceso Largo", 1000, 200, 0));
        procesos.add(new Proceso("Proceso Corto", 200, 100, 50));
        procesos.add(new Proceso("Proceso Mediano", 600, 150, 100));
        procesos.add(new Proceso("Proceso Muy Largo", 1500, 300, 150));
        procesos.add(new Proceso("Proceso Rápido", 300, 80, 200));
        procesos.add(new Proceso("Proceso Medio-Corto", 400, 120, 250));
        return procesos;
    }
}
