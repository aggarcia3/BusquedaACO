// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO;

import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Phaser;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.NoSuchElementException;

import static java.lang.Thread.currentThread;

import jason.asSyntax.Structure;
import jason.environment.Environment;
import jason.runtime.RuntimeServices;

import esei.si.alejandrogg.busquedaACO.mapas.FactoriaMapas;
import esei.si.alejandrogg.busquedaACO.algoritmos.FactoriaAlgoritmos;
import esei.si.alejandrogg.busquedaACO.algoritmos.RazonTerminacion;

/**
 * Clase principal del entorno del proyecto de Jason. Actúa como controlador del
 * entorno, y es responsable de cargar los parámetros de configuración iniciales
 * del SMA.
 * @see CoordinadorHormigas
 * @see LocalizadorHormigas
 * @see OperadorAccionesEntorno
 * @see GestorPercepciones
 * @see Algoritmo
 * @see GrafoCarreteras
 * @author Alejandro González García
 */
public final class Mundo extends Environment {
    /**
     * La cantidad de feromona que hay inicialmente en todas las carreteras del mundo.
     */
    static final double FEROMONA_INICIAL = Double.MIN_VALUE;

	/**
	 * Una referencia a la única instancia de esta clase en la ejecución actual de la JVM.
     * Se asume que la arquitectura de Jason es la única que usa el constructor implícito
     * de esta clase, y que lo hace una sola vez.
	 */
	private static Mundo instancia = null;
    /**
     * El candado que usaremos para crear una condición de espera a que la inicialización del mundo
     * se complete. En una arquitectura centralizada normal de Jason se ha visto que, por lo menos en
     * el PC del desarrollador, esto no parece ser necesario para garantizar una ejecución correcta
     * del SMA, pues un correcto orden de instrucciones en init() es suficiente, y los agentes no se
     * empiezan a crear hasta que init() finalice. Sin embargo, JADE parece crear el entorno concurrentemente
     * con los agentes, y de no haber este candado cabe la posibilidad de que, dependiendo de cómo se
     * intercale la ejecución de instrucciones de diferentes hilos, la ejecución del SMA sea inestable
     * (NullPointerException durante el inicio de los agentes, debido a que no ven los campos de este objeto inicializados).
     */
    private static final Lock candadoInicializacion = new ReentrantLock();
    /**
     * La condición de espera del candado anterior, {@link candadoInicializacion}.
     */
    private static final Condition mundoInicializado = candadoInicializacion.newCondition();

	/**
	 * Objeto con servicios útiles para controlar el SMA.
	 */
	private volatile RuntimeServices rs = null;
    /**
     * El algoritmo en uso por el SMA actual, que influye en cómo y cuándo se actualiza
     * la feromona depositada por las hormigas en las carreteras, entre otras responsabilidades.
     */
    private volatile Algoritmo algoritmo = null;
    /**
     * El Phaser de Java que permite esperar a que todas las hormigas ejecuten sus acciones
     * de entorno programadas.
     */
    private final Phaser ejecucionAcciones = new Phaser(1);
    /**
     * Las acciones que han registrado su intención de ejecutarse, y todavía no lo hicieron. En caso de que las hormigas
     * envíen acciones tras notificar la terminación de sus respuestas, y antes del instante del tiempo siguiente,
     * éstas se ejecutarán en el instante de tiempo siguiente.
     */
    private final Queue<AccionProgramada> accionesProgramadas = new ConcurrentLinkedQueue<>();
    /**
     * El operador de acciones de entorno, que se encargará de responder apropiadamente a
     * las acciones sobre el entorno realizadas por los agentes.
     */
    private final OperadorAccionesEntorno operadorAcciones = new OperadorAccionesEntorno(this);
    /**
     * El servicio de ejecución de tareas a usar para ejecutar las acciones del entorno cuando
     * corresponda.
     */
    private final Executor servicioAccionesEntorno = Executors.newCachedThreadPool();
    /**
     * El servicio de ejecución de tareas a usar para ejecutar el desplazamiento de todas las hormigas
     * a la localidad de inicio en otro hilo.
     */
    private final ExecutorService servicioDesplazamientoHormigas = Executors.newSingleThreadExecutor();

