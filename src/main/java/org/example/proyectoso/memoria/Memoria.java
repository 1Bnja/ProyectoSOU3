package org.example.proyectoso.memoria;
import org.example.proyectoso.models.*;

import java.util.*;


public class Memoria {
    private final int TAMAÑO_TOTAL;                    
    private final List<BloqueMemoria> bloques;         
    private final Swapping swapping;                   
    private final Object lock = new Object();          

    
    private int memoriaTotalUsada = 0;
    private int fragmentacionExterna = 0;


    public Memoria(int tamañoMB) {
        this.TAMAÑO_TOTAL = tamañoMB;
        this.bloques = new ArrayList<>();
        this.swapping = new Swapping();

        
        bloques.add(new BloqueMemoria(0, TAMAÑO_TOTAL));

        System.out.println("💾 Memoria inicializada: " + TAMAÑO_TOTAL + "MB");
    }


    public boolean asignarMemoria(Proceso proceso) {
        synchronized (lock) {
            int tamañoNecesario = proceso.getTamanoMemoria();

            
            for (int i = 0; i < bloques.size(); i++) {
                BloqueMemoria bloque = bloques.get(i);

                if (!bloque.isOcupado() && bloque.getTamaño() >= tamañoNecesario) {
                    
                    asignarBloqueAProceso(bloque, proceso, i);
                    memoriaTotalUsada += tamañoNecesario;
                    calcularFragmentacion();

                    System.out.println("✅ Memoria asignada a Proceso " + proceso.getId() +
                            " (" + tamañoNecesario + "MB)");
                    return true;
                }
            }

            
            System.out.println("❌ No hay memoria para Proceso " + proceso.getId() +
                    " (" + tamañoNecesario + "MB)");
            return false;
        }
    }


    private void asignarBloqueAProceso(BloqueMemoria bloque, Proceso proceso, int indice) {
        int tamañoNecesario = proceso.getTamanoMemoria();

        if (bloque.getTamaño() == tamañoNecesario) {
            
            bloque.asignar(proceso);
        } else {
            
            int inicioOriginal = bloque.getInicio();

            
            BloqueMemoria bloqueOcupado = new BloqueMemoria(inicioOriginal, tamañoNecesario);
            bloqueOcupado.asignar(proceso);

            
            int inicioLibre = inicioOriginal + tamañoNecesario;
            int tamañoLibre = bloque.getTamaño() - tamañoNecesario;
            BloqueMemoria bloqueLibre = new BloqueMemoria(inicioLibre, tamañoLibre);

            
            bloques.set(indice, bloqueOcupado);
            bloques.add(indice + 1, bloqueLibre);
        }
    }

    public boolean liberarMemoria(Proceso proceso) {
        synchronized (lock) {
            
            for (BloqueMemoria bloque : bloques) {
                if (bloque.isOcupado() &&
                        bloque.getProceso().getId() == proceso.getId()) {

                    int tamañoLiberado = bloque.getTamaño();
                    bloque.liberar();
                    memoriaTotalUsada -= tamañoLiberado;

                    System.out.println("🔓 Memoria liberada de Proceso " + proceso.getId() +
                            " (" + tamañoLiberado + "MB)");

                    
                    fusionarBloquesLibres();
                    calcularFragmentacion();

                    
                    procesarSwapping();

                    return true;
                }
            }

            System.out.println("⚠️ No se encontró memoria para liberar del Proceso " + proceso.getId());
            return false;
        }
    }


