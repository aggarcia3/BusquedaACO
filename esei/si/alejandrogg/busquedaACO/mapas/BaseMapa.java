// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO.mapas;

import java.util.HashMap;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;

import esei.si.alejandrogg.busquedaACO.GrafoCarreteras;
import esei.si.alejandrogg.busquedaACO.Carretera;
import esei.si.alejandrogg.busquedaACO.Localidad;

/**
 * Implementa lógica de uso muy común por parte de grafos de carreteras.
 * @author Alejandro González García
 */
abstract class BaseMapa implements GrafoCarreteras {
    /**
     * Relaciona pares de localidades con la carretera que las conecta en un grafo de carreteras.
     * Es responsabilidad de la subclase la inserción de valores en este mapa, aunque la superclase
     * se encarga de crearlo en su constructor. La subclase debe de insertar en este mapa dos
     * entradas por cada carretera, cubriendo ambas permutaciones de localidades que la componen.
     */
    protected final Map<SimpleEntry<Localidad, Localidad>, Carretera> carreterasLocalidades;

    /**
     * Crea un nuevo grafo de carreteras, con un número determinado de carreteras.
     * Las implementaciones de esta clase deben de llamar a este constructor en el suyo, y acto
     * seguido rellenar los atributos protegidos de esta clase como corresponda.
     * @param numeroCarreteras El número de carreteras presentes en el grafo de carreteras.
     */
    protected BaseMapa(final int numeroCarreteras) {
        this.carreterasLocalidades = new HashMap<>((int) ((numeroCarreteras * 2) / 0.75f) + 1, 0.75f);
    }

    @Override
    public Carretera getCarretera(final Localidad a, final Localidad b) {
        return carreterasLocalidades.get(new SimpleEntry<>(a, b));
    }
}