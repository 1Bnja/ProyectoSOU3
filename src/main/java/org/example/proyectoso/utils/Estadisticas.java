package org.example.proyectoso.utils;

import org.example.proyectoso.models.*;
import org.example.proyectoso.memoria.Memoria;

import java.util.*;
import java.text.DecimalFormat;

/**
 * Clase para recopilar y mostrar estadísticas de la ejecución de procesos
 * en el sistema operativo simulado.
 */
public class Estadisticas {
    // Métricas generales
    private long tiempoTotalEjecucion;
    private long tiempoInicioSimulacion;
    private long tiempoFinSimulacion;

    // Métricas de procesos
    private int totalProcesos;
    private int procesosTerminados;
    private int procesosBloqueados;

    // Tiempos medios
    private double tiempoMedioEspera;
    private double tiempoMedioRetorno;
    private double tiempoMedioRespuesta;

    // Métricas de CPU
    private double utilizacionCPU;
    private int cambiosContexto;
    private Map<Core, Integer> procesosAtendidosPorCore;

    // Métricas de memoria
    private double utilizacionMediaMemoria;
    private int pageFaults;
    private int swappingOperations;

    // Historial para gráficos
    private List<Double> historialUtilizacionCPU;
    private List<Double> historialUtilizacionMemoria;

    /**
     * Constructor para inicializar las estadísticas
     */
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

    /**
     * Inicializa la recopilación de estadísticas
     */
    public void iniciar() {
        tiempoInicioSimulacion = SimuladorTiempo.getTiempoActual();
        System.out.println("📊 Iniciando recopilación de estadísticas en: " +
                SimuladorTiempo.formatearTiempo(tiempoInicioSimulacion));
    }

    /**
     * Finaliza la recopilación de estadísticas
     */
    public void finalizar() {
        tiempoFinSimulacion = SimuladorTiempo.getTiempoActual();
        tiempoTotalEjecucion = tiempoFinSimulacion - tiempoInicioSimulacion;
        System.out.println("📊 Finalizando recopilación de estadísticas en: " +
                SimuladorTiempo.formatearTiempo(tiempoFinSimulacion));
    }

    /**
     * Registra un cambio de contexto
     */
    public void registrarCambioContexto() {
        cambiosContexto++;
    }

    /**
     * Registra la atención de un proceso por un core
     * @param core Core que atendió el proceso
     */
    public void registrarProcesoAtendidoPorCore(Core core) {
        procesosAtendidosPorCore.put(core, procesosAtendidosPorCore.getOrDefault(core, 0) + 1);
    }

    /**
     * Registra un proceso terminado y actualiza las métricas
     * @param proceso Proceso que ha terminado
     */
    public void registrarProcesoTerminado(Proceso proceso) {
        procesosTerminados++;

        // Acumular tiempos para calcular medias
        tiempoMedioEspera = ((tiempoMedioEspera * (procesosTerminados - 1)) + proceso.getTiempoEspera()) / procesosTerminados;
        tiempoMedioRetorno = ((tiempoMedioRetorno * (procesosTerminados - 1)) + proceso.getTiempoRetorno()) / procesosTerminados;
        tiempoMedioRespuesta = ((tiempoMedioRespuesta * (procesosTerminados - 1)) + proceso.getTiempoRespuesta()) / procesosTerminados;

        System.out.println("📊 Proceso terminado ID: " + proceso.getId() +
                " | Tiempo espera: " + proceso.getTiempoEspera() +
                " | Tiempo retorno: " + proceso.getTiempoRetorno() +
                " | Tiempo respuesta: " + proceso.getTiempoRespuesta());
    }

    /**
     * Registra un nuevo proceso en el sistema
     */
    public void registrarNuevoProceso() {
        totalProcesos++;
    }

    /**
     * Registra un page fault
     */
    public void registrarPageFault() {
        pageFaults++;
    }

    /**
     * Registra una operación de swapping
     */
    public void registrarSwapping() {
        swappingOperations++;
    }

    /**
     * Actualiza la utilización de CPU
     * @param porcentajeUtilizacion Porcentaje de utilización actual
     */
    public void actualizarUtilizacionCPU(double porcentajeUtilizacion) {
        utilizacionCPU = (utilizacionCPU * historialUtilizacionCPU.size() + porcentajeUtilizacion) /
                         (historialUtilizacionCPU.size() + 1);
        historialUtilizacionCPU.add(porcentajeUtilizacion);
    }

    /**
     * Actualiza la utilización de memoria
     * @param memoria Objeto memoria para calcular utilización
     */
    public void actualizarUtilizacionMemoria(Memoria memoria) {
        try {
            // Usar reflection para acceder a los campos privados
            java.lang.reflect.Field memoriaTotalUsadaField = Memoria.class.getDeclaredField("memoriaTotalUsada");
            memoriaTotalUsadaField.setAccessible(true);
            int memoriaTotalUsada = (int) memoriaTotalUsadaField.get(memoria);

            java.lang.reflect.Field tamañoTotalField = Memoria.class.getDeclaredField("TAMAÑO_TOTAL");
            tamañoTotalField.setAccessible(true);
            int tamañoTotal = (int) tamañoTotalField.get(memoria);

            // Calcular porcentaje de utilización
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

    /**
     * Muestra un resumen de las estadísticas recopiladas
     */
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

    /**
     * Genera un informe detallado de las estadísticas
     * @return Cadena con el informe completo
     */
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

    /**
     * Formatea un valor decimal con dos decimales
     * @param valor Valor a formatear
     * @return Cadena formateada
     */
    private String formatearDecimal(double valor) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(valor);
    }

    /**
     * Formatea un valor como porcentaje
     * @param valor Valor a formatear (0-1)
     * @return Cadena formateada como porcentaje
     */
    private String formatearPorcentaje(double valor) {
        DecimalFormat df = new DecimalFormat("0.00%");
        return df.format(valor);
    }

    // Getters para acceder a las estadísticas

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
