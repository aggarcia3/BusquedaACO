// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO.algoritmos;

import java.util.List;
import java.util.Map;

import esei.si.alejandrogg.busquedaACO.Camino;
import esei.si.alejandrogg.busquedaACO.Carretera;
import esei.si.alejandrogg.busquedaACO.Localidad;
import esei.si.alejandrogg.busquedaACO.GestorPercepciones;
import esei.si.alejandrogg.busquedaACO.GrafoCarreteras;

/**
 * Implementa un algoritmo para un "ant-system" que no deposita feromonas ni
 * las evapora, por lo que esencialmente proporciona un comportamiento similar
 * al de una búsqueda voraz estocástica que no tiene en cuenta experiencias anteriores.
 * Cualquier otro algoritmo que sí deposite feromona debería de converger más rápidamente
 * a las soluciones y centrar los esfuerzos de exploración en los caminos más prometedores,
 * por lo que este algoritmo sencillo proporciona una línea base y pesimista respecto a
 * la que comparar resultados.
 * @author Alejandro González García
 */
final class BusquedaVorazEstocastica extends BaseAlgoritmo {
    /**
     * El nombre identificativo de este algoritmo.
     */
    static final String NOMBRE = "Búsqueda voraz estocástica";

    /**
     * Crea un nuevo algoritmo de búsqueda voraz estocástica a partir de los parámetros
     * especificados.
     * @param grafoCarreteras El grafo de carreteras sobre el que operará este algoritmo.
     * @param gestorPercepciones El gestor de percepciones. Aunque este algoritmo en
     * particular no modificará su entorno mediante este gestor, el parámetro se recibe y
     * valida para proporiconar una interfaz común a otros algoritmos.
     * @param ciclosMaximos Los ciclos máximos a ejecutar por el algoritmo.
     * @param coeficienteRetencion Un parámetro ignorado por este algoritmo.
     * @param q Un parámetro ignorado por este algoritmo.
     * @param localidadDestino La localidad de destino, de la que el algoritmo intentará
     * encontrar el camino más corto hacia ella.
     * @throws IllegalArgumentException Si grafoCarreteras es nulo, gestorPercepciones es nulo,
     * ciclosMaximos es menor que 1 o localidadDestino es nula.
     */
    BusquedaVorazEstocastica(final GrafoCarreteras grafoCarreteras, final GestorPercepciones gestorPercepciones, final int ciclosMaximos, final double coeficienteRetencion, final double q, final Localidad localidadDestino) {
        super(grafoCarreteras, gestorPercepciones, ciclosMaximos, localidadDestino, NOMBRE);
    }

    /**
     * No ejecuta ninguna instrucción.
     * @param carretera Un parámetro ignorado en esta implementación.
     */
    @Override
    public void procesarArco(final Carretera carretera) {}

    /**
     * No ejecuta ninguna instrucción.
     * @param caminosCarretera Un parámetro ignorado en esta implementación.
     */
    @Override
    public void procesarCaminosSolucionCiclo(final Map<Carretera, List<Camino>> caminosCarretera) {}
}