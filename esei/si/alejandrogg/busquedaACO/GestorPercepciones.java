// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.text.Normalizer;
import java.util.concurrent.atomic.LongAdder;

import jason.asSyntax.ASSyntax;
import jason.asSyntax.Atom;
import jason.asSyntax.Literal;

/**
 * Esta clase es una fabricación pura, cuyas responsabilidades versan sobre el mantenimiento
 * de las percepciones de los agentes en coordinación con el entorno, {@link Mundo}.
 *
 * Esta clase no debe de invocar métodos en su instancia de {@link Mundo} asociada que puedan
 * requerir de información no inicializada. Para más información acerca de qué está inicializado
 * y qué no cuando se crea este gestor, véase la implementación de {@link Mundo}.
 *
 * @see Mundo
 * @author Alejandro González García
 */
public final class GestorPercepciones {
    /**
     * El autómata que reconoce la expresión regular que siguen los caracteres a sustituir
     * por otro caracter.
     */
    private static final Pattern regexSustituciones = Pattern.compile("^[0-9]|\\s");
    /**
     * El autómata que reconoce la expresión regular de los símbolos que no forman parte de
     * un átomo válido.
     */
    private static final Pattern regexCaracteresProhibidos = Pattern.compile("[^a-zA-Z0-9_]");
    /**
     * El literal que unifica con el usado para notificar a los agentes del paso del tiempo en el sistema.
     */
    private static final Literal literalPasoTiempoUnif = ASSyntax.createLiteral("pasoTiempo", ASSyntax.createVar());
    /**
     * El literal que unifica con el usado para notificar a los agentes de la llegada de un nuevo ciclo.
     */
    private static final Literal literalSiguienteCicloUnif = ASSyntax.createLiteral("siguienteCiclo", ASSyntax.createVar());

    /**
     * La instancia del entorno asociada con este gestor.
     */
    private final Mundo mundo;
    /**
     * Los átomos que representan en AgentSpeak de manera inequívoca a cada localidad.
     * Es seguro que varios hilos operen sobre él de manera concurrente.
     */
    private final Map<Localidad, Atom> atomosLocalidades;
    /**
     * Las localidades representadas por átomos en AgentSpeak, de manera inequívoca.
     * Es seguro que varios hilos operen sobre él de manera concurrente.
     */
    private final Map<String, Localidad> localidadesAtomos;
    /**
     * El objeto cuyo monitor se usará para realizar exclusión mutua en las actualizaciones
     * de percepciones de feromonas (carreteras), para garantizar que los agentes siempre
     * ven un entorno consistente.
     */
    private final Object candadoActualizacionFeromona = new Object();
    /**
     * Se utiliza para asignar a las percepciones que sean necesarias un identificador lo más unívoco
     * posible. Esto sirve para que Jason pueda diferenciar diferentes percepciones de paso de tiempo,
     * y los agentes no vuelvan a reincorporar esa percepción tras borrarla de su BC, por alguna razón
     * que, para variar, no está documentada explícitamente.
     */
    private LongAdder idPercepcion = new LongAdder();

    /**
     * Crea un nuevo gestor de percepciones, asociado con un entorno (o mundo) dado, al
     * que le añadirá las percepciones iniciales relacionadas con el grafo de carreteras
     * en uso.
     * @param mundo El mundo a asociar.
     * @throws IllegalArgumentException Si el mundo a asociar es nulo.
     */
    GestorPercepciones(final Mundo mundo) {
        if (mundo == null) {
            throw new IllegalArgumentException("Un gestor de percepciones no puede tener un mundo asociado nulo.");
        }
        this.mundo = mundo;

        final Set<Localidad> localidades = mundo.getGrafoCarreteras().localidades();
        final Set<String> funtoresUsados = new HashSet<>((int) (localidades.size() / 0.75) + 1, 0.75f);

        // De paso que guardamos los átomos asociados a cada localidad en memoria, para no tener que computarlos
        // frecuentemente, también comprobamos que los funtores asociados sean unívocos, para garantizar el
        // correcto funcionamiento del sistema. Y, naturalmente, también añadimos percepciones para el mapeo
        // átomo-nombre
        final Map<Localidad, Atom> atomosLocalidades = new HashMap<>((int) (localidades.size() / 0.75) + 1, 0.75f);
        final Map<String, Localidad> localidadesAtomos = new HashMap<>((int) (localidades.size() / 0.75) + 1, 0.75f);
        for (final Localidad l : localidades) {
            final Atom at = calcularAtomoLocalidad(l);
            final String func = at.getFunctor();

            if (funtoresUsados.contains(func)) {
                throw new IllegalArgumentException("No se pudo generar un átomo unívoco para identificar la localidad " + l.getNombre());
            }

            mundo.addPercept(
                ASSyntax.createLiteral("localidadANombre",
                    at, ASSyntax.createString(l.getNombre())
                )
            );

            atomosLocalidades.put(l, at);
            localidadesAtomos.put(func, l);
            funtoresUsados.add(func);
        }
        // Como no necesitaremos actualizar los contenidos de los mapas, hacerlos de solo
        // lectura es una manera de bajo coste de garantizar la seguridad con varios hilos
        // de ejecución, aún bajo condiciones imprevistas
        this.atomosLocalidades = Collections.unmodifiableMap(atomosLocalidades);
        this.localidadesAtomos = Collections.unmodifiableMap(localidadesAtomos);

        // Crear percepciones del tipo:
        // carretera(A, B, Distancia, IntFeromona)
        for (final Carretera c : mundo.getGrafoCarreteras().carreteras()) {
            final Localidad[] nodosCarretera = c.getLocalidades();
            mundo.addPercept(
                ASSyntax.createLiteral("carretera",
                    atomosLocalidades.get(nodosCarretera[0]), atomosLocalidades.get(nodosCarretera[1]),
                    ASSyntax.createNumber(c.getDistancia()), ASSyntax.createNumber(Mundo.FEROMONA_INICIAL)
                )
            );
        }

        // Crear percepciones del tipo:
        // localidadInicio(localidad)
        // localidadDestino(localidad)
        mundo.addPercept(
            ASSyntax.createLiteral("localidadInicio",
                atomosLocalidades.get(mundo.getLocalidadInicio())
            )
        );
        mundo.addPercept(
            ASSyntax.createLiteral("localidadDestino",
                atomosLocalidades.get(mundo.getLocalidadDestino())
            )
        );

        // Crear percepciones del tipo:
        // importanciaFeromona(Alfa)
        // importanciaDistancia(Beta)
        mundo.addPercept(
            ASSyntax.createLiteral("importanciaFeromona",
                ASSyntax.createNumber(mundo.getAlfa())
            )
        );
        mundo.addPercept(
            ASSyntax.createLiteral("importanciaDistancia",
                ASSyntax.createNumber(mundo.getBeta())
            )
        );

        // Aunque no están documentadas las implicaciones de llamar a este método
        // de Jason por ninguna parte, un examen del código fuente indica que invoca
        // el método wakeUpSense() de los agentes en la arquitectura centralizada,
        // que tiene algo que ver con el control de la ejecución de los ciclos de percepción.
        // Así pues, invocar este método supongo que puede interpretarse como una indicación
        // a los agentes de que deberían de refrescar sus percepciones del entorno más pronto
        // que tarde (algo que tiene sentido por su nombre, pero quería asegurarme), aunque
        // normalmente los agentes ya ejecutan esos ciclos por sí solos
        mundo.informAgsEnvironmentChanged();
    }

    /**
     * Reenvía a todos los agentes la percepción de paso de tiempo al siguiente
     * instante de tiempo discreto. Este método no está diseñado para ser ejecutado
     * por varios hilos de ejecución en paralelo.
     */
    void percibirPasoTiempo() {
        mundo.removePerceptsByUnif(literalPasoTiempoUnif);
        mundo.addPercept(ASSyntax.createLiteral("pasoTiempo", ASSyntax.createNumber(idPercepcion.intValue())));

        mundo.informAgsEnvironmentChanged();

        idPercepcion.increment(); // Este incremento no es atómico respecto a la consulta del valor, pero no nos importa
    }

    /**
     * Informa a todos los agentes del nuevo nivel de feromona que hay en una carretera determinada.
     * Este método es seguro para ser ejecutado por varios hilos concurrentemente.
     * @param carretera La carretera cuyo nivel de feromona ha variado.
     * @param nuevoNivel El nuevo nivel de feromona de la carretera. El paso por parámetro de este
     * valor implica evitar tener que volver a consultárselo a la carretera, y ayuda a reducir contención
     * de hilos.
     */
    void actualizarNivelFeromona(final Carretera carretera, final double nuevoNivel) {
        final Localidad[] nodosCarretera = carretera.getLocalidades();
        final double distancia = carretera.getDistancia();

        mundo.removePerceptsByUnif(
            ASSyntax.createLiteral("carretera",
                atomosLocalidades.get(nodosCarretera[0]), atomosLocalidades.get(nodosCarretera[1]),
                ASSyntax.createNumber(distancia), ASSyntax.createVar()
            )
        );
        mundo.addPercept(
            ASSyntax.createLiteral("carretera",
                atomosLocalidades.get(nodosCarretera[0]), atomosLocalidades.get(nodosCarretera[1]),
                ASSyntax.createNumber(distancia), ASSyntax.createNumber(nuevoNivel)
            )
        );

        mundo.informAgsEnvironmentChanged();
    }

    /**
     * Envía a todos los agentes la percepción de que ha llegado el siguiente ciclo. Este método no está
     * diseñado para ser ejecutado por varios hilos de ejecución en paralelo.
     */
    void siguienteCiclo() {
        mundo.removePerceptsByUnif(literalSiguienteCicloUnif);
        mundo.addPercept(ASSyntax.createLiteral("siguienteCiclo", ASSyntax.createNumber(idPercepcion.intValue())));

        mundo.informAgsEnvironmentChanged();

        idPercepcion.increment(); // Este incremento no es atómico respecto a la consulta del valor, pero no nos importa
    }

    /**
     * Obtiene la localidad correspondiente al átomo de AgentSpeak dado.
     * @param atomo El átomo cuya localidad asociada obtener.
     * @return La localidad identificada por el átomo. De no corresponderse
     * el átomo con ninguna localidad, el valor de retorno puede ser nulo.
     */
    public Localidad localidadAtomo(final Atom atomo) {
        return localidadesAtomos.get(atomo == null ? null : atomo.getFunctor());
    }

    /**
     * Obtiene el átomo identificativo de una localidad, usado en las percepciones asociadas.
     * Esta operación normaliza el nombre de la localidad de manera que pueda ser interpretado
     * de manera segura como un átomo. Este método no garantiza que el resultado sea unívoco; es
     * decir, que para dos localidades diferentes el resultado siempre sea diferente.
     * @param l La localidad de la que obtener su átomo.
     * @return El devandicho átomo.
     */
    private static Atom calcularAtomoLocalidad(final Localidad l) {
        // Descomponer caracteres Unicode compuestos en secuencias de combinación,
        // eliminando distinciones semánticas inútiles para comparaciones binarias
        // (esta operación separa, por ejemplo, las tildes de las vocales)
        String nombreAtomo = Normalizer.normalize(l.getNombre(), Normalizer.Form.NFKD);

        // Sustituir espacios en blanco y dígito inicial por guiones bajos
        nombreAtomo = regexSustituciones.matcher(nombreAtomo).replaceAll("_");
        // Eliminar caracteres Unicode "extraños"
        nombreAtomo = regexCaracteresProhibidos.matcher(nombreAtomo).replaceAll("");

        // Finalmente, convertir a minúsculas
        nombreAtomo = nombreAtomo.toLowerCase();

        return ASSyntax.createAtom(nombreAtomo);
    }
}