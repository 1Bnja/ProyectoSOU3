package org.example.proyectoso;
import org.example.proyectoso.models.*;
import org.example.proyectoso.memoria.*;
import org.example.proyectoso.planificacion.*;

import java.util.ArrayList;
import java.util.List;

public class TestMain {
    public static void main(String[] args) {
        System.out.println("=== PRUEBA DEL ALGORITMO SJF ===");

        // Crear algunos procesos de prueba
        Proceso proceso1 = new Proceso("nombre", 10, 200, 10);
        proceso1.setId("P1");
        proceso1.setTiempoLlegada(0);
        proceso1.setDuracion(6);
        proceso1.setTiempoEspera(0);
        proceso1.setTiempoRespuesta(0);
        proceso1.setEstado(EstadoProceso.NUEVO);

        Proceso proceso2 = new Proceso();
        proceso2.setId("P2");
        proceso2.setTiempoLlegada(2);
        proceso2.setDuracion(8);
        proceso2.setTiempoEspera(0);
        proceso2.setTiempoRespuesta(0);
        proceso2.setEstado(EstadoProceso.NUEVO);

        Proceso proceso3 = new Proceso();
        proceso3.setId("P3");
        proceso3.setTiempoLlegada(1);
        proceso3.setDuracion(7);
        proceso3.setTiempoEspera(0);
        proceso3.setTiempoRespuesta(0);
        proceso3.setEstado(EstadoProceso.NUEVO);

        Proceso proceso4 = new Proceso();
        proceso4.setId("P4");
        proceso4.setTiempoLlegada(3);
        proceso4.setDuracion(3);
        proceso4.setTiempoEspera(0);
        proceso4.setTiempoRespuesta(0);
        proceso4.setEstado(EstadoProceso.NUEVO);

        // Crear lista de procesos
        List<Proceso> procesos = new ArrayList<>();
        procesos.add(proceso1);
        procesos.add(proceso2);
        procesos.add(proceso3);
        procesos.add(proceso4);

        // Mostrar procesos antes de la planificación
        System.out.println("\n--- PROCESOS INICIALES ---");
        for (Proceso p : procesos) {
            System.out.println("Proceso: " + p.getId() +
                    " | Llegada: " + p.getTiempoLlegada() +
                    " | Duración: " + p.getDuracion() +
                    " | Estado: " + p.getEstado());
        }

        // Crear CPU con un core
        Core core = new Core();
        List<Core> cores = new ArrayList<>();
        cores.add(core);

        Cpu cpu = new Cpu();
        cpu.setCores(cores);

        // Crear memoria
        Memoria memoria = new Memoria();

        // Crear manejador de procesos
        ManejoProcesos manejoProcesos = new ManejoProcesos();
        manejoProcesos.setProcesos(procesos);
        manejoProcesos.setPlanificador(new Planificacion()); // Asumiendo que existe un constructor vacío

        // Crear controlador
        Controlador controlador = new Controlador();
        controlador.setCpu(cpu);
        controlador.setMemoria(memoria);
        controlador.setManejoProcesos(manejoProcesos);

        // Crear algoritmo SJF
        SJF sjf = new SJF();
        sjf.setColaProcesos(procesos);

        System.out.println("\n--- INICIANDO ALGORITMO SJF ---");

        try {
            // Ejecutar SJF
            sjf.planificar();

            System.out.println("\n--- PLANIFICACIÓN COMPLETADA ---");
            System.out.println("Procesos después de SJF:");

            for (Proceso p : procesos) {
                System.out.println("Proceso: " + p.getId() +
                        " | Estado: " + p.getEstado() +
                        " | Tiempo Espera: " + p.getTiempoEspera() +
                        " | Tiempo Respuesta: " + p.getTiempoRespuesta());
            }

        } catch (Exception e) {
            System.err.println("Error durante la ejecución: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n=== FIN DE LA PRUEBA ===");
    }
}