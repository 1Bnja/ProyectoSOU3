package org.example.proyectoso.models;
import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CPU {
    private List<Core> cores;
    private ExecutorService executor;

    public CPU(int numeroCores) {
        this.cores = new ArrayList<>();
        for(int i = 0; i < numeroCores; i++) {
            cores.add(new Core(i));
        }
        this.executor = Executors.newFixedThreadPool(numeroCores);
    }

    public void ejecutarProceso(Proceso proceso, int quantum) {
        Core coreDisponible = buscarCoreLibre();
        if(coreDisponible != null) {
            executor.submit(() -> {
                coreDisponible.ejecutar(proceso, quantum);
            });
        }
    }
    public Core buscarCoreLibre() {
        for (Core core : cores) {
            if (!core.estaOcupado()) {
                return core;
            }
        }
        return null; // No hay núcleos libres
    }
    public List<Core> getCores() {
        return cores;
    }
}
