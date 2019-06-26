// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO.algoritmos;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.HashMap;
import java.util.NoSuchElementException;

import esei.si.alejandrogg.busquedaACO.Algoritmo;
import esei.si.alejandrogg.busquedaACO.Localidad;
import esei.si.alejandrogg.busquedaACO.GestorPercepciones;
import esei.si.alejandrogg.busquedaACO.GrafoCarreteras;

/**
 * Se encarga de la creación a demanda de algoritmos para un sistema multiagente de hormigas.
 * Naturalmente, es una fabricación pura.
 * @author Alejandro González García
 */
public final class FactoriaAlgoritmos {
    /**
     * El número de algoritmos disponibles en el sistema. Es recomendable incrementarlo o decrementarlo
     * según se añadan o eliminen algoritmos, aunque no necesario.
     */
    private static final short NUM_ALGORITMOS = 2;
    /**
     * Asocia un nombre de algoritmo con su constructor, para agilizar la creación repetida de algoritmos.
     */
    private static final Map<String, Constructor<? extends Algoritmo>> constructores = new HashMap<>((int) (NUM_ALGORITMOS / 0.75f) + 1);

    static {
        try {
            // Añadir algoritmos aquí, copiando y pegando la siguiente línea y modificándola donde corresponda
            constructores.put(AntCycle.NOMBRE, AntCycle.class.getDeclaredConstructor(GrafoCarreteras.class, GestorPercepciones.class, int.class, double.class, double.class, Localidad.class));
            constructores.put(BusquedaVorazEstocastica.NOMBRE, BusquedaVorazEstocastica.class.getDeclaredConstructor(GrafoCarreteras.class, GestorPercepciones.class, int.class, double.class, double.class, Localidad.class));
        } catch (Throwable t) {
            // Para este inicializador, tratar checked excepctions (que no heredan de RuntimeException) como
            // si fuesen unchecked. No tiene mucho sentido manejarlas aquí
            throw new ExceptionInInitializerError(t);
        }
    }

    /**
     * Crea el algoritmo identificado por el nombre especificado.
     * @param nombre El nombre identificativo del algoritmo.
     * @param grafoCarreteras El grafo de carreteras a asociar con el algoritmo.
     * @param gestorPercepciones El gestor de percepciones, que proporciona funcionalidades
     * para actualizar el estado del entorno en base a los resultados del algoritmo.
     * @param ciclosMaximos El número máximo de ciclos que ejecutará el algoritmo.
     * @param coeficienteRetencion El coeficiente de retención de feromona a emplear.
     * @param q Una constante positiva lo más pequeña posible, que influye en las magnitudes
     * de feromona a sumar.
     * @param localidadDestino La localidad de destino, de la que el algoritmo intentará
     * encontrar la ruta más corta hacia ella.
     * @return El algoritmo que se desea crear.
     * @throws NoSuchElementException Si el algoritmo especificado no existe, o no se pudo crear.
     */
    public static Algoritmo algoritmo(final String nombre, final GrafoCarreteras grafoCarreteras, final GestorPercepciones gestorPercepciones, final int ciclosMaximos, final double coeficienteRetencion, final double q, final Localidad localidadDestino) {
        if (!constructores.containsKey(nombre)) {
            throw new NoSuchElementException("El algoritmo \"" + nombre + "\" no está disponible en el sistema.");
        }

        try {
            return constructores.get(nombre).newInstance(grafoCarreteras, gestorPercepciones, ciclosMaximos, coeficienteRetencion, q, localidadDestino);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException exc) {
            throw new NoSuchElementException("El algoritmo \"" + nombre + "\" existe, pero no se ha podido cargar. Mensaje de error: " + exc.getLocalizedMessage());
        }
    }
}