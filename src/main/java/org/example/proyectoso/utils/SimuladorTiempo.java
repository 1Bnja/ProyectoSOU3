package org.example.proyectoso.utils;

public class SimuladorTiempo {
    // Factor de escala para la simulación (ms reales = 1 unidad de tiempo simulado)
    private static double factorVelocidad = 1.0;

    // Tiempo de inicio de la simulación en milisegundos reales
    private static long tiempoInicio = 0;

    // Tiempo actual de la simulación
    private static long tiempoActualSimulado = 0;

    // Estado de la simulación
    private static boolean enPausa = false;
    private static boolean inicializado = false;

    /**
     * Inicializa el simulador de tiempo
     */
    public static void iniciar() {
        tiempoInicio = System.currentTimeMillis();
        tiempoActualSimulado = 0;
        inicializado = true;
        enPausa = false;
        System.out.println("⏱️ Simulador de tiempo iniciado. Factor de velocidad: " + factorVelocidad + "x");
    }

    /**
     * Obtiene el tiempo actual de la simulación
     * @return Tiempo simulado en milisegundos
     */
    public static long getTiempoActual() {
        if (!inicializado) {
            iniciar();
        }

        if (!enPausa) {
            long tiempoReal = System.currentTimeMillis() - tiempoInicio;
            tiempoActualSimulado = Math.round(tiempoReal * factorVelocidad);
        }

        return tiempoActualSimulado;
    }

    /**
     * Configura el factor de velocidad de la simulación
     * @param factor Factor de velocidad (1.0 = tiempo real, 2.0 = doble de velocidad)
     */
    public static void setFactorVelocidad(double factor) {
        if (factor <= 0) {
            throw new IllegalArgumentException("El factor de velocidad debe ser mayor que 0");
        }

        // Actualizar el tiempo actual antes de cambiar el factor
        getTiempoActual();

        // Cambiar el factor de velocidad
        factorVelocidad = factor;

        // Actualizar el tiempo de inicio para mantener la continuidad
        tiempoInicio = System.currentTimeMillis() - Math.round(tiempoActualSimulado / factorVelocidad);

        System.out.println("⏱️ Factor de velocidad cambiado a: " + factorVelocidad + "x");
    }

    /**
     * Pausa la simulación
     */
    public static void pausar() {
        if (!enPausa) {
            getTiempoActual(); // Actualizar el tiempo antes de pausar
            enPausa = true;
            System.out.println("⏸️ Simulación pausada en tiempo: " + tiempoActualSimulado + "ms");
        }
    }

    /**
     * Reanuda la simulación
     */
    public static void reanudar() {
        if (enPausa) {
            // Ajustar el tiempo de inicio para mantener la continuidad
            tiempoInicio = System.currentTimeMillis() - Math.round(tiempoActualSimulado / factorVelocidad);
            enPausa = false;
            System.out.println("▶️ Simulación reanudada en tiempo: " + tiempoActualSimulado + "ms");
        }
    }

    /**
     * Simula una espera del tiempo especificado
     * @param milisegundos Tiempo a esperar en milisegundos simulados
     */
    public static void esperar(long milisegundos) {
        if (milisegundos <= 0) return;

        long tiempoEsperaReal = Math.round(milisegundos / factorVelocidad);

        try {
            Thread.sleep(tiempoEsperaReal);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Avanza manualmente el tiempo simulado
     * @param milisegundos Cantidad de milisegundos a avanzar
     */
    public static void avanzarTiempo(long milisegundos) {
        tiempoActualSimulado += milisegundos;
    }

    /**
     * Reinicia el simulador de tiempo
     */
    public static void reiniciar() {
        iniciar();
    }

    /**
     * Formatea el tiempo en formato legible: HH:MM:SS.mmm
     * @param milisegundos Tiempo en milisegundos
     * @return Cadena formateada con el tiempo
     */
    public static String formatearTiempo(long milisegundos) {
        long segundos = milisegundos / 1000;
        long ms = milisegundos % 1000;
        long minutos = segundos / 60;
        segundos = segundos % 60;
        long horas = minutos / 60;
        minutos = minutos % 60;

        return String.format("%02d:%02d:%02d.%03d", horas, minutos, segundos, ms);
    }
}
