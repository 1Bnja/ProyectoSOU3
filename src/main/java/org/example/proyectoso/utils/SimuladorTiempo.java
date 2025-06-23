package org.example.proyectoso.utils;

public class SimuladorTiempo {
    
    private static double factorVelocidad = 1.0;

    
    private static long tiempoInicio = 0;

    
    private static long tiempoActualSimulado = 0;

    
    private static boolean enPausa = false;
    private static boolean inicializado = false;

    
    public static void iniciar() {
        tiempoInicio = System.currentTimeMillis();
        tiempoActualSimulado = 0;
        inicializado = true;
        enPausa = false;
        System.out.println("⏱️ Simulador de tiempo iniciado. Factor de velocidad: " + factorVelocidad + "x");
    }

    
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

    
    public static void setFactorVelocidad(double factor) {
        if (factor <= 0) {
            throw new IllegalArgumentException("El factor de velocidad debe ser mayor que 0");
        }

        
        getTiempoActual();

        
        factorVelocidad = factor;

        
        tiempoInicio = System.currentTimeMillis() - Math.round(tiempoActualSimulado / factorVelocidad);

        System.out.println("⏱️ Factor de velocidad cambiado a: " + factorVelocidad + "x");
    }

    
    public static void pausar() {
        if (!enPausa) {
            getTiempoActual(); 
            enPausa = true;
            System.out.println("⏸️ Simulación pausada en tiempo: " + tiempoActualSimulado + "ms");
        }
    }

    
    public static void reanudar() {
        if (enPausa) {
            
            tiempoInicio = System.currentTimeMillis() - Math.round(tiempoActualSimulado / factorVelocidad);
            enPausa = false;
            System.out.println("▶️ Simulación reanudada en tiempo: " + tiempoActualSimulado + "ms");
        }
    }

    
    public static void esperar(long milisegundos) {
        if (milisegundos <= 0) return;

        long tiempoEsperaReal = Math.round(milisegundos / factorVelocidad);

        try {
            Thread.sleep(tiempoEsperaReal);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    
    public static void avanzarTiempo(long milisegundos) {
        tiempoActualSimulado += milisegundos;
    }

    
    public static void reiniciar() {
        iniciar();
    }

    
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
