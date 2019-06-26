package esei.si.alejandrogg.busquedaACO;

import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import jason.asSyntax.Atom;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;

/**
 * Clase, creada de acuerdo con el patrón de fabricación pura, para encargarse de
 * responsabilidades relacionadas con la atención de las acciones realizadas
 * sobre los agentes en el entorno.
 * @author Alejandro González García
 */
final class OperadorAccionesEntorno {
    /**
     * El número de acciones del entorno actualmente implementadas. Se utiliza para
     * inicializar estructuras de datos con el tamaño óptimo.
     */
    private static final int NUM_ACCIONES_ENTORNO = 3;

    /**
     * El mundo (entorno) sobre el que operador de acciones de entorno realizará
     * las acciones de entorno.
     */
    private final Mundo mundo;
    /**
     * Relaciona cada acción de entorno, identificada por su funtor, con su acción correspondiente.
     */
    private final Map<String, Consumer<ContextoAccion>> accionesEntorno;

    /**
     * Contiene la información asociada al contexto de ejecución de una acción del entorno:
     * el agente que la realiza, y la propia acción a realizar, expresada como una estructura.
     * @author Alejandro González García
     */
    private static final class ContextoAccion {
        /**
         * El agente responsable de ejecutar la acción.
         */
        private final String hormiga;
        /**
         * La acción a ejecutar por el agente.
         */
        private final Structure accion;

        /**
         * Crea un nuevo contexto de ejecución de una acción del entorno.
         * @param hormiga El agente que ejecuta la acción.
         * @param accion La acción a ejecutar por el agente.
         * @throws IllegalArgumentException Si algún parámetro es nulo.
         */
        ContextoAccion(final String hormiga, final Structure accion) {
            if (hormiga == null) {
                throw new IllegalArgumentException("No se puede crear un contexto de ejecución de una acción asociado a una hormiga nula.");
            }
            if (accion == null) {
                throw new IllegalArgumentException("No se puede crear un contexto de ejecución de una acción asociado a una acción nula.");
            }
            this.hormiga = hormiga;
            this.accion = accion;
        }

        /**
         * Obtiene el agente responsable de ejecutar la acción asociada a este objeto.
         * @return El devandicho agente.
         */
        String getHormiga() {
            return hormiga;
        }

        /**
         * Obtiene la acción que el agente ejecutará.
         * @return La devandicha acción.
         */
        Structure getAccion() {
            return accion;
        }
    }