    /**
     * El objeto encargado de avanzar el tiempo de modo discreto en el sistema,
     * notificándoselo a los agentes.
     */
    private volatile Tiempo tiempo = null;
	/**
	 * El grafo de carreteras, que representa las interconexiones entre localidades
	 * y sus distancias, sobre las que los agentes harán su cometido.
	 */
	private volatile GrafoCarreteras grafoCarreteras = null;
	/**
	 * La localidad de inicio, desde la que las hormigas intentarán encontrar el
	 * camino más corto a la de destino.
	 */
	private volatile Localidad locInicio = null;
	/**
	 * La localidad destino, donde las hormigas encontrarán comida, que desencadenará
	 * la secreción de feromonas.
	 */
	private volatile Localidad locDestino = null;
    /**
     * La vista asociada al grafo de carreteras que modela el mundo actual.
     */
    private volatile VistaGrafo vista = null;
    /**
     * El exponente que determina la importancia relativa de los caminos de feromonas
     * depositados por otras hormigas: a más importancia relativa, más basarán sus
     * decisiones en base a los caminos que otras hormigas recorrieron.
     */
    private volatile double alfa = Double.NEGATIVE_INFINITY;
    /**
     * El exponente que determina la importancia relativa de escoger en cada paso la distancia
     * más corta: a más importancia relativa, las hormigas más basarán sus decisiones en base
     * a una estrategia voraz.
     */
    private volatile double beta = Double.NEGATIVE_INFINITY;
    /**
     * Una constante real pequeña que ciertos algoritmos usan para calcular la magnitud de
     * feromona a depositar en las carreteras.
     */
    private volatile double q = Double.NEGATIVE_INFINITY;
    /**
     * El número de ciclos de simulación máximo a ejecutar por el SMA.
     */
    private volatile int ciclosMaximos;
    /**
     * Indica al {@link algoritmo} cuánta feromona ya depositada anteriormente debiera de retener
     * entre iteraciones.
     */
    private volatile double coeficienteRetencion;
    /**
     * La cantidad de segundos que el entorno esperará a que aparezcan los agentes hormigas
     * en el SMA, contados a partir de la última hormiga que apareció, o bien desde el inicio
     * de la espera de no aparecer una.
     */
    private volatile short segundosEsperaInicio = Short.MIN_VALUE;
    /**
     * La cantidad de milisegundos que deben de pasar entre dos instantes de tiempo consecutivos.
     * Si es 0, no se esperará antes de avanzar al siguiente instante de tiempo, ejecuando la simulación
     * del SMA a la máxima velocidad que permita el sistema.
     */
    private volatile int milisegundosPeriodoTiempo = Integer.MIN_VALUE;
    /**
     * La cantidad de segundos máxima a esperar a que todas las hormigas ejecuten su respuesta
     * a la percepción de avance de tiempo. Si es igual a 0, se esperará indefinidamente,
     * todo el tiempo que haga falta. Cabe destacar que se deja de esperar automáticamente por
     * hormigas que se eliminen del SMA mientras tanto, al igual que se espera automáticamente por
     * hormigas que se incorporen al SMA.
     */
    private volatile short segundosEsperaRespuesta = Short.MIN_VALUE;
    /**
     * El número de hormigas que se espera que estén vivas en el sistema en todo momento.
     */
    private final LongAdder nHormigas = new LongAdder();

    /**
     * El gestor de percepciones asociado a este mundo, con el que esta clase se
     * coordinará para mentener actualizadas las percepciones de los agentes.
     */
    private volatile GestorPercepciones gp = null;
    /**
     * Referencia al objeto que proporciona servicios de ubicación de hormigas
     * para este mundo.
     */
    private final LocalizadorHormigas localizadorHormigas = new LocalizadorHormigas(this);
    /**
     * El coordinador de hormigas en uso por el mundo, que provee servicios de coordinación
     * entre hormigas, para garantizar la consistencia de las invariantes del sistema.
     */
    private final CoordinadorHormigas coordinadorHormigas = new CoordinadorHormigas(this, (final String nombre) -> {
        // Incrementar el número de hormigas presentes en el sistema
        nHormigas.increment();

        // Añadir la hormiga a la localidad de inicio
        getLocalizadorHormigas().mover(nombre, locInicio);

        // Si no estamos esperando a que lleguen las hormigas, actualizar la vista
        // La vista puede ser nula si el SMA se está deteniendo por un error, pero llega una hormiga mientras tanto
        if (vista != null && !getCoordinadorHormigas().esperandoLlegadaHormigas()) {
            vista.actualizar();
        }

        getLogger().log(Level.INFO, "Detectada nueva hormiga en el sistema: " + nombre);
    }, (final String nombre) -> {
        // Decrementar el número de hormigas presentes en el sistema
        nHormigas.decrement();

        // Eliminar a la hormiga de donde quiera que esté
        getLocalizadorHormigas().desaparecer(nombre);

        // Si hemos cargado un algoritmo desde la configuración, notificarle la ida
        if (algoritmo != null) {
            algoritmo.notificarMuerteHormiga(nombre);
        }

        // Actualizar la vista, para mostrar el nuevo número de hormigas
        if (vista != null) {
            vista.actualizar();
        }

        getLogger().log(Level.INFO, "Una hormiga deja de formar parte del sistema: " + nombre);
    });

