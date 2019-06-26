// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO.util;

import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Implementa un {@link Spliterator} inmutable y de tamaño definido sobre otro Spliterator,
 * del que hereda otras características que tenga. Es, por tanto, un adaptador.
 * @author Alejandro González García
 */
public final class SpliteratorSoloLectura<E> implements Spliterator<E> {
    /**
     * Las características que este Spliterator aporta al Spliterator usado internamente.
     */
    private static final int CARACTERISTICAS = Spliterator.IMMUTABLE | Spliterator.SIZED | Spliterator.SUBSIZED;

    /**
     * El Spliterator del que este depende para ejecutar la lógica de implementación.
     */
    private final Spliterator<E> spliteratorInterno;

    /**
     * Crea una nueva instancia de esta clase, cuyo Spliterator interno es el especificado.
     * @param spliteratorInterno El Spliterator interno deseado.
     * @throws IllegalArgumentException Si el Spliterator interno es nulo.
     */
    public SpliteratorSoloLectura(final Spliterator<E> spliteratorInterno) {
        if (spliteratorInterno == null) {
            throw new IllegalArgumentException("No se puede crear un Spliterator de solo lectura usando un Spliterator interno nulo.");
        }
        this.spliteratorInterno = spliteratorInterno;
    }

    @Override
    public boolean tryAdvance(final Consumer<? super E> accion) {
        return spliteratorInterno.tryAdvance(accion);
    }

    @Override
    public void forEachRemaining(final Consumer<? super E> accion) {
        spliteratorInterno.forEachRemaining(accion);
    }

    @Override
    public Spliterator<E> trySplit() {
        try {
            return new SpliteratorSoloLectura<>(spliteratorInterno.trySplit());
        } catch (IllegalArgumentException exc) {
            return null;
        }
    }

    @Override
    public long estimateSize() {
        return spliteratorInterno.estimateSize();
    }

    @Override
    public int characteristics() {
        return (spliteratorInterno.characteristics() | CARACTERISTICAS) & ~Spliterator.CONCURRENT;
    }
}