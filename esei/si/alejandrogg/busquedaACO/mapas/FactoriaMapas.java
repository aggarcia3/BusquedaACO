// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO.mapas;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.HashMap;
import java.util.NoSuchElementException;

import esei.si.alejandrogg.busquedaACO.GrafoCarreteras;

/**
 * Se encarga de la creación a demanda de mapas del mundo, que son grafos de carreteras
 * con datos asociados.
 * @author Alejandro González García
 */
public final class FactoriaMapas {
    /**
     * El número de mapas disponibles en el sistema. Es recomendable incrementarlo o decrementarlo
     * según se añadan o eliminen mapas, aunque no necesario.
     */
    private static final short NUM_MAPAS = 1;
    /**
     * Asocia un nombre de mapa con su constructor, para agilizar la creación repetida de mapas.
     */
    private static final Map<String, Constructor<? extends GrafoCarreteras>> constructores = new HashMap<>((int) (NUM_MAPAS / 0.75f) + 1);

    static {
        try {
            // Añadir mapas aquí, copiando y pegando la siguiente línea y modificándola donde corresponda
            constructores.put(Rumania.NOMBRE, Rumania.class.getDeclaredConstructor());
        } catch (Throwable t) {
            // Para este inicializador, tratar checked excepctions (que no heredan de RuntimeException) como
            // si fuesen unchecked. No tiene mucho sentido manejarlas aquí
            throw new ExceptionInInitializerError(t);
        }
    }

    /**
     * Crea el grafo de carreteras identificado por el nombre especificado.
     * @param nombre El nombre identificativo del grafo.
     * @return El grafo de carreteras que se desea crear.
     * @throws NoSuchElementException Si el grafo de carreteras especificado no existe, o no se pudo crear.
     */
    public static GrafoCarreteras grafoCarreteras(final String nombre) {
        if (!constructores.containsKey(nombre)) {
            throw new NoSuchElementException("El mapa " + nombre + " no está disponible en el sistema.");
        }

        try {
            return constructores.get(nombre).newInstance();
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException exc) {
            throw new NoSuchElementException("El mapa " + nombre + " existe, pero no se ha podido cargar. Mensaje de error: " + exc.getLocalizedMessage());
        }
    }
}