    /**
     * Representa un parámetro de configuración del {@link Mundo}, leído desde el fichero .mas2j del
     * proyecto.
     * @author Alejandro González García
     */
    private enum ParametroMundo {
        CARRETERAS {
            @Override
            boolean interpretar(final Mundo m, final String arg) {
                boolean toret = true;

                try {
                    m.grafoCarreteras = FactoriaMapas.grafoCarreteras(arg);
                } catch (Exception exc) { // Capturamos todas las excepciones, incluidas las que haya podido generar el constructor particular
                    m.getLogger().log(Level.SEVERE, "No se ha podido cargar el grafo de carreteras especificado: " + exc.getMessage());
                    toret = false;
                }

                return toret;
            }
        },
        LOCALIDAD_INICIO {
            @Override
            boolean interpretar(final Mundo m, final String arg) {
                boolean toret = m.grafoCarreteras != null;
                Localidad loc;

                if (toret) {
                    loc = contieneLocalidad(m.grafoCarreteras.localidades(), arg);
                    toret = loc != null;
                    if (!toret) {
                        m.getLogger().log(Level.SEVERE, "La localidad de inicio especificada no es válida.");
                    } else {
                        m.locInicio = loc;
                    }
                } else {
                    // Esto no debe de pasar, pues este parámetro se lee después de CARRETERAS, solo si
                    // se ha podido obtener el grafo
                    m.getLogger().log(Level.SEVERE, "Ha ocurrido un error interno del programa al intentar reconocer la localidad de inicio.");
                }

                return toret;
            }
        },
        LOCALIDAD_DESTINO {
            @Override
            boolean interpretar(final Mundo m, final String arg) {
                boolean toret = m.grafoCarreteras != null;
                Localidad loc;

                if (toret) {
                    loc = contieneLocalidad(m.grafoCarreteras.localidades(), arg);
                    toret = loc != null && loc != m.locInicio;
                    if (!toret) {
                        m.getLogger().log(Level.SEVERE, "La localidad destino especificada no es válida.");
                    } else {
                        m.locDestino = loc;
                    }
                } else {
                    // Esto no debe de pasar, pues este parámetro se lee después de CARRETERAS, solo si
                    // se ha podido obtener el grafo
                    m.getLogger().log(Level.SEVERE, "Ha ocurrido un error interno del programa al intentar reconocer la localidad destino.");
                }

                return toret;
            }
        },
        CICLOS_MAXIMOS {
            @Override
            boolean interpretar(final Mundo m, final String arg) {
                boolean toret = true;

                try {
                    m.ciclosMaximos = interpretarNatural(m, arg, "La cantidad de ciclos máximos a simular debe de ser un número entero y mayor que 0.", false);
                } catch (IllegalArgumentException exc) {
                    toret = false;
                }

                return toret;
            }
        },
        COEFICIENTE_RETENCION {
            @Override
            boolean interpretar(final Mundo m, final String arg) {
                boolean toret = true;

                try {
                    m.coeficienteRetencion = interpretarReal(m, arg, "El coeficiente de retención de feromona debe de ser un númeror real en el intervalo [0, 1].", false, true);
                } catch (IllegalArgumentException exc) {
                    toret = false;
                }

                return toret;
            }
        },
        ALFA {
            @Override
            boolean interpretar(final Mundo m, final String arg) {
                boolean toret = true;

                try {
                    m.alfa = interpretarReal(m, arg, "El exponente de importancia de la feromona no sigue un formato numérico, es negativo, infinito o inválido.", true, true);
                } catch (IllegalArgumentException exc) {
                    toret = false;
                }

                return toret;
            }
        },
        BETA {
            @Override
            boolean interpretar(final Mundo m, final String arg) {
                boolean toret = true;

                try {
                    m.beta = interpretarReal(m, arg, "El exponente de importancia de la distancia no sigue un formato numérico, es negativo, infinito o inválido.", true, true);
                } catch (IllegalArgumentException exc) {
                    toret = false;
                }

                return toret;
            }
        },
        Q {
            @Override
            boolean interpretar(final Mundo m, final String arg) {
                boolean toret = true;

                try {
                    m.q = interpretarReal(m, arg, "La constante real positiva a usar para calcular la magnitud de las sumas de feromonas no puede ser menor o igual que 0.", true, false);
                } catch (IllegalArgumentException exc) {
                    toret = false;
                }

                return toret;
            }
        },
        ALGORITMO {
            @Override
            boolean interpretar(final Mundo m, final String arg) {
                boolean toret = m.grafoCarreteras != null && m.locDestino != null;

                if (toret) {
                    try {
                        // Crear ahora el gestor de percepciones, para que el algoritmo pueda usarlo.
                        // Por suerte, el mundo ya tiene todos los atributos que necesita para construirse
                        m.gp = new GestorPercepciones(m);

                        m.algoritmo = FactoriaAlgoritmos.algoritmo(arg, m.grafoCarreteras, m.gp, m.ciclosMaximos, m.coeficienteRetencion, m.q, m.locDestino);
                    } catch (Exception exc) { // Capturamos todas las excepciones, incluidas las que haya podido generar el constructor particular
                        m.getLogger().log(Level.SEVERE, "No se ha podido cargar el algoritmo especificado: " + exc.getMessage());
                        toret = false;
                    }
                } else {
                    // Esto no debe de pasar, pues este parámetro se lee después de los otros, solo si
                    // se han podido obtener
                    m.getLogger().log(Level.SEVERE, "Ha ocurrido un error interno del programa al intentar reconocer el algoritmo dado.");
                }

                return toret;
            }
        },
        SEGUNDOS_ESPERA_INICIO {
            @Override
            boolean interpretar(final Mundo m, final String arg) {
                boolean toret = true;

                try {
                    m.segundosEsperaInicio = interpretarNaturalCorto(m, arg, "La cantidad de segundos a esperar por la creación de hormigas en el sistema debe de ser mayor que 0.", false);
                } catch (IllegalArgumentException exc) {
                    toret = false;
                }

                return toret;
            }
        },
        MS_PERIODO_TIEMPO {
            @Override
            boolean interpretar(final Mundo m, final String arg) {
                boolean toret = true;

                try {
                    m.milisegundosPeriodoTiempo = interpretarNatural(m, arg, "La cantidad de milisegundos entre dos instantes de tiempo discreto consecutivos no puede ser menor que 0.", true);
                } catch (IllegalArgumentException exc) {
                    toret = false;
                }

                return toret;
            }
        },
        SEGUNDOS_ESPERA_RESPUESTA {
            @Override
            boolean interpretar(final Mundo m, final String arg) {
                boolean toret = true;

                try {
                    m.segundosEsperaRespuesta = interpretarNaturalCorto(m, arg, "La cantidad de segundos a esperar a que todas las hormigas respondan a la percepción de paso de tiempo no puede ser menor que 0.", true);
                } catch (IllegalArgumentException exc) {
                    toret = false;
                }

                return toret;
            }
        };