    private void fusionarBloquesLibres() {
        boolean fusionado;

        do {
            fusionado = false;

            for (int i = 0; i < bloques.size() - 1; i++) {
                BloqueMemoria actual = bloques.get(i);
                BloqueMemoria siguiente = bloques.get(i + 1);

                
                if (actual.puedeUnirse(siguiente)) {
                    
                    BloqueMemoria primero = (actual.getInicio() < siguiente.getInicio()) ? actual : siguiente;
                    BloqueMemoria segundo = (actual.getInicio() < siguiente.getInicio()) ? siguiente : actual;

                    
                    int nuevoTamaño = primero.getTamaño() + segundo.getTamaño();
                    BloqueMemoria bloqueUnido = new BloqueMemoria(primero.getInicio(), nuevoTamaño);

                    
                    bloques.set(i, bloqueUnido);
                    bloques.remove(i + 1);

                    fusionado = true;
                    break; 
                }
            }
        } while (fusionado);

        
        bloques.sort(Comparator.comparingInt(BloqueMemoria::getInicio));
    }


    private void calcularFragmentacion() {
        int memoriaLibreTotal = 0;
        int bloqueLibreMasGrande = 0;

        for (BloqueMemoria bloque : bloques) {
            if (!bloque.isOcupado()) {
                memoriaLibreTotal += bloque.getTamaño();
                bloqueLibreMasGrande = Math.max(bloqueLibreMasGrande, bloque.getTamaño());
            }
        }

        
        fragmentacionExterna = memoriaLibreTotal - bloqueLibreMasGrande;
    }



    public void moverASwapping(Proceso proceso) {
        swapping.moverASwap(proceso);
    }


    private void procesarSwapping() {
        List<Proceso> procesosLiberados = swapping.procesarCola(this);

        if (!procesosLiberados.isEmpty()) {
            System.out.println("🔄 " + procesosLiberados.size() +
                    " proceso(s) salieron del swapping");
        }
    }


    public List<BloqueMemoria> getBloques() {
        synchronized (lock) {
            return new ArrayList<>(bloques);
        }
    }


    public int[] getEstadoBasico() {
        synchronized (lock) {
            return new int[]{
                    memoriaTotalUsada,
                    TAMAÑO_TOTAL - memoriaTotalUsada,
                    fragmentacionExterna,
                    swapping.getCantidadProcesos()
            };
        }
    }


    public void imprimirEstado() {
        synchronized (lock) {
            System.out.println("\n=== ESTADO DE MEMORIA ===");
            System.out.println("Tamaño total: " + TAMAÑO_TOTAL + "MB");
            System.out.println("Memoria usada: " + memoriaTotalUsada + "MB");
            System.out.println("Memoria libre: " + (TAMAÑO_TOTAL - memoriaTotalUsada) + "MB");
            System.out.println("Fragmentación externa: " + fragmentacionExterna + "MB");
            System.out.printf("Uso: %.1f%%\n", getPorcentajeUso());

            System.out.println("\nBloques de memoria:");
            for (int i = 0; i < bloques.size(); i++) {
                System.out.println(i + ": " + bloques.get(i));
            }

            System.out.println("========================");

            
            swapping.imprimirEstado();
        }
    }


    public void reiniciar() {
        synchronized (lock) {
            bloques.clear();
            bloques.add(new BloqueMemoria(0, TAMAÑO_TOTAL));
            memoriaTotalUsada = 0;
            fragmentacionExterna = 0;
            swapping.limpiar();

            System.out.println("🔄 Memoria reiniciada");
        }
    }

    
    public int getTamañoTotal() {
        return TAMAÑO_TOTAL;
    }

    public int getMemoriaUsada() {
        return memoriaTotalUsada;
    }

    public int getMemoriaLibre() {
        return TAMAÑO_TOTAL - memoriaTotalUsada;
    }

    public int getFragmentacionExterna() {
        return fragmentacionExterna;
    }

    public double getPorcentajeUso() {
        return (double) memoriaTotalUsada / TAMAÑO_TOTAL * 100;
    }

    public Swapping getSwapping() {
        return swapping;
    }

    public int getCantidadBloques() {
        return bloques.size();
    }

    public int getBloquesLibres() {
        return (int) bloques.stream().filter(b -> !b.isOcupado()).count();
    }

    public int getBloquesOcupados() {
        return (int) bloques.stream().filter(b -> b.isOcupado()).count();
    }
}