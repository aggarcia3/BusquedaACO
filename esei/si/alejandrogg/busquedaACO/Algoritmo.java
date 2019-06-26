// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO;

import java.util.List;
import java.util.NoSuchElementException;

import jason.asSyntax.Term;

import esei.si.alejandrogg.busquedaACO.algoritmos.RazonTerminacion;

/**
 * Define el contrato que debe de cumplir cualquier algoritmo de un SMA de búsqueda
 * mediante ACO. En general, la implementación de un algoritmo es la encargada de dictaminar
 * cuándo y cómo se actualizan las feromonas de las carreteras, en respuesta a las acciones
 * realizadas por las hormigas. También mantiene el control sobre la terminación y las
 * iteraciones del algoritmo.
 *
 * Las implementaciones de esta interfaz deben de garantizar la
 * corrección de su implementación aún bajo diferentes hilos de ejecución.
 *
 * @author Alejandro González García
 */
public interface Algoritmo {
    /**
     * Informa al algoritmo de que una hormiga acaba de recorrer un arco. La respuesta a esta
     * acción depende del algoritmo, pero normalmente implica actualizar la cantidad de feromona
     * depositada en la carretera correspondiente. En general, debe de asumirse que este método puede
     * realizar cualquier acción en su implementación.
     * @param hormiga La hormiga que ha recorrido un arco (carretera). Las implementaciones pueden
     * asumir que este parámetro no es nulo.
     * @param carretera La carretera que ha tomado la hormiga. Las implementaciones pueden asumir que
     * este parámetro no es nulo.
     */
    public void recorrerArco(final String hormiga, final Carretera carretera);

    /**
     * Notifica al algoritmo de que una hormiga ha terminado de realizar un camino, bien porque
     * ha llegado al destino, o bien porque no puede seguir avanzando. La respuesta a esta acción
     * depende del algoritmo. En general, debe de asumirse que este método puede realizar cualquier
     * acción en su implementación.
     * @param hormiga La hormiga que acaba de terminar un camino. Las implementaciones pueden
     * asumir que este parámetro no es nulo.
     * @param camino El camino realizado por la hormiga, en forma de lista de ciudades (nodos) por
     * las que pasó. Las implementaciones pueden asumir que este parámetro no es nulo, pero no que
     * los términos contenidos en la lista sean válidos. Tampoco pueden asumir que el acceso
     * aleatorio a elementos de la lista sea de complejidad constante.
     * @throws IllegalArgumentException Si algún término contenido en la lista no es un átomo que
     * represente una localidad.
     */
    public void notificarCaminoEncontrado(final String hormiga, final List<Term> camino);

    /**
     * Informa al algoritmo de que una hormiga ha dejado de formar parte del sistema o, más generalmente,
     * que no debe de ser tenida en cuenta a efectos del algoritmo. Las implementaciones son libres
     * de utilizar esta información para lo que deseen, o de incluso ignorarla.
     * @param hormiga La hormiga que ha dejado de formar parte del sistema.
     */
    public void notificarMuerteHormiga(final String hormiga);

    /**
     * Avanza a la siguiente iteración del SMA. Este método debe de ser invocado en cuanto se hayan
     * recibido las respuestas de todas las hormigas, y en consecuencia invocado otros métodos del
     * objeto algoritmo según correspondiese. Como parte del avance de la iteración, la implementación
     * puede usar información deducida a partir de llamadas a otros métodos para cambiar su estado interno,
     * recalculando, por ejemplo, si ha terminado o no, si hay que empezar un nuevo ciclo, etc.
     * @return Verdadero si y solo si, aparte de avanzarse a la siguiente iteración, también se avanzó al
     * siguiente ciclo.
     */
    public boolean avanzarIteracion();

    /**
     * Comprueba si el algoritmo ha terminado de ejecutarse, obteniendo la razón por la que ello ocurrió.
     * Puede deberse a cualquiera de las razones representadas por {@link RazonTerminacion}.
     * @return La razón de terminación del algoritmo, si ha terminado, o nulo en caso de que todavía no haya terminado.
     */
    public RazonTerminacion razonTerminacion();

    /**
     * Obtiene el número de ciclo en el que se encuentra actualmente la ejecución del algoritmo. El primer ciclo es
     * el 1.
     * @return El devandicho número.
     */
    public int getCiclo();

    /**
     * Obtiene el número máximo de ciclos que el algoritmo ejecutará.
     * @return El devandicho número.
     */
    public int getCiclosMaximos();

    /**
     * Obtiene el mejor camino hacia el destino encontrado hasta el momento por el algoritmo.
     * @return El devandicho camino.
     * @throws NoSuchElementException Si el algoritmo todavía no conoce al menos un camino.
     */
    public Camino getMejorCamino();

    /**
     * Obtiene el nombre identificativo de este algoritmo, que se mostrará al usuario, y se usará para
     * identificarlo en los parámetros de configuración del SMA.
     * @return El devandicho nombre.
     */
    public String getNombre();
}