    /**
     * Crea un nuevo operador de acciones de entorno, asociado a un mundo dado.
     * @param mundo El mundo a asociar al operador.
     * @throws IllegalArgumentException Si el mundo es nulo.
     */
    OperadorAccionesEntorno(final Mundo mundo) {
        if (mundo == null) {
            throw new IllegalArgumentException("No se puede crear un operador de acciones de entorno asociado a un mundo nulo.");
        }
        this.mundo = mundo;
        
        final Map<String, Consumer<ContextoAccion>> accionesEntorno = new HashMap<>((int) (NUM_ACCIONES_ENTORNO / 0.75) + 1, 0.75f);

        // Código para atender la acción irA/1
        accionesEntorno.put("irA", (final ContextoAccion contexto) -> {
            final String hormiga = contexto.getHormiga();
            final Structure accion = contexto.getAccion();
            final Term parametro = accion.getTerm(0);

            // Comprobar que los argumentos sean válidos en número y tipo
            if (accion.getArity() == 1 && parametro instanceof Atom) {
                final Localidad localidad = mundo.getGestorPercepciones().localidadAtomo((Atom) parametro);
                if (localidad != null) {
                    final LocalizadorHormigas lh = mundo.getLocalizadorHormigas();
                    final Carretera carretera = mundo.getGrafoCarreteras().getCarretera(lh.ubicacion(hormiga), localidad);
                    if (carretera != null) {
                        // Actualizar la feromona depositada en el arco, si procede
                        mundo.getAlgoritmo().recorrerArco(hormiga, carretera);

                        // Registrar el movimiento
                        lh.mover(hormiga, localidad);
                    } else {
                        throw new IllegalArgumentException("La hormiga " + hormiga + " ha intentado hacer un movimiento no permitido, dada su ubicación actual, hacia " + localidad.getNombre() + ".");
                    }
                } else {
                    throw new IllegalArgumentException("La hormiga " + hormiga + " se ha intentado mover a una localidad no existente: " + ((Atom) parametro).getFunctor());
                }
            } else {
                throw new IllegalArgumentException("La hormiga " + hormiga + " no ha pasado un parámetro para la acción interna \"irA\", o el parámetro que ha pasado no es un átomo.");
            }
        });

        // Código para atender la acción irAHormiguero/1
        accionesEntorno.put("irAHormiguero", (final ContextoAccion contexto) -> {
            final String hormiga = contexto.getHormiga();
            final Structure accion = contexto.getAccion();
            final Term parametro = accion.getTerm(0);

            // Comprobar que los argumentos sean válidos en número y tipo
            if (accion.getArity() == 1 && parametro instanceof ListTerm) {
                // Notificar al algoritmo de que hemos completado un camino, para que haga lo pertinente
                // Por definición, ListTerm implementa List<Term>, así que es una conversión segura (al
                // menos en las versiones actuales de Jason) y la advertencia no aporta nada
                @SuppressWarnings("unchecked")
                final List<Term> listaParametro = (List<Term>) parametro;
                mundo.getAlgoritmo().notificarCaminoEncontrado(hormiga, listaParametro);

                // Al volver al hormiguero, volvemos a la localidad de inicio
                mundo.getLocalizadorHormigas().mover(hormiga, mundo.getLocalidadInicio());
            } else {
                throw new IllegalArgumentException("La hormiga " + hormiga + " no ha pasado un parámetro para la acción interna \"irAHormiguero\", o el parámetro que ha pasado no es una lista.");
            }
        });

        // Código para atender la acción listaParaContinuar/0
        accionesEntorno.put("listaParaContinuar", (final ContextoAccion contexto) -> {
            // Comprobar que los argumentos sean válidos en número
            if (contexto.getAccion().getArity() != 0) {
                throw new IllegalArgumentException("La hormiga " + contexto.getHormiga() + " ha pasado parámetros a la acción interna \"listaParaContinuar\", cuando no se esperaban.");
            }
        });

        // Hacer que el mapa sea no modificable garantiza de manera poco costosa su seguridad con varios hilos de ejecución,
        // aún bajo condiciones de ejecución imprevistas
        this.accionesEntorno = Collections.unmodifiableMap(accionesEntorno);
    }

    /**
     * Comprueba que el agente especificado puede ejecutar la acción de entorno dada,
     * definida por su estructura. Este método asume que los agentes no pueden realizar
     * acciones antes o durante su inicialización (ejecución del método init() de su
     * arquitectura), y no comprueba si los parámetros de la acción a ejecutar son correctos.
     * @param agente El agente que pretende ejecutar la acción de entorno dada.
     * @param accion La acción a comprobar si es válida para el agente.
     * @return Verdadero si y solo si la acción es válida para ser ejecutada por el agente,
     * falso en caso contrario.
     */
    boolean esAccionValida(final String agente, final Structure accion) {
        boolean toret = agente != null && accion != null;

        if (toret) {
            toret = mundo.getLocalizadorHormigas().ubicacion(agente) != null && accionesEntorno.containsKey(accion.getFunctor());
        }

        return toret;
    }

    /**
     * Ejecuta la acción de entorno especificada en el hilo actual. Este método no comprueba que
     * esta acción sea válida para este agente, lo que se debe de hacer previamente empleando el método
     * {@link esAccionValida}.
     * @param agente El agente responsable de la ejecución de la acción de entorno.
     * @param accion La estructura que define la acción. Contiene sus parámetros.
     * @throws IllegalArgumentException Si se intenta ejecutar una acción de entorno nula o no implementada,
     * o bajo la responsabilidad de un agente nulo.
     */
    void ejecutarAccion(final String agente, final Structure accion) {
        final String functor = accion == null ? null : accion.getFunctor();
        final Consumer<ContextoAccion> codigoAccion = accionesEntorno.get(functor);

        if (codigoAccion == null) {
            throw new IllegalArgumentException((agente == null ? "Una hormiga" : agente) + " ha intentado ejecutar una acción de entorno no implementada: " + (functor == null ? "(nula)" : functor));
        }

        codigoAccion.accept(new ContextoAccion(agente, accion));
    }
}