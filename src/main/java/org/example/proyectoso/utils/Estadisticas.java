package org.example.proyectoso.utils;

import org.example.proyectoso.models.*;
import org.example.proyectoso.memoria.Memoria;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Comparator;

public class Estadisticas {

    private static final DecimalFormat DF = new DecimalFormat("#.##");

    public static void generarArchivoEstadisticas(
            List<Proceso> procesos,
            CPU cpu,
            Memoria memoria,
            int tiempoActual,
            long tiempoInicioSimulacion,
            String algoritmoUtilizado,
            int quantumUtilizado,
            int numCores) {

        try {
            LocalDateTime ahora = LocalDateTime.now();
            DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
            String nombreArchivo = "estadisticas_simulacion_" + formato.format(ahora) + ".txt";

            FileWriter writer = new FileWriter(nombreArchivo);            writer.write("=".repeat(80) + "\n");
            writer.write("           REPORTE DE ESTADÍSTICAS DE SIMULACIÓN\n");
            writer.write("=".repeat(80) + "\n\n");            escribirInformacionGeneral(writer, algoritmoUtilizado, quantumUtilizado,
                    tiempoActual, tiempoInicioSimulacion, procesos, memoria, numCores);

            escribirEstadisticasPorProceso(writer, procesos);

            escribirEstadisticasRendimiento(writer, procesos, tiempoActual);

            escribirEstadisticasMemoria(writer, memoria);

            escribirEstadisticasCPU(writer, cpu);

            escribirResumenFinal(writer, procesos, algoritmoUtilizado, quantumUtilizado, tiempoActual);

            writer.close();

            System.out.println("📄 Archivo de estadísticas generado: " + nombreArchivo);

        } catch (IOException e) {
            System.err.println("❌ Error al generar archivo de estadísticas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Calcula el tiempo de espera promedio
     */
    public static double calcularTiempoEsperaPromedio(List<Proceso> procesos) {
        return procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .mapToInt(Proceso::getTiempoEspera)
                .average()
                .orElse(0.0);
    }

    /**
     * Calcula el tiempo de respuesta promedio
     */
    public static double calcularTiempoRespuestaPromedio(List<Proceso> procesos) {
        return procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .mapToInt(Proceso::getTiempoRespuesta)
                .average()
                .orElse(0.0);
    }

    /**
     * Calcula el tiempo de retorno promedio
     */
    public static double calcularTiempoRetornoPromedio(List<Proceso> procesos) {
        return procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .mapToInt(Proceso::getTiempoRetorno)
                .average()
                .orElse(0.0);
    }

    /**
     * Calcula el throughput del sistema
     */
    public static double calcularThroughput(List<Proceso> procesos, int tiempoActual) {
        int procesosTerminados = (int) procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .count();

        return tiempoActual > 0 ? (double) procesosTerminados / tiempoActual : 0.0;
    }

    /**
     * Calcula la eficiencia del sistema
     */
    public static double calcularEficiencia(List<Proceso> procesos, int tiempoActual, int numeroCores) {
        if (procesos.isEmpty() || tiempoActual == 0) {
            return 0.0;
        }

        int tiempoTotalCPU = procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .mapToInt(Proceso::getDuracion)
                .sum();

        int tiempoTotalDisponible = tiempoActual * numeroCores;

        return tiempoTotalDisponible > 0 ?
                (double) tiempoTotalCPU / tiempoTotalDisponible * 100 : 0.0;
    }

    /**
     * Obtiene estadísticas de estados de procesos como String
     */
    public static String getEstadisticasEstados(List<Proceso> procesosNuevo,
                                                List<Proceso> procesosListo,
                                                List<Proceso> procesosEspera,
                                                List<Proceso> procesosTerminado,
                                                List<Proceso> todosProcesos) {
        StringBuilder stats = new StringBuilder();
        stats.append("=== ESTADÍSTICAS DE ESTADOS ===\n");
        stats.append("Procesos Nuevos: ").append(procesosNuevo.size()).append("\n");
        stats.append("Procesos Listos: ").append(procesosListo.size()).append("\n");
        stats.append("Procesos en Espera: ").append(procesosEspera.size()).append("\n");
        stats.append("Procesos Terminados: ").append(procesosTerminado.size()).append("\n");
        stats.append("Total de Procesos: ").append(todosProcesos.size()).append("\n");

        if (!todosProcesos.isEmpty()) {
            double porcentajeTerminados = (double) procesosTerminado.size() / todosProcesos.size() * 100;
            stats.append("Progreso: ").append(String.format("%.1f%%", porcentajeTerminados)).append("\n");
        }

        return stats.toString();
    }

    private static void escribirInformacionGeneral(FileWriter writer,
                                                   String algoritmoUtilizado,
                                                   int quantumUtilizado,
                                                   int tiempoActual,
                                                   long tiempoInicioSimulacion,
                                                   List<Proceso> procesos,
                                                   Memoria memoria, int numCores) throws IOException {
        LocalDateTime ahora = LocalDateTime.now();
        DateTimeFormatter formatoCompleto = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        writer.write("INFORMACIÓN GENERAL\n");
        writer.write("-".repeat(50) + "\n");
        writer.write("Fecha y hora: " + formatoCompleto.format(ahora) + "\n");
        writer.write("Algoritmo utilizado: " + algoritmoUtilizado + "\n");

        if ("Round Robin".equals(algoritmoUtilizado)) {
            writer.write("Quantum: " + quantumUtilizado + " ms\n");
        }

        writer.write("Tiempo total de simulación: " + tiempoActual + " unidades\n");
        writer.write("Duración real: " + calcularDuracionReal(tiempoInicioSimulacion) + "\n");
        writer.write("Total de procesos: " + procesos.size() + "\n");
        writer.write("Cores utilizados:" + numCores +"\n");

        if (memoria != null) {
            writer.write("Memoria total: " + memoria.getTamañoTotal() + " MB\n");
        }

        writer.write("\n");
    }

    private static void escribirEstadisticasPorProceso(FileWriter writer, List<Proceso> procesos) throws IOException {
        writer.write("ESTADÍSTICAS POR PROCESO\n");
        writer.write("-".repeat(50) + "\n");
        writer.write(String.format("%-4s %-20s %-8s %-8s %-8s %-10s %-10s %-10s %-10s %-8s\n",
                "ID", "Nombre", "Llegada", "Burst", "Memoria", "T.Inicio", "T.Fin", "T.Espera", "T.Retorno", "Estado"));
        writer.write("-".repeat(110) + "\n");

        for (Proceso proceso : procesos) {
            writer.write(String.format("%-4d %-20s %-8d %-8d %-8d %-10d %-10d %-10d %-10d %-8s\n",
                    proceso.getId(),
                    truncarTexto(proceso.getNombre(), 20),
                    proceso.getTiempoLlegada(),
                    proceso.getDuracion(),
                    proceso.getTamanoMemoria(),
                    proceso.getTiempoInicioReal(),
                    proceso.getTiempoFinalizacionReal(),
                    proceso.getTiempoEspera(),
                    proceso.getTiempoRetorno(),
                    proceso.getEstado().toString()));
        }        writer.write("\nVERIFICACIÓN DEL ALGORITMO SJF\n");
        writer.write("-".repeat(50) + "\n");
        verificarSJF(writer, procesos);
        writer.write("\n");
    }

    private static void verificarSJF(FileWriter writer, List<Proceso> procesos) throws IOException {
        Map<Integer, List<Proceso>> procesosPorLlegada = procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .collect(Collectors.groupingBy(Proceso::getTiempoLlegada));

        writer.write("Análisis del orden de ejecución por SJF:\n\n");

        for (Map.Entry<Integer, List<Proceso>> entry : procesosPorLlegada.entrySet()) {
            int tiempoLlegada = entry.getKey();
            List<Proceso> procesosGrupo = entry.getValue();

            if (procesosGrupo.size() > 1) {
                writer.write("Procesos que llegaron en t=" + tiempoLlegada + ":\n");

                List<Proceso> ordenSJF = procesosGrupo.stream()
                        .sorted(Comparator.comparingInt(Proceso::getDuracion))
                        .collect(Collectors.toList());

                List<Proceso> ordenReal = procesosGrupo.stream()
                        .sorted(Comparator.comparingInt(Proceso::getTiempoInicioReal))
                        .collect(Collectors.toList());

                writer.write("  Orden esperado (SJF): ");
                for (int i = 0; i < ordenSJF.size(); i++) {
                    writer.write("P" + ordenSJF.get(i).getId() + "(burst=" + ordenSJF.get(i).getDuracion() + ")");
                    if (i < ordenSJF.size() - 1) writer.write(" → ");
                }
                writer.write("\n");

                writer.write("  Orden real ejecutado: ");
                for (int i = 0; i < ordenReal.size(); i++) {
                    writer.write("P" + ordenReal.get(i).getId() + "(inicio=" + ordenReal.get(i).getTiempoInicioReal() + ")");
                    if (i < ordenReal.size() - 1) writer.write(" → ");
                }
                writer.write("\n");

                boolean sjfCorrecto = true;
                for (int i = 0; i < Math.min(ordenSJF.size(), ordenReal.size()); i++) {
                    if (ordenSJF.get(i).getId() != ordenReal.get(i).getId()) {
                        sjfCorrecto = false;
                        break;
                    }
                }

                writer.write("  ✓ SJF implementado correctamente: " + (sjfCorrecto ? "SÍ" : "NO") + "\n\n");
            }
        }

        writer.write("Procesos que llegaron individualmente:\n");
        for (Proceso proceso : procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .sorted(Comparator.comparingInt(Proceso::getTiempoLlegada))
                .collect(Collectors.toList())) {

            long procesosEnMismoTiempo = procesos.stream()
                    .filter(p -> p.getTiempoLlegada() == proceso.getTiempoLlegada())
                    .count();

            if (procesosEnMismoTiempo == 1) {
                writer.write("  P" + proceso.getId() + " (llegada=" + proceso.getTiempoLlegada() +
                        ", burst=" + proceso.getDuracion() +
                        ", inicio=" + proceso.getTiempoInicioReal() +
                        ", espera=" + proceso.getTiempoEspera() + ")\n");
            }
        }
    }

    private static void escribirEstadisticasRendimiento(FileWriter writer, List<Proceso> procesos, int tiempoActual) throws IOException {
        double promedioEspera = calcularTiempoEsperaPromedio(procesos);
        double promedioRespuesta = calcularTiempoRespuestaPromedio(procesos);
        double promedioRetorno = calcularTiempoRetornoPromedio(procesos);

        int procesosTerminados = (int) procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .count();

        double throughput = calcularThroughput(procesos, tiempoActual);
        double eficiencia = calcularEficiencia(procesos, tiempoActual, 6);
        writer.write("ESTADÍSTICAS DE RENDIMIENTO\n");
        writer.write("-".repeat(50) + "\n");
        writer.write("Procesos completados: " + procesosTerminados + " / " + procesos.size() + "\n");
        writer.write("Porcentaje completado: " + DF.format((double) procesosTerminados / procesos.size() * 100) + "%\n");
        writer.write("Tiempo promedio de espera: " + DF.format(promedioEspera) + " unidades\n");
        writer.write("Tiempo promedio de respuesta: " + DF.format(promedioRespuesta) + " unidades\n");
        writer.write("Tiempo promedio de retorno: " + DF.format(promedioRetorno) + " unidades\n");
        writer.write("Throughput: " + DF.format(throughput) + " procesos/unidad tiempo\n");
        writer.write("Eficiencia del sistema: " + DF.format(eficiencia) + "%\n");
        writer.write("\n");
    }

    private static void escribirEstadisticasMemoria(FileWriter writer, Memoria memoria) throws IOException {
        if (memoria == null) {
            writer.write("ESTADÍSTICAS DE MEMORIA\n");
            writer.write("-".repeat(50) + "\n");
            writer.write("Sistema de memoria no disponible\n\n");
            return;
        }

        writer.write("ESTADÍSTICAS DE MEMORIA\n");
        writer.write("-".repeat(50) + "\n");
        writer.write("Memoria total: " + memoria.getTamañoTotal() + " MB\n");
        writer.write("Memoria usada: " + memoria.getMemoriaUsada() + " MB\n");
        writer.write("Memoria libre: " + memoria.getMemoriaLibre() + " MB\n");
        writer.write("Porcentaje de uso: " + DF.format(memoria.getPorcentajeUso()) + "%\n");

        if (memoria.getSwapping() != null) {
            writer.write("Procesos en swap: " + memoria.getSwapping().getCantidadProcesos() + "\n");
            writer.write("Memoria en swap: " + memoria.getSwapping().getMemoriaRequerida() + " MB\n");
        }

        int bloquesLibres = (int) memoria.getBloques().stream()
                .filter(bloque -> !bloque.isOcupado())
                .count();
        writer.write("Bloques libres: " + bloquesLibres + "\n");
        writer.write("Fragmentación: " + (bloquesLibres > 1 ? "SÍ" : "NO") + "\n");
        writer.write("\n");
    }

    private static void escribirEstadisticasCPU(FileWriter writer, CPU cpu) throws IOException {
        if (cpu == null) {
            writer.write("ESTADÍSTICAS DE CPU\n");
            writer.write("-".repeat(50) + "\n");
            writer.write("Sistema de CPU no disponible\n\n");
            return;
        }

        writer.write("ESTADÍSTICAS DE CPU\n");
        writer.write("-".repeat(50) + "\n");
        writer.write("Número de cores: " + cpu.getNumeroCores() + "\n");
        writer.write("Cores libres: " + cpu.getCoresLibresCount() + "\n");
        writer.write("Cores ocupados: " + cpu.getCoresOcupadosCount() + "\n");
        writer.write("Uso promedio de CPU: " + DF.format(cpu.getUsoPromedioCpu()) + "%\n");
        writer.write("Procesos ejecutados: " + cpu.getProcesosTotalesEjecutados() + "\n");

        writer.write("\nEstado por core:\n");
        for (int i = 0; i < cpu.getNumeroCores(); i++) {
            Core core = cpu.getCore(i);
            if (core != null) {
                writer.write("  Core " + i + ": " +
                        (core.isLibre() ? "LIBRE" : "OCUPADO") +
                        " - Uso: " + DF.format(core.getPorcentajeUso()) + "%\n");
            }
        }
        writer.write("\n");
    }

    private static void escribirResumenFinal(FileWriter writer, List<Proceso> procesos,
                                             String algoritmoUtilizado, int quantumUtilizado,
                                             int tiempoActual) throws IOException {
        writer.write("RESUMEN FINAL Y ANÁLISIS\n");
        writer.write("-".repeat(50) + "\n");

        Proceso procesoMasRapido = procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .min((p1, p2) -> Integer.compare(p1.getTiempoRetorno(), p2.getTiempoRetorno()))
                .orElse(null);

        Proceso procesoMasLento = procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .max((p1, p2) -> Integer.compare(p1.getTiempoRetorno(), p2.getTiempoRetorno()))
                .orElse(null);

        if (procesoMasRapido != null) {
            writer.write("Proceso más rápido: P" + procesoMasRapido.getId() +
                    " (" + procesoMasRapido.getNombre() + ") - " +
                    procesoMasRapido.getTiempoRetorno() + " unidades\n");
        }

        if (procesoMasLento != null) {
            writer.write("Proceso más lento: P" + procesoMasLento.getId() +
                    " (" + procesoMasLento.getNombre() + ") - " +
                    procesoMasLento.getTiempoRetorno() + " unidades\n");
        }

        writer.write("\nANÁLISIS DEL ALGORITMO " + algoritmoUtilizado + ":\n");

        double tiempoEsperaPromedio = calcularTiempoEsperaPromedio(procesos);
        writer.write("Tiempo de espera promedio: " + DF.format(tiempoEsperaPromedio) + " unidades\n");

        long procesosConEspera = procesos.stream()
                .filter(p -> p.getEstado() == EstadoProceso.TERMINADO)
                .filter(p -> p.getTiempoEspera() > 0)
                .count();

        writer.write("Procesos que tuvieron que esperar: " + procesosConEspera + "\n");
        writer.write("Eficiencia del algoritmo " + algoritmoUtilizado + ": " +
                DF.format(calcularEficiencia(procesos, tiempoActual, 6)) + "%\n");


        writer.write("\n" + "=".repeat(80) + "\n");
        writer.write("Fin del reporte\n");
        writer.write("=".repeat(80) + "\n");
    }


    private static String calcularDuracionReal(long tiempoInicioSimulacion) {
        if (tiempoInicioSimulacion > 0) {
            long duracionMs = System.currentTimeMillis() - tiempoInicioSimulacion;
            return String.format("%.2f segundos", duracionMs / 1000.0);
        }
        return "No disponible";
    }

    private static String truncarTexto(String texto, int longitud) {
        if (texto == null) return "";
        return texto.length() > longitud ?
                texto.substring(0, longitud - 3) + "..." : texto;
    }
}