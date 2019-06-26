package esei.si.alejandrogg.busquedaACO.accionesInternas;

import java.util.Iterator;
import java.util.List;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Random;
//import java.util.Arrays;

/**
 * Modela una distribución de probabilidad discreta, de la que se puede obtener
 * un número natural aleatorio K con probabilidad pk = P(K), donde P(K) es el valor
 * correspondiente de la función masa de probabilidad discreta.
 *
 * El algoritmo usado es una variante del método de alias propuesta por Michael D. Vose,
 * en su artículo "A Linear Algorithm For Generating Random Numbers With a Given Distribution".
 * Esta variante tiene un tiempo de inicialización O(n) y tiempo de muestreo O(1) para todos
 * los casos, lo que lo convierte en la más eficiente de su categoría.
 * @author Alejandro González García
 */
final class DistribProbabilidadDiscreta {
    /**
     * El generador de números aleatorios de una distribución uniforme que usa esta distribución.
     * Su calidad es decisiva para obtener unos resultados matemáticamente apropiados.
     */
    private final Random rng;
    /**
     * Variable de trabajo que contiene probabilidades intermedias.
     */
    private final double[] prob;
    /**
     * Variable de trabajo que contiene alias.
     */
    private final int[] alias;
    /**
     * El número de elementos para los que está definida la función masa de probabilidad.
     */
    private final int n;

    /**
     * Inicializa una nueva distribución de probabilidad discreta a partir de su función masa de
     * probabilidad y un generador de números aleatorios.
     * @param valoresMasaProb Los valores de la función masa de probabilidad. Los índices del array
     * se interpretan como las preimágenes del valor correspondiente a su posición. Es decir, el valor
     * 0 tendría una probabilidad valoresMasaProb[0] de salir; el 1, valoresMasaProb[1], y así
     * sucesivamente. Aunque el algoritmo funciona igualmente con otro tipo de listas, es recomendable
     * que implementen la interfaz RandomAccess, pues ello implica que los accesos a posiciones arbitrarias
     * son de complejidad O(1), y la inicialización del objeto se completará más rápido.
     * @param rng El generador de números aleatorios a usar para muestrear la distribución, cuando
     * sea necesario.
     */
    public DistribProbabilidadDiscreta(final List<Double> valoresMasaProb, final Random rng) {
        if (valoresMasaProb == null || valoresMasaProb.isEmpty()) {
            throw new IllegalArgumentException("La función masa de probabilidad discreta no está definida para ningún valor.");
        }

        if (rng == null) {
            throw new IllegalArgumentException("No se puede generar una distribución de probabilidad a partir de un RNG nulo.");
        }
        this.rng = rng;

        // Inicialización de variables y estructuras de datos
        this.n = valoresMasaProb.size();
        this.alias = new int[n];
        this.prob = new double[n];
        final double equiprobabilidad = 1.0 / n;
        final Deque<Integer> peque = new ArrayDeque<>(n);
        final Deque<Integer> grande = new ArrayDeque<>(n);

        // Inicializamos las colas de valores de la función de masa de probabilidad pequeños y grandes.
        // De paso, comprobamos que la función masa de probabilidad sea coherente
        double total = 0;
        int i = 0;
        for (double v : valoresMasaProb) {
            if (v > equiprobabilidad) {
                grande.push(i++);
            } else {
                peque.push(i++);
            }

            total += v;
        }
        // La función masa de probabilidad define el espacio muestral. Por tanto,
        // incluye el suceso seguro, cuya probabilidad es 1. Permitimos algo de error
        // al comprobar si la suma de las probabilidades es 1
        if (total < 0.9999999 || total > 1.0000001) {
            throw new IllegalArgumentException("La función masa de probabilidad no cumple su definición: la suma de todos sus valores no es 1.");
        }

        while (!peque.isEmpty() && !grande.isEmpty()) {
            final int l = peque.pop();
            final int g = grande.pop();

            // Actualizamos valores según corresponda
            prob[l] = n * valoresMasaProb.get(l);
            alias[l] = g;

            final double nuevaProb = valoresMasaProb.get(g) + (valoresMasaProb.get(l) - equiprobabilidad);
            valoresMasaProb.set(g, nuevaProb);
            if (nuevaProb > equiprobabilidad) {
                grande.push(g);
            } else {
                peque.push(g);
            }
        }

        // Consumir elementos restantes hasta agotarlos
        while (!grande.isEmpty()) {
            prob[grande.pop()] = 1;
        }
        while (!peque.isEmpty()) {
            prob[peque.pop()] = 1;
        }
    }

    /**
     * Inicializa una nueva distribución de probabilidad discreta a partir de su función masa de
     * probabilidad, usando como generador de números aleatorios la implementación de Random
     * disponible en Java, basada en un generador lineal congruencial.
     * @param valoresMasaProb Los valores de la función masa de probabilidad. Los índices del array
     * se interpretan como las preimágenes del valor correspondiente a su posición. Es decir, el valor
     * 0 tendría una probabilidad valoresMasaProb[0] de salir; el 1, valoresMasaProb[1], y así
     * sucesivamente. Aunque el algoritmo funciona igualmente con otro tipo de listas, es recomendable
     * que implementen la interfaz RandomAccess, pues ello implica que los accesos a posiciones arbitrarias
     * son de complejidad O(1), y la inicialización del objeto se completará más rápido.
     */
    public DistribProbabilidadDiscreta(final List<Double> valoresMasaProb) {
        this(valoresMasaProb, new Random());
    }

    /**
     * Obtiene una muestra aleatoria de la distribución de probabilidad discreta que
     * representa este objeto, de acuerdo con la probabilidad de que salga cada elemento.
     * @return La devandicha muestra aleatoria.
     */
    public int next() {
        final double u = rng.nextDouble() * n;
        final int j = (int) u;
        return u - j <= prob[j] ? j : alias[j];
    }

    // Código usado para hacerse a la idea de si el algoritmo muestra un funcionamiento correcto
    /*public static void main(final String[] args) {
        // Con un millón de muestras se debe de apreciar una gran convergencia hacia
        // los valores de probabilidad reales, dados por la función masa de probabilidad
        final int totalMuestras = 1000000;
        final List<Double> masaProb = Arrays.asList(0.15, 0.1, 0.05, 0.7);
        final DistribProbabilidadDiscreta d = new DistribProbabilidadDiscreta(masaProb);
        final int[] muestrasPorNum = new int[masaProb.size()];
        int i = 0;

        // Tomamos totalMuestras muestras, y contamos cuántas veces ha salido cada resultado
        for (; i < totalMuestras; ++i) {
            ++muestrasPorNum[d.next()];
        }

        // Para cada posible resultado, estimamos su probabilidad a partir de las veces que
        // ha salido
        for (i = 0; i < muestrasPorNum.length; ++i) {
            System.out.println("Probabilidad estimada de i = " + i + ": " + ((double) muestrasPorNum[i]) / totalMuestras);
        }
    }*/
}