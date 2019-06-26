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
 * Implementa un algoritmo para un "ant-system" que deposita feromonas
 * solo cuando todas las hormigas involucradas han completado un camino
 * (es decir, se avanza al siguiente ciclo). Este algoritmo está descrito en
 * el artículo de Dorigo et al. (1996), "The Ant System: Optimization by a colony
 * of cooperating agents". La implementación aquí hecha está paralelizada.
 * @author Alejandro González García
 */
final class AntCycle extends BaseAlgoritmo {
    /**
     * El nombre identificativo de este algoritmo.
     */
    static final String NOMBRE = "Dorigo et al. (1996), ant-cycle";

    /**
     * El coeficiente de retención de feromona.
     */
    private final double coeficienteRetencion;
    /**
     * Una constante positiva lo más pequeña posible, que indica la magnitud de feromona a sumar
     * en cada carretera por cada hormiga. Su valor debe ajustarse teniendo en cuenta la longitud
     * máxima estimada de un camino solución, el coeficiente de retención, el valor subjetivo de
     * importancia de feromona y el creciente error de precisión en el que incurre la representación
     * de coma flotante IEEE 754 de 64 bits según las magnitudes de feromona se alejan del cero.
     */
    private final double q;

    /**
     * Crea un nuevo algoritmo "ant-cycle" a partir de los parámetros especificados.
     * @param grafoCarreteras El grafo de carreteras sobre el que operará este algoritmo.
     * @param gestorPercepciones El gestor de percepciones, que proporciona funcionalidades
     * para actualizar el estado del entorno en base a los resultados del algoritmo.
     * @param ciclosMaximos Los ciclos máximos a ejecutar por el algoritmo.
     * @param coeficienteRetencion El coeficiente de retención de feromona a emplear.
     * @param q Una constante positiva lo más pequeña posible, que influye en las magnitudes
     * de feromona a sumar.
     * @param localidadDestino La localidad de destino, de la que el algoritmo intentará
     * encontrar el camino más corto hacia ella.
     * @throws IllegalArgumentException Si grafoCarreteras es nulo, gestorPercepciones es nulo,
     * ciclosMaximos es menor que 1, el coeficiente de retención no está en el intervalo [0, 1],
     * q es menor o igual a 0 o localidadDestino es nula.
     */
    AntCycle(final GrafoCarreteras grafoCarreteras, final GestorPercepciones gestorPercepciones, final int ciclosMaximos, final double coeficienteRetencion, final double q, final Localidad localidadDestino) {
        super(grafoCarreteras, gestorPercepciones, ciclosMaximos, localidadDestino, NOMBRE);

        if (coeficienteRetencion < 0 || coeficienteRetencion > 1) {
            throw new IllegalArgumentException("El coeficiente de retención de feromona a emplear no está en el intervalo [0, 1].");
        }
        if (q <= 0) {
            throw new IllegalArgumentException("Q no puede tener un valor menor o igual a 0.");
        }

        this.coeficienteRetencion = coeficienteRetencion;
        this.q = q;
    }

    /**
     * No ejecuta ninguna instrucción.
     * @param carretera Un parámetro ignorado en esta implementación.
     */
    @Override
    public void procesarArco(final Carretera carretera) {}

    /**
     * Evapora las feromonas presentes en todas las carreteras, de manera que el nuevo nivel de feromona de cada carretera
     * sea t = ρ · t, donde ρ ∈ [0, 1] es el coeficiente de retención del algoritmo, y a continuación simula el depósito de feromonas
     * de las hormigas en las carreteras por las que pasaron, de manera que el nuevo valor de t de tales carreteras sea
     * t = t + Δt, siendo Δt = Q / L, donde Q es una constante real positiva pequeña y L la distancia del camino solución recorrido
     * por cada hormiga.
     * @param caminosCarretera El mapa que relaciona cada carretera con los caminos solución que pasan por ella.
     */
    @Override
    public void procesarCaminosSolucionCiclo(final Map<Carretera, List<Camino>> caminosCarretera) {
        // TODO: paralelizar con Spliterators
        for (final Carretera carretera : grafoCarreteras.carreteras()) {
            // Evaporar feromona, teniendo en cuenta el coeficiente de retención
            carretera.actualizarNivelFeromona(gestorPercepciones, (final Double t) -> {
                return coeficienteRetencion * t;
            });

            // Para los caminos que pasan por aquí, depositar feromonas de acuerdo a sus características
            final List<Camino> caminos = caminosCarretera.get(carretera);
            if (caminos != null) {
                for (final Camino camino : caminos) {
                    carretera.actualizarNivelFeromona(gestorPercepciones, (final Double t) -> {
                        final double deltaT = q / camino.distanciaTotal();
                        return t + deltaT;
                    });
                }
            }
        }
    }
}