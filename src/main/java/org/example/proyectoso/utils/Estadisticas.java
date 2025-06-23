package org.example.proyectoso.utils;

import org.example.proyectoso.models.*;
import org.example.proyectoso.memoria.Memoria;

import java.util.*;
import java.text.DecimalFormat;


public class Estadisticas {
    
    private long tiempoTotalEjecucion;
    private long tiempoInicioSimulacion;
    private long tiempoFinSimulacion;

    
    private int totalProcesos;
    private int procesosTerminados;
    private int procesosBloqueados;

    
    private double tiempoMedioEspera;
    private double tiempoMedioRetorno;
    private double tiempoMedioRespuesta;

    
    private double utilizacionCPU;
    private int cambiosContexto;
    private Map<Core, Integer> procesosAtendidosPorCore;

    
    private double utilizacionMediaMemoria;
    private int pageFaults;
    private int swappingOperations;

    
    private List<Double> historialUtilizacionCPU;
    private List<Double> historialUtilizacionMemoria;

    
    public Estadisticas() {
        tiempoTotalEjecucion = 0;
        tiempoInicioSimulacion = 0;
        tiempoFinSimulacion = 0;

        totalProcesos = 0;
        procesosTerminados = 0;
        procesosBloqueados = 0;

        tiempoMedioEspera = 0.0;
        tiempoMedioRetorno = 0.0;
        tiempoMedioRespuesta = 0.0;

        utilizacionCPU = 0.0;
        cambiosContexto = 0;
        procesosAtendidosPorCore = new HashMap<>();

        utilizacionMediaMemoria = 0.0;
        pageFaults = 0;
        swappingOperations = 0;

        historialUtilizacionCPU = new ArrayList<>();
        historialUtilizacionMemoria = new ArrayList<>();
    }

    
    public void iniciar() {
        tiempoInicioSimulacion = SimuladorTiempo.getTiempoActual();
        System.out.println("📊 Iniciando recopilación de estadísticas en: " +
                SimuladorTiempo.formatearTiempo(tiempoInicioSimulacion));
    }

    
    public void finalizar() {
        tiempoFinSimulacion = SimuladorTiempo.getTiempoActual();
        tiempoTotalEjecucion = tiempoFinSimulacion - tiempoInicioSimulacion;
        System.out.println("📊 Finalizando recopilación de estadísticas en: " +
                SimuladorTiempo.formatearTiempo(tiempoFinSimulacion));
    }

    
    public void registrarCambioContexto() {
        cambiosContexto++;
    }

    
    public void registrarProcesoAtendidoPorCore(Core core) {
        procesosAtendidosPorCore.put(core, procesosAtendidosPorCore.getOrDefault(core, 0) + 1);
    }

    
    public void registrarProcesoTerminado(Proceso proceso) {
        procesosTerminados++;

        
        tiempoMedioEspera = ((tiempoMedioEspera * (procesosTerminados - 1)) + proceso.getTiempoEspera()) / procesosTerminados;
        tiempoMedioRetorno = ((tiempoMedioRetorno * (procesosTerminados - 1)) + proceso.getTiempoRetorno()) / procesosTerminados;
        tiempoMedioRespuesta = ((tiempoMedioRespuesta * (procesosTerminados - 1)) + proceso.getTiempoRespuesta()) / procesosTerminados;

        System.out.println("📊 Proceso terminado ID: " + proceso.getId() +
                " | Tiempo espera: " + proceso.getTiempoEspera() +
                " | Tiempo retorno: " + proceso.getTiempoRetorno() +
                " | Tiempo respuesta: " + proceso.getTiempoRespuesta());
    }

    
    public void registrarNuevoProceso() {
        totalProcesos++;
    }

    
    public void registrarPageFault() {
        pageFaults++;
    }

    
    public void registrarSwapping() {
        swappingOperations++;
    }

    
    public void actualizarUtilizacionCPU(double porcentajeUtilizacion) {
        utilizacionCPU = (utilizacionCPU * historialUtilizacionCPU.size() + porcentajeUtilizacion) /
                         (historialUtilizacionCPU.size() + 1);
        historialUtilizacionCPU.add(porcentajeUtilizacion);
    }

    
    public void actualizarUtilizacionMemoria(Memoria memoria) {
        try {
            
            java.lang.reflect.Field memoriaTotalUsadaField = Memoria.class.getDeclaredField("memoriaTotalUsada");
            memoriaTotalUsadaField.setAccessible(true);
            int memoriaTotalUsada = (int) memoriaTotalUsadaField.get(memoria);

            java.lang.reflect.Field tamañoTotalField = Memoria.class.getDeclaredField("TAMAÑO_TOTAL");
            tamañoTotalField.setAccessible(true);
            int tamañoTotal = (int) tamañoTotalField.get(memoria);

            
            double porcentajeUtilizacion = (double) memoriaTotalUsada / tamañoTotal;

            utilizacionMediaMemoria = (utilizacionMediaMemoria * historialUtilizacionMemoria.size() + porcentajeUtilizacion) /
                                     (historialUtilizacionMemoria.size() + 1);
            historialUtilizacionMemoria.add(porcentajeUtilizacion);

            System.out.println("📊 Utilización de memoria actualizada: " + formatearPorcentaje(porcentajeUtilizacion) +
                    " (" + memoriaTotalUsada + "MB / " + tamañoTotal + "MB)");
        } catch (Exception e) {
            System.err.println("Error al actualizar estadísticas de memoria: " + e.getMessage());
        }
    }

    
    public void mostrarResumen() {
        System.out.println("\n========== RESUMEN DE ESTADÍSTICAS ==========");
        System.out.println("⏱️ Tiempo total de ejecución: " +
                SimuladorTiempo.formatearTiempo(tiempoTotalEjecucion));

        System.out.println("\n----- PROCESOS -----");
        System.out.println("🔢 Total de procesos: " + totalProcesos);
        System.out.println("✅ Procesos terminados: " + procesosTerminados);
        if (totalProcesos > 0) {
            System.out.println("📋 Porcentaje completado: " +
                    formatearPorcentaje((double)procesosTerminados / totalProcesos));
        }

        System.out.println("\n----- TIEMPOS MEDIOS -----");
        System.out.println("⌛ Tiempo medio de espera: " +
                formatearDecimal(tiempoMedioEspera) + " ms");
        System.out.println("⏳ Tiempo medio de retorno: " +
                formatearDecimal(tiempoMedioRetorno) + " ms");
        System.out.println("⏰ Tiempo medio de respuesta: " +
                formatearDecimal(tiempoMedioRespuesta) + " ms");

        System.out.println("\n----- CPU -----");
        System.out.println("💻 Utilización de CPU: " +
                formatearPorcentaje(utilizacionCPU));
        System.out.println("🔄 Cambios de contexto: " + cambiosContexto);

        System.out.println("\n----- MEMORIA -----");
        System.out.println("🧠 Utilización media de memoria: " +
                formatearPorcentaje(utilizacionMediaMemoria));
        System.out.println("📉 Page faults: " + pageFaults);
        System.out.println("💾 Operaciones de swapping: " + swappingOperations);

        System.out.println("\n----- CORES -----");
        procesosAtendidosPorCore.forEach((core, cantidad) -> {
            System.out.println("🔌 " + core.getNombre() + ": " + cantidad + " procesos atendidos");
        });

        System.out.println("===========================================");
    }

    
    public String generarInforme() {
        StringBuilder informe = new StringBuilder();

        informe.append("INFORME DE RENDIMIENTO DEL SISTEMA\n");
        informe.append("=================================\n\n");

        informe.append("DATOS GENERALES:\n");
        informe.append("- Tiempo de inicio: ").append(SimuladorTiempo.formatearTiempo(tiempoInicioSimulacion)).append("\n");
        informe.append("- Tiempo de finalización: ").append(SimuladorTiempo.formatearTiempo(tiempoFinSimulacion)).append("\n");
        informe.append("- Duración total: ").append(SimuladorTiempo.formatearTiempo(tiempoTotalEjecucion)).append("\n\n");

        informe.append("PROCESOS:\n");
        informe.append("- Total de procesos: ").append(totalProcesos).append("\n");
        informe.append("- Procesos terminados: ").append(procesosTerminados).append("\n");
        informe.append("- Procesos bloqueados: ").append(procesosBloqueados).append("\n");
        if (totalProcesos > 0) {
            informe.append("- Tasa de finalización: ").append(formatearPorcentaje((double)procesosTerminados / totalProcesos)).append("\n\n");
        }

        informe.append("RENDIMIENTO CPU:\n");
        informe.append("- Utilización media: ").append(formatearPorcentaje(utilizacionCPU)).append("\n");
        informe.append("- Cambios de contexto: ").append(cambiosContexto).append("\n\n");

        informe.append("TIEMPOS:\n");
        informe.append("- Tiempo medio de espera: ").append(formatearDecimal(tiempoMedioEspera)).append(" ms\n");
        informe.append("- Tiempo medio de retorno: ").append(formatearDecimal(tiempoMedioRetorno)).append(" ms\n");
        informe.append("- Tiempo medio de respuesta: ").append(formatearDecimal(tiempoMedioRespuesta)).append(" ms\n\n");

        informe.append("MEMORIA:\n");
        informe.append("- Utilización media: ").append(formatearPorcentaje(utilizacionMediaMemoria)).append("\n");
        informe.append("- Page faults: ").append(pageFaults).append("\n");
        informe.append("- Operaciones de swapping: ").append(swappingOperations).append("\n\n");

        informe.append("DISTRIBUCIÓN POR CORES:\n");
        procesosAtendidosPorCore.forEach((core, cantidad) -> {
            informe.append("- ").append(core.getNombre()).append(": ").append(cantidad).append(" procesos\n");
        });

        return informe.toString();
    }

    
    private String formatearDecimal(double valor) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(valor);
    }

    
    private String formatearPorcentaje(double valor) {
        DecimalFormat df = new DecimalFormat("0.00%");
        return df.format(valor);
    }

    

    public long getTiempoTotalEjecucion() {
        return tiempoTotalEjecucion;
    }

    public int getTotalProcesos() {
        return totalProcesos;
    }

    public int getProcesosTerminados() {
        return procesosTerminados;
    }

    public double getTiempoMedioEspera() {
        return tiempoMedioEspera;
    }

    public double getTiempoMedioRetorno() {
        return tiempoMedioRetorno;
    }

    public double getTiempoMedioRespuesta() {
        return tiempoMedioRespuesta;
    }

    public double getUtilizacionCPU() {
        return utilizacionCPU;
    }

    public int getCambiosContexto() {
        return cambiosContexto;
    }

    public double getUtilizacionMediaMemoria() {
        return utilizacionMediaMemoria;
    }

    public int getPageFaults() {
        return pageFaults;
    }

    public int getSwappingOperations() {
        return swappingOperations;
    }

    public List<Double> getHistorialUtilizacionCPU() {
        return new ArrayList<>(historialUtilizacionCPU);
    }

    public List<Double> getHistorialUtilizacionMemoria() {
        return new ArrayList<>(historialUtilizacionMemoria);
    }
}
