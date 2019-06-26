// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO.util;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Implementa un {@link Iterator} sobre una colección que nunca soporta la eliminación
 * de elementos. Esencialmente, este Iterator es un adaptador para otro Iterator.
 * @author Alejandro González García
 */
public final class IteradorSoloLectura<E> implements Iterator<E> {
    /**
     * El Iterator del que este depende para ejecutar la lógica de implementación.
     */
    private final Iterator<E> iteradorInterno;

    /**
     * Crea una nueva instancia de esta clase, a partir de otro Iterator.
     * @param iterador El iterador a usar, para ejecutar sobre él las operaciones de
     * iteración.
     * @throws IllegalArgumentException Si el parámetro es nulo.
     */
    public IteradorSoloLectura(final Iterator<E> iterador) {
        if (iterador == null) {
            throw new IllegalArgumentException("No es posible adaptar un iterador nulo a un iterador de solo lectura.");
        }
        this.iteradorInterno = iterador;
    }

    @Override
    public void forEachRemaining(final Consumer<? super E> accion) {
        iteradorInterno.forEachRemaining(accion);
    }

    @Override
    public boolean hasNext() {
        return iteradorInterno.hasNext();
    }

    @Override
    public E next() {
        return iteradorInterno.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("El iterador sobre esta colección es de solo lectura.");
    }
}