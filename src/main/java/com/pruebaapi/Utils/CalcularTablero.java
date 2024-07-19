package com.pruebaapi.Utils;

public class CalcularTablero {
    public static int[][] sumarTableros(int[][] tablero1, int[][] nuevasCasillas) throws IllegalArgumentException {
        int filas = tablero1.length;
        int columnas = tablero1[0].length;
        int[][] resultado = new int[filas][columnas];

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                int casillaActual = tablero1[i][j];
                int nuevaCasilla = nuevasCasillas[i][j];
                
                if (nuevaCasilla == 0) {
                    resultado[i][j] = casillaActual;
                } else if (nuevaCasilla == 1 && casillaActual == 0) {
                    resultado[i][j] = 1;
                } else if (nuevaCasilla == 1 && casillaActual == 3) {
                    resultado[i][j] = 4;
                } else if (nuevaCasilla == 1 && (casillaActual == 1 || casillaActual == 4)) {
                    throw new IllegalArgumentException("Error: No se puede impactar una casilla ya impactada o hundida.");
                } else {
                    throw new IllegalArgumentException("Error: Valor inválido en las nuevas casillas.");
                }
            }
        }
        return resultado;
    }

    public static int calcularPuntuacion(int[][] nuevasCasillas, int[][] tableroActual) {
        int puntuacion = 0;
        for (int i = 0; i < nuevasCasillas.length; i++) {
            for (int j = 0; j < nuevasCasillas[i].length; j++) {
                if (nuevasCasillas[i][j] == 1 && tableroActual[i][j] == 3) {
                    puntuacion += 2;
                }
            }
        }
        return puntuacion;
    }
}


