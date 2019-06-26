// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO;

import java.util.Set;

/**
 * Modela un grafo de carreteras, que representa las interconexiones entre
 * localidades y sus distancias, sobre las que los agentes harán su cometido.
 * Las implementaciones de esta interfaz deben de garantizar un comportamiento
 * adecuado aunque sus métodos sean llamados por diferentes hilos de ejecución, lo
 * lo cual debería de ser trivial, porque los grafos de carreteras son inmutables
 * (aunque sus nodos o arcos pueden cambiar de estado).
 *
 * El grafo se tratará como no dirigido de manera implícita, y no debe de tener
 * varias aristas conectando los mismos vértices (es decir, no puede ser un multigrafo).
 * Sin embargo, puede tener bucles. Para simular varias aristas para un par de vértices,
 * puede crearse un vértice adicional que tenga distancia 0 hacia uno de los extremos
 * originales, y añadir dos arcos que conecten el nuevo vértice con los dos originales.
 *
 * @author Alejandro González García
 */
public interface GrafoCarreteras {
	/**
	 * Obtiene las carreteras (arcos) del grafo.
	 * @return Los arcos del grafo.
	 */
	public Set<Carretera> carreteras();

	/**
	 * Obtiene las localidades del grafo (nodos).
	 * @return Las localidades del grafo.
	 */
	public Set<Localidad> localidades();

    /**
     * Obtiene la carretera (arista) de este grafo que va de una localidad a otra (une ambos vértices).
     * Este método debe de tratar el grafo como no dirigido, dando el mismo resultado independientemente
     * del orden de sus argumentos.
     * @param a Una de las localidades que forma parte de la carretera a obtener.
     * @param b La otra de las localidades que forma parte de la carretera a obtener.
     * @return La devandicha carretera, si existe. Si no existe, toma el valor nulo.
     */
    public Carretera getCarretera(final Localidad a, final Localidad b);

	/**
	 * Obtiene el nombre identificativo del mapa. Para que la creación de la vista
     * asociada a este grafo tenga éxito, este nombre se debe de corresponder con el
     * de un fichero PNG en la carpeta "res", que contiene una representación gráfica
     * de este grafo.
	 * @return El nombre identificativo del grafo.
	 */
	public String getNombre();
}
