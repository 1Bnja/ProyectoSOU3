package org.example.proyectoso.models;

/**
 * Clase para representar una fila en la tabla de colas de procesos
 */
public class FilaCola {
    private final String nuevo;
    private final String listo;
    private final String espera;
    private final String terminado;

    public FilaCola(String nuevo, String listo, String espera, String terminado) {
        this.nuevo = nuevo != null ? nuevo : "";
        this.listo = listo != null ? listo : "";
        this.espera = espera != null ? espera : "";
        this.terminado = terminado != null ? terminado : "";
    }

    public String getNuevo() {
        return nuevo;
    }

    public String getListo() {
        return listo;
    }

    public String getEspera() {
        return espera;
    }

    public String getTerminado() {
        return terminado;
    }

    @Override
    public String toString() {
        return String.format("FilaColas[nuevo=%s, listo=%s, espera=%s, terminado=%s]",
                nuevo, listo, espera, terminado);
    }
}