        /**
         * Interpreta un parámetro de configuración representado por un número real positivo y finito,
         * a partir de una cadena de texto, mostrando un mensaje de error al usuario si el número no es tal.
         * @param m El mundo vinculado con este parámetro de configuración.
         * @param arg El parámetro de configuración expresado como una cadena de texto.
         * @param mensajeError El mensaje de error a mostrar al usuario si el número no es real, positivo
         * y finito.
         * @param permitirMayorQueUno Si es verdadero, se considerarán inválidos números reales estrictamente
         * mayores que uno. En caso contrario, se tomarán por válidos.
         * @param permitirCero Si es verdadero, se considerará válido el cero. En caso contrario, el cero no
         * será un valor válido.
         * @return El número real interpretado.
         * @throws IllegalArgumentException Si el parámetro no es un número real, positivo y finito.
         */
        final protected double interpretarReal(final Mundo m, final String arg, final String mensajeError, final boolean permitirMayorQueUno, final boolean permitirCero) {
            try {
                final double toret = Double.parseDouble(arg);

                // También rechazar números negativos, iguales a cero si procede, mayores que uno si procede, infinitos o NaN
                if (toret < 0 || (!permitirCero && toret == 0) || (!permitirMayorQueUno && toret > 1) || Double.isInfinite(toret) || Double.isNaN(toret)) {
                    throw new NumberFormatException();
                }

                return toret;
            } catch (NumberFormatException exc) {
                m.getLogger().log(Level.SEVERE, mensajeError);
                throw new IllegalArgumentException();
            }
        }

        /**
         * Interpreta un parámetro de configuración representado por un número natural, a partir de una
         * cadena de texto, mostrando un mensaje de error al usuario si el número no es natural.
         * @param m El mundo vinculado con este parámetro de configuración.
         * @param arg El parámetro de configuración expresado como una cadena de texto.
         * @param mensajeError El mensaje de error a mostrar al usuario si el número no es natural.
         * @param permitirCero Si es verdadero, se permitirá que el parámetro sea 0. En caso contrario, no se
         * permitirá.
         * @return El número natural interpretado.
         * @throws IllegalArgumentException Si el parámetro no es un número natural (no es un entero, es un entero
         * estrictamente menor que cero o es cero y no se permite que sea cero).
         */
        final protected int interpretarNatural(final Mundo m, final String arg, final String mensajeError, final boolean permitirCero) {
            try {
                final int toret = Integer.parseInt(arg);

                // También rechazar números negativos, o iguales a cero si procede
                if (toret < 0 || (!permitirCero && toret == 0)) {
                    throw new NumberFormatException();
                }

                return toret;
            } catch (NumberFormatException exc) {
                m.getLogger().log(Level.SEVERE, mensajeError);
                throw new IllegalArgumentException();
            }
        }

        /**
         * Interpreta un parámetro de configuración representado por un número natural corto (de 16 bits),
         * a partir de una cadena de texto, mostrando un mensaje de error al usuario si el número no es
         * natural.
         * @param m El mundo vinculado con este parámetro de configuración.
         * @param arg El parámetro de configuración expresado como una cadena de texto.
         * @param mensajeError El mensaje de error a mostrar al usuario si el número no es natural.
         * @param permitirCero Si es verdadero, se permitirá que el parámetro sea 0. En caso contrario, no se
         * permitirá.
         * @return El número natural interpretado.
         * @throws IllegalArgumentException Si el parámetro no es un número natural (no es un entero, es un entero
         * estrictamente menor que cero o es cero y no se permite que sea cero).
         */
        final protected short interpretarNaturalCorto(final Mundo m, final String arg, final String mensajeError, final boolean permitirCero) {
            try {
                final short toret = Short.parseShort(arg);

                // También rechazar números negativos, o iguales a cero si procede
                if (toret < 0 || (!permitirCero && toret == 0)) {
                    throw new NumberFormatException();
                }

                return toret;
            } catch (NumberFormatException exc) {
                m.getLogger().log(Level.SEVERE, mensajeError);
                throw new IllegalArgumentException();
            }
        }

        /**
         * Interpreta el parámetro de configuración del mundo dado, realizando
         * las tareas de inicialización que correspondan.
         * @param m El entorno donde guardar los resultados de la interpretación.
         * @param arg El valor del parámetro de configuración a interpretar.
         * @return Verdadero si y solo si el parámetro fue interpretado correctamente.
         * En caso contrario, el método devuelve falso y emite un mensaje de error.
         */
        abstract boolean interpretar(final Mundo m, final String arg);
    };

	/**
	 * Obtiene la única instancia de esta clase (patrón singleton). Se garantiza que el
     * resultado de la ejecución de este método nunca es nulo, y devuelve un objeto
     * totalmente inicializado, sin atributos visiblemente nulos.
	 * @return La devandicha instancia.
	 */
	public static Mundo get() {
        // Esperar a que la instancia del mundo termine de inicializarse
        candadoInicializacion.lock();
        try {
            while (instancia == null) {
                try {
                    final boolean tiempoNoExpirado = mundoInicializado.await(15, TimeUnit.SECONDS);
                    if (!tiempoNoExpirado) {
                        // Avisar al usuario de que podríamos estar bloqueados
                        Logger.getLogger("EnvironmentWatchdog").log(Level.WARNING, "El entorno no se ha inicializado en 15 segundos. Si la espera tarda demasiado, por favor investigue la causa y reinicie el sistema.");
                    }
                } catch (InterruptedException exc) {
                    Logger.getLogger("EnvironmentWatchdog").log(Level.INFO, "Esperando a que se inicie el entorno...");
                }
            }
        } finally {
            candadoInicializacion.unlock();
        }

		return instancia;
	}

	/**
	 * Punto de entrada del entorno, ejecutado antes del comienzo del SMA.
	 * @param args Los argumentos especificados en el fichero .mas2j. Véase la
	 * documentación de {@link interpretarArgumentos} para su significado.
	 */
    @Override
    public void init(final String[] args) {
		boolean detenerSMA = false;

		// Obtener referencia al objeto con servicios útiles para controlar el SMA
		rs = getEnvironmentInfraTier() != null ? getEnvironmentInfraTier().getRuntimeServices() : null;

		if (!interpretarArgumentos(args)) {
			getLogger().log(Level.SEVERE, "Ha ocurrido un error interpretando algún parámetro proporcionado al mundo.");
			detenerSMA = true;
		}

		// Avisar al usuario si no tenemos control sobre el SMA por algún motivo
		if (rs == null) {
			getLogger().log(Level.SEVERE, "El entorno no pudo obtener una referencia a los servicios de tiempo de ejecución de la infraestructura del SMA, lo que significa que no tiene control sobre él y no funcionará correctamente. Por favor, reinicia la aplicación.");
			detenerSMA = true;
		}

		if (!detenerSMA) {
            // Esperamos a que se creen las hormigas iniciales. Si llegasen hormigas al sistema
            // antes o durante la inicialización del entorno (lo que puede ocurrir en la arquitectura
            // de JADE), su notificación de llegada se retendría hasta que se finalice la ejecución de
            // este método
            coordinadorHormigas.esperarLlegadaHormigas(() -> {
                // Actualizar la vista para mostrar el estado inicial. Puede
                // ser nula en caso de que el hilo que ejecuta init() sufra
                // exagerada inanición, y la espera por la llegada de las hormigas
                // se complete antes que la creación de la vista
                if (vista != null) {
                    vista.actualizar();
                }

                // Empezar el avance del tiempo discreto de la simulación
                tiempo = new Tiempo(Mundo.this, () -> {
                    boolean pararAvanceTiempo = false;

                    // Notificar a los agentes del paso del tiempo, y esperar a que hagan lo que tengan que hacer
                    coordinadorHormigas.esperarRespuestaHormigas(() -> gp.percibirPasoTiempo(), segundosEsperaRespuesta);

                    // Ahora, cuando todas las hormigas respondieron, es la hora de ejecutar
                    // las acciones del entorno
                    final long inicioEjecucionAcciones = System.currentTimeMillis();
                    getLogger().log(Level.INFO, "Ejecutando acciones sobre el entorno enviadas por las hormigas...");

                    AccionProgramada accion = accionesProgramadas.poll();
                    while (accion != null) {
                        // Registrar una nueva acción a ejecutar
                        ejecucionAcciones.register();

                        // Realizar la acción en otro hilo de ejecución
                        servicioAccionesEntorno.execute(new TareaAccionEntorno(accion));

                        // Consumir la siguiente acción en la cola
                        accion = accionesProgramadas.poll();
                    }

                    // Esperar hasta que nos interrumpan o las acciones terminen de ejecutarse
                    boolean esperaCompletada = false;
                    while (!esperaCompletada) {
                        try {
                            ejecucionAcciones.awaitAdvanceInterruptibly(ejecucionAcciones.arrive(), 30, TimeUnit.SECONDS);
                            esperaCompletada = true;
                        } catch (TimeoutException exc) {
                            getLogger().log(Level.WARNING, "Se está tardando más de 30 segundos en ejecutar las acciones del entorno programadas. Si la espera tarda demasiado, por favor reinicie el sistema o interrumpa este hilo.");
                        } catch (InterruptedException exc) {
                            getLogger().log(Level.WARNING, "Se ha interrumpido el hilo que esperaba a que finalizase la ejecución de las acciones en el entorno. El hilo hará caso a la interrupción y abandonará la espera, pero ello puede volver inestable al sistema.");
                            pararAvanceTiempo = true;
                        }
                    }

                    getLogger().log(Level.INFO, "Acciones sobre el entorno ejecutadas en " + ((System.currentTimeMillis() - inicioEjecucionAcciones) / 1000.0) + " s.");

                    // Avanzamos a la siguiente iteración del algoritmo
                    final long inicioAvanceIteracion = System.currentTimeMillis();
                    getLogger().log(Level.INFO, "Avanzando a la siguiente iteración...");
                    boolean avanceNuevoCiclo = false;
                    try {
                        avanceNuevoCiclo = algoritmo.avanzarIteracion();
                    } catch (Exception exc) {
                        getLogger().log(Level.WARNING, "Ha ocurrido una excepción no controlada mientras el algoritmo avanzaba a la siguiente iteración.", exc);
                    }
                    getLogger().log(Level.INFO, "Se ha avanzado a la siguiente iteración en " + ((System.currentTimeMillis() - inicioAvanceIteracion) / 1000.0) + " s.");

                    // En caso de que se empiece un nuevo ciclo, mover todas las hormigas
                    // a la localidad de inicio
                    if (avanceNuevoCiclo) {
                        final long inicioAvanceCiclo = System.currentTimeMillis();
                        getLogger().log(Level.INFO, "El algoritmo indica el comienzo de un nuevo ciclo. Avisando a hormigas del comienzo del nuevo ciclo...");

                        // Ir ejecutando el movimiento de todas las hormigas a la localidad de inicio
                        // en otro hilo. De esta forma podríamos hacer algo útil mientras esperamos
                        // por las respuestas a las percepciones
                        final Future<?> terminacionMovimiento = servicioDesplazamientoHormigas.submit(() -> {
                            localizadorHormigas.moverTodasALocalidad(locInicio);
                        });

                        // Esperar a que informen de la recepción de la percepción
                        coordinadorHormigas.esperarRespuestaHormigas(() -> {
                            gp.siguienteCiclo();
                        }, segundosEsperaRespuesta);

                        // Esperar a que se terminen de mover las hormigas a la localidad de inicio:
                        // nada garantiza que ello se termine antes que la respuesta de los agentes
                        try {
                            terminacionMovimiento.get();
                        } catch (ExecutionException exc) {
                            getLogger().log(Level.WARNING, "Ha ocurrido una excepción no controlada mientras se movían hormigas a la localidad de inicio. El sistema no funcionará correctamente a partir de ahora, y se recomienda su detención. Detalles: ", exc);
                        } catch (InterruptedException exc) {
                            getLogger().log(Level.WARNING, "Se ha interrumpido la espera a que todas las hormigas se colocasen de nuevo en la localidad de inicio.");
                            pararAvanceTiempo = true;
                        }

                        getLogger().log(Level.INFO, "Avance de ciclo completado en " + ((System.currentTimeMillis() - inicioAvanceCiclo) / 1000.0) + " s.");
                    }

                    // Si el algoritmo ha terminado, dejar de avanzar el tiempo, pues no hay más que hacer
                    final RazonTerminacion razonTerminacion = algoritmo.razonTerminacion();
                    if (razonTerminacion != null) {
                        getLogger().log(Level.INFO, "El algoritmo ha terminado de ejecutarse: " + razonTerminacion + ".");
                        pararAvanceTiempo = true;
                    }

                    // Finalmente, actualizar la vista mostrada al usuario
                    if (vista != null) {
                        vista.actualizar();
                    }

                    return pararAvanceTiempo;
                }, milisegundosPeriodoTiempo).iniciar();
            }, segundosEsperaInicio);

            // Avisar a hilos interesados de que nos hemos inicializado, poniendo a su disposición
            // la referencia a esta instancia
            candadoInicializacion.lock();
            try {
                mundoInicializado.signalAll();
                instancia = this;
            } finally {
                candadoInicializacion.unlock();
            }

            // Inicializar la vista asociada al mundo. Lo hacemos algo tarde, aún tras
            // avisar a otros hilos de que hemos completado la inicialización del Mundo,
            // para que la vista pueda obtener una referencia al Mundo sin bloquearse. Además,
            // hacerlo algo tarde no es un problema en la implementación actual, porque el Mundo
            // nunca proporciona una referencia a la vista a otros objetos, gracias a la encapsulación :)
            try {
                vista = new VistaGrafo(grafoCarreteras, grafoCarreteras.getNombre() + ".png");
            } catch (NullPointerException | IllegalArgumentException | IllegalStateException exc) {
                detenerSMA = true;
            }
		}

        // Detener el SMA si procede
        if (detenerSMA && rs != null) {
			getLogger().log(Level.WARNING, "Deteniendo SMA.");

            // Esperar un poco antes de detener el sistema, para darle tiempo al usuario de ver qué pasó
            Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                try {
                    rs.stopMAS();
                } catch (Exception ignorada) {}
            }, 10, TimeUnit.SECONDS);

            // Lanzarle una excepción a quien nos ha llamado. En la arquitectura centralizada, esto evita
            // la creación de agentes. En Jade no hace nada, pero da a entender nuestra intención
            throw new IllegalStateException("El SMA debe de detenerse; el entorno no se pudo inicializar.");
		}
    }

	/**
	 * Interpreta los argumentos especificados para el entorno en el fichero .mas2j,
	 * inicializando atributos de este objeto.
	 * @param args Los argumentos leídos del fichero.
     * @return Verdadero si y solo si todos los argumentos se han leído correctamente, falso en
     * otro caso.
	 */
	private boolean interpretarArgumentos(final String[] args) {
		final ParametroMundo[] params = ParametroMundo.values();
		boolean toret = args != null && args.length == params.length;

		if (toret) {
			// Interpretar todos los argumentos recibidos mientras no ocurra un error
			for (int i = 0; i < args.length && toret; ++i) {
				String arg = args[i];

				// Considerar argumentos nulos como erróneos
				if (arg == null) {
					getLogger().log(Level.SEVERE, "El argumento " + (i + 1) + " pasado al mundo es nulo.");
					toret = false;
				} else {
					toret = params[i].interpretar(this, arg);
				}
			}
		} else {
			getLogger().log(Level.SEVERE, "No se ha recibido el número de parámetros esperado para inicializar el mundo.");
		}

		return toret;
	}

	/**
	 * Comprueba si un conjunto de localidades contiene una determinada localidad, identificada
	 * por su nombre, y de ser así devuelve tal localidad.
	 * @param localidades Las localidades donde buscar una localidad en particular.
	 * @param localidad La localidad a intentar encontrar.
	 * @return La localidad especificada si se pudo encontrar, nulo en otro caso.
	 */
	private static Localidad contieneLocalidad(final Set<Localidad> localidades, final String localidad) {
		Localidad toret = null;
        final Iterator<Localidad> iter = localidades.iterator();

        while (toret == null && iter.hasNext()) {
            final Localidad actual = iter.next();
			if (actual.getNombre().equals(localidad)) {
				toret = actual;
			}
        }

		return toret;
	}

	/**
	 * Obtiene el número de hormigas que se espera que haya actualmente en el sistema.
	 * @return El devandicho número.
	 */
	int getNHormigas() {
		return nHormigas.intValue();
	}

	/**
	 * Obtiene la localidad de inicio, donde se ubica el hormiguero.
	 * @return La devandicha localidad.
	 */
	Localidad getLocalidadInicio() {
		return locInicio;
	}

	/**
	 * Obtiene la localidad destino, hacia donde las hormigas intentarán encontrar
	 * un camino corto.
	 * @return La devandicha localidad.
	 */
	Localidad getLocalidadDestino() {
		return locDestino;
	}

    /**
     * Obtiene el grafo de carreteras asociado a este mundo.
     * @return El devandicho grafo.
     */
    GrafoCarreteras getGrafoCarreteras() {
        return grafoCarreteras;
    }

    /**
     * Devuelve alfa, el exponente de importancia relativa de intensidad de feromona.
     * @return El devandicho valor.
     */
    double getAlfa() {
        return alfa;
    }

    /**
     * Devuelve beta, el exponente de importancia relativa de distancias.
     * @return El devandicho valor.
     */
    double getBeta() {
        return beta;
    }

    /**
     * Obtiene la instancia del gestor de percepciones en uso por este mundo.
     * @return El devandicho objeto.
     */
    GestorPercepciones getGestorPercepciones() {
        return gp;
    }

    /**
     * Obtiene la instancia del localizador de hormigas en uso por este mundo.
     * @return El devandicho objeto.
     */
    LocalizadorHormigas getLocalizadorHormigas() {
        return localizadorHormigas;
    }

    /**
     * Devuelve el algoritmo de control de feromona actualmente en uso por el SMA.
     * @return El devandicho algoritmo.
     */
    Algoritmo getAlgoritmo() {
        return algoritmo;
    }

    /**
     * Obtiene el número de localidades presentes en el grafo de carreteras asociado
     * a este mundo.
     * @return El devandicho número.
     */
    public int getNumeroLocalidades() {
        return grafoCarreteras.localidades().size();
    }

    /**
     * Obtiene el coordinador de hormigas asociado a este mundo. Este método está concebido
     * para ser usado por las clases que definen la arquitectura de los agentes hormiga y por
     * la vista asociada.
     * @return El devandicho coordinador.
     */
    public CoordinadorHormigas getCoordinadorHormigas() {
        return coordinadorHormigas;
    }

    @Override
    public void scheduleAction(final String agName, final Structure action, final Object infraData) {
        boolean ejecucionExitosa = true;

        try {
            // Solo programar la ejecución si es una acción válida
            if (operadorAcciones.esAccionValida(agName, action)) {
                getLogger().log(Level.FINE, "La hormiga " + agName + " envió la respuesta " + action.getFunctor() + ".");

                // Añadir a la cola de acciones programadas
                accionesProgramadas.add(new AccionProgramada(agName, action));

                // Finalmente, podemos decir que la hormiga ha ejecutado una respuesta
                coordinadorHormigas.respuestaHormiga(agName);
            } else {
                throw new UnsupportedOperationException("La acción de entorno \"" + (action != null ? action.getFunctor() : "(nula)") + "\" no existe, o no puede ser ejecutada por " + agName + " en este momento.");
            }
        } catch (Exception exc) {
            // Registrar que se ha producido un error
            getLogger().log(Level.WARNING, "Ha ocurrido una excepción no controlada durante la programación de una acción de entorno: ", exc);
            ejecucionExitosa = false;
        } finally {
            // Avisar a la infraestructura de que la ejecución de la acción ha terminado
            getEnvironmentInfraTier().actionExecuted(agName, action, ejecucionExitosa, infraData);
        }
    }

    /**
     * Representa una acción programada para su ejecución en el entorno, a cargo de una hormiga.
     * @author Alejandro González García
     */
    private static final class AccionProgramada {
        /**
         * La hormiga responsable de la acción.
         */
        private final String hormiga;
        /**
         * La acción que la hormiga desea ejecutar.
         */
        private final Structure accion;

        /**
         * Crea una nueva acción programada a cargo de una hormiga.
         * @param hormiga La hormiga responsable.
         * @param accion La acción que desea ejecutar.
         */
        AccionProgramada(final String hormiga, final Structure accion) {
            this.hormiga = hormiga;
            this.accion = accion;
        }

        /**
         * Obtiene la hormiga responsable de esta acción.
         * @return La devandicha hormiga.
         */
        String getHormiga() {
            return hormiga;
        }

        /**
         * Obtiene la acción que la hormiga pretende ejecutar.
         * @return La devandicha acción.
         */
        Structure getAccion() {
            return accion;
        }
    }

    /**
     * Implementa la lógica necesaria para ejecutar una {@link AccionProgramada}
     * determinada en otro hilo de ejecución.
     * @author Alejandro González García
     */
    private final class TareaAccionEntorno implements Runnable {
        /**
         * La acción programada a ejecutar.
         */
        private final AccionProgramada accion;

        /**
         * Crea una nueva tarea para ejecutar una acción programada determinada,
         * opcionalmente en otro hilo.
         * @param accion La acción a ejecutar. El constructor no comprueba que no
         * sea nula.
         */
        TareaAccionEntorno(final AccionProgramada accion) {
            this.accion = accion;
        }

        @Override
        public void run() {
            // Realizar la acción propiamente dicha
            final String hormiga = accion.getHormiga();
            final Structure estructuraAccion = accion.getAccion();
            try {
                operadorAcciones.ejecutarAccion(hormiga, estructuraAccion);
                getLogger().log(Level.FINE, "La hormiga " + hormiga + " ha ejecutado la acción " + estructuraAccion + " sobre el entorno.");
            } catch (Exception exc) {
                getLogger().log(Level.WARNING, "Ha ocurrido una excepción no controlada durante la ejecución de una acción en el entorno para la hormiga " + hormiga + ". Se continúa la ejecución del SMA igualmente. Detalles: ", exc);
            }

            // Hemos terminado de hacerla
            ejecucionAcciones.arriveAndDeregister();
        }
    }
}