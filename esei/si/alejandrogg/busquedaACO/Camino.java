// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Deque;
import java.util.Queue;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import esei.si.alejandrogg.busquedaACO.util.IteradorSoloLectura;
import esei.si.alejandrogg.busquedaACO.util.SpliteratorSoloLectura;

/**
 * Modela un camino, que es una serie de carreteras (aristas) de un mapa (grafo).
 * Una vez que se le han añadido las carreteras deseadas, un camino puede volverse inmutable,
 * lo que facilita en gran medida la realización de operaciones eficientes con él entre
 * varios hilos de ejecución. Un camino inmutable puede, además, compararse con otros caminos,
 * según el orden natural de su distancia total.
 * @author Alejandro González García
 */
public final class Camino implements Comparable<Camino>, Iterable<Carretera> {
    /**
     * El grafo de carreteras asociado a este camino, al que pertenecen las localidades y carreteras
     * que formarán parte de este camino.
     */
    private final GrafoCarreteras grafoCarreteras;
    /**
     * La cola que contiene las carreteras por las que ha pasado este camino. Inicialmente está vacía.
     */
    private final Queue<Carretera> carreteras = new ConcurrentLinkedQueue<>();
    /**
     * La cola que contiene las localidades por las que ha pasado este camino, en orden FIFO.
     * Inicialmente esta vacía, y su tamaño crece en múltiplos de dos.
     */
    private final Deque<Localidad> localidades = new ConcurrentLinkedDeque<>();
    /**
     * El {@link StringBuilder} usado internamente para ir construyendo la representación
     * textual del camino según se va inicializando. De esta forma, sucesivas llamadas a
     * {@link toString} no tienen que recomputar esta representación.
     */
    private StringBuilder sb = new StringBuilder();
    /**
     * La representación textual final del camino, para usar una vez esté inicializado.
     */
    private volatile String caminoComoCadena = null;
    /**
     * Toma el valor verdadero si y solo si el camino ya se ha construido totalmente; es decir,
     * si el responsable de crearlo ha añadido todas las carreteras que forman parte de él, y
     * no se espera que nadie más lo haga.
     */
    private volatile boolean construido = false;
    /**
     * La distancia total actual del camino ya construido, para evitar tener que computarla repetidamente.
     */
    private volatile int distanciaTotalActual = 0;
    /**
     * El código de dispersión actual del camino, para evitar tener que computarlo más de una vez.
     */
    private volatile int codigoDispersion = 1;
    /**
     * El número de localidades que forman parte del camino actualmente, para evitar tener que invocar
     * el método size() en {@link localidades}, que tiene complejidad O(n).
     */
    private volatile int numLocalidades = 0;

    /**
     * Crea un nuevo camino, asociado a un determinado grafo de carreteras.
     * @param grafoCarreteras El grafo de carreteras asociado al camino.
     */
    public Camino(final GrafoCarreteras grafoCarreteras) {
        if (grafoCarreteras == null) {
            throw new IllegalArgumentException("El grafo de carreteras asociado a un camino no puede ser nulo.");
        }
        this.grafoCarreteras = grafoCarreteras;
    }

    /**
     * Añade una carretera al final de este camino, teniendo en cuenta que se ha tomado una carretera
     * en un orden determinado, desde una localidad A hacia otra B. Esta operación tendrá éxito solo si
     * el camino no se ha terminado de construir todavía, y asume que solo un hilo la emplea, al menos hasta
     * que el camino se termine de construir. Para garantizar esta última asunción, basta con mantener
     * la referencia a este objeto como variable local de un método hasta que se termine de construir.
     * @param a La localidad desde la que se inicia el tramo del camino a añadir.
     * @param b La localidad donde termina el tramo del camino a añadir.
     * @throws IllegalArgumentException Si no hay una carretera que conecte ambas localidades.
     * @throws IllegalStateException Si el camino ya se ha terminado de construir, pero se intenta añadir
     * una nueva carretera a él.
     */
    public void addCarretera(final Localidad a, final Localidad b) {
        final Carretera carretera = grafoCarreteras.getCarretera(a, b);
        if (carretera == null) {
            throw new IllegalArgumentException("No existe una carretera que conecte las localidades especificadas.");
        }
        if (construido) {
            throw new IllegalStateException("No se puede añadir una carretera a un camino ya construido.");
        }

        // Añadir la carretera a la cola, actualizar el código de dispersión del camino,
        // e incrementar la distancia total actual
        carreteras.add(carretera);
        codigoDispersion = 31 * codigoDispersion + carretera.hashCode(); // https://docs.oracle.com/javase/8/docs/api/java/util/List.html#hashCode--
        distanciaTotalActual += carretera.getDistancia();

        // Solo añadir la primera localidad si la lista está vacía, para evitar localidades
        // repetidas
        if (localidades.isEmpty()) {
            localidades.add(a);
            sb.append(a.getNombre()).append(", ");
            ++numLocalidades;
        }
        localidades.add(b);
        sb.append(b.getNombre()).append(", ");
        ++numLocalidades;
    }

    /**
     * Marca este camino como construido. Un camino ya construido no permite la adición de nuevas
     * carreteras a él, volviéndose esencialmente de solo lectura, garantizando la coherencia de
     * la información si varios hilos acceden al camino (pues todos ellos verán la última versión
     * de los datos), y permitiendo operaciones de lectura sobre él. Este método está diseñado para
     * ser llamado por el mismo que ha estado invocando {@link addCarretera}.
     */
    public void terminarConstruccion() {
        // Eliminar coma sobrante
        try {
            sb.delete(sb.length() - 2, sb.length());
        } catch (StringIndexOutOfBoundsException ignorar) {}

        caminoComoCadena = sb.toString();
        sb = null; // Ya no necesitamos seguir construyendo la cadena
        construido = true;
    }

    /**
     * Obtiene la última localidad que forma parte de este camino construido. En caso de que el
     * camino no tenga todavía localidades, se devuelve el valor nulo.
     * @return La devandicha localidad.
     * @throws IllegalStateException Si el camino todavía no se ha terminado de construir.
     */
    public Localidad getUltimaLocalidad() {
        if (construido) {
            try {
                return localidades.getLast();
            } catch (NoSuchElementException | IndexOutOfBoundsException exc) {
                return null;
            }
        } else {
            throw new IllegalStateException("No se puede obtener la última localidad de un camino todavía no construido.");
        }
    }

    /**
     * Calcula la distancia total abarcada por este camino, si está ya construido.
     * @implNote. La implementación de este método tiene complejidad O(1).
     * @return La devandicha distancia.
     * @throws IllegalStateException Si el camino todavía no se ha terminado de construir.
     */
    public int distanciaTotal() {
        if (!construido) {
            throw new IllegalStateException("No se puede obtener la distancia total de un camino todavía no construido.");
        }

        return distanciaTotalActual;
    }

    /**
     * {@inheritDoc}
     * @implNote. La implementación de este método tiene complejidad O(1).
     * @throws IllegalStateException Si algún camino implicado no está construido.
     */
    @Override
    public int compareTo(final Camino otro) {
        if (otro == null) {
            throw new NullPointerException("No se puede comparar un camino con otro nulo.");
        }

        return distanciaTotal() - otro.distanciaTotal();
    }

    /**
     * {@inheritDoc}
     * @implNote. La implementación de este método tiene complejidad O(n) en el peor caso,
     * siendo n el número de localidades por las que pasa el camino, por lo que solo se
     * recomienda usar este método si es estrictamente necesario. No obstante, si es obvio
     * que los caminos no pueden ser iguales por tener longitudes diferentes, la complejidad
     * se reduce a O(1).
     * @throws IllegalStateException Si algún camino implicado no está construido.
     */
    @Override
    public boolean equals(final Object otro) {
        boolean toret = otro instanceof Camino;
        Camino otroCamino;

        if (toret) {
            otroCamino = (Camino) otro;

            if (!construido || !otroCamino.construido) {
                throw new IllegalStateException("No se puede comparar la igualdad de dos caminos cuando al menos uno no está construido.");
            }

            // Si no tienen el mismo tamaño, ni de lejos son iguales
            toret = numLocalidades == otroCamino.numLocalidades;
            if (toret) {
                // TODO: quizás optimizar esto con Spliterator
                final Iterator<Localidad> iter1 = localidades.iterator();
                final Iterator<Localidad> iter2 = localidades.iterator();

                while (toret && iter1.hasNext()) {
                    final Localidad l1 = iter1.next();
                    final Localidad l2 = iter2.next();
                    toret = l1.equals(l2);
                }
            }
        }

        return toret;
    }

    /**
     * {@inheritDoc}
     * @implNote. La implementación de este método tiene complejidad O(1).
     * @throws IllegalStateException Si el camino no está construido.
     */
    @Override
    public int hashCode() {
        if (!construido) {
            throw new IllegalStateException("No se puede obtener la clave de dispersión de un camino no construido.");
        }

        return codigoDispersion;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException Si el camino no está construido.
     */
    @Override
    public Iterator<Carretera> iterator() {
        if (!construido) {
            throw new IllegalStateException("No se puede obtener un iterador sobre un camino no construido.");
        }

        return new IteradorSoloLectura<>(carreteras.iterator());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException Si el camino no está construido.
     */
    @Override
    public Spliterator<Carretera> spliterator() {
        if (!construido) {
            throw new IllegalStateException("No se puede obtener un iterador sobre un camino no construido.");
        }

        return new SpliteratorSoloLectura<>(carreteras.spliterator());
    }

    /**
     * {@inheritDoc}
     * @implNote. La implementación de este método tiene complejidad O(1).
     */
    @Override
    public String toString() {
        String toret = "Inicializando...";

        if (construido) {
            toret = caminoComoCadena;
        }

        return toret;
    }
}