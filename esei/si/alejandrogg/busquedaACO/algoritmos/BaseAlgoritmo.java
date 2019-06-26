// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO.algoritmos;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import jason.asSyntax.Atom;
import jason.asSyntax.Term;

import esei.si.alejandrogg.busquedaACO.Algoritmo;
import esei.si.alejandrogg.busquedaACO.Camino;
import esei.si.alejandrogg.busquedaACO.Carretera;
import esei.si.alejandrogg.busquedaACO.Mundo;
import esei.si.alejandrogg.busquedaACO.Localidad;
import esei.si.alejandrogg.busquedaACO.GestorPercepciones;
import esei.si.alejandrogg.busquedaACO.GrafoCarreteras;

/**
 * Implementa lógica de uso común por parte de algoritmos de búsqueda basados
 * en optimización por colonias de hormigas. De esta forma, las subclases de esta clase pueden
 * ahorrarse la reimplementación de tal lógica, y centrarse más en codificar lo que las
 * diferencia.
 *
 * De hecho, esta clase abstracta contiene suficiente lógica como para constituir por ella misma
 * una implementación (algo ineficiente y peculiar) de un algoritmo de búsqueda voraz estocástico, que no
 * modifica los niveles de feromona del entorno. En consecuencia, lo más habitual es que las subclases se
 * centren en añadirle estrategias concretas de actualización de feromonas.
 *
 * @author Alejandro González García
 */
abstract class BaseAlgoritmo implements Algoritmo {
    /**
     * El nombre identificativo de este algoritmo. Este nombre es proporcionado
     * por la implementación concreta del algoritmo (subclase), no por el usuario.
     */
    private final String nombre;
    /**
     * El grafo de carreteras sobre el que se ejecuta el algoritmo.
     */
    protected final GrafoCarreteras grafoCarreteras;
    /**
     * El gestor de percepciones del entorno sobre el que se ejecuta el algoritmo.
     */
    protected final GestorPercepciones gestorPercepciones;
    /**
     * Los ciclos máximos que el algoritmo ejecutará.
     */
    private final int ciclosMaximos;
    /**
     * El ciclo actual en el que se encuentra la ejecución del algoritmo.
     */
    private final LongAdder ciclo;
    /**
     * La localidad de destino asociada a este algoritmo.
     */
    private final Localidad localidadDestino;

    /**
     * El conjunto de hormigas que se han desplazado mediante un arco en la iteración actual.
     */
    private final Set<String> hormigasRecorridoArco = Collections.newSetFromMap(new ConcurrentHashMap<>());
    /**
     * El conjunto de hormigas que han encontrado un camino en el ciclo actual, sea solución o no.
     */
    private final Set<String> hormigasCaminoEncontrado = Collections.newSetFromMap(new ConcurrentHashMap<>());
    /**
     * La cola de todos los caminos encontrados hasta el momento por las hormigas, sean
     * solución o no, en el ciclo actual.
     */
    private final Queue<Camino> caminosEncontrados = new ConcurrentLinkedQueue<>();
    /**
     * Un mapa que relaciona las carreteras por las que pasa un camino encontrado en el ciclo actual
     * que conduce a una solución.
     */
    private final Map<Carretera, List<Camino>> carreterasCaminoSolucion = new ConcurrentHashMap<>();
    /**
     * Objeto cuyo monitor se usa para establecer exclusión mutua y relaciones pasa-antes en los accesos
     * a {@link mejorSolucionEncontrada}.
     */
    private final Object candadoMejorSolucionEncontrada = new Object();
    /**
     * El mejor camino solución encontrado hasta el momento por las hormigas, entre todos los ciclos.
     */
    private Camino mejorSolucionEncontrada = null;

    /**
     * La causa de terminación del algoritmo, si ha terminado todavía.
     */
    private volatile RazonTerminacion razonTerminacion = null;

    /**
     * Crea un nuevo algoritmo a partir de los parámetros especificados.
     * @param grafoCarreteras El grafo de carreteras sobre el que operará este algoritmo.
     * @param gestorPercepciones El gestor de percepciones, que proporciona funcionalidades
     * para actualizar el estado del entorno en base a los resultados del algoritmo.
     * @param ciclosMaximos Los ciclos máximos a ejecutar por el algoritmo.
     * @param localidadDestino La localidad de destino, de la que el algoritmo intentará
     * encontrar el camino más corto hacia ella.
     * @param nombre El nombre identificativo de este algoritmo. Debe de ser proporcionado
     * por la implementación de esta clase, no por el usuario.
     * @throws IllegalArgumentException Si grafoCarreteras es nulo, gestorPercepciones es nulo,
     * ciclosMaximos es menor que 1, localidadDestino es nula o nombre es nulo.
     */
    protected BaseAlgoritmo(final GrafoCarreteras grafoCarreteras, final GestorPercepciones gestorPercepciones, final int ciclosMaximos, final Localidad localidadDestino, final String nombre) {
        if (grafoCarreteras == null) {
            throw new IllegalArgumentException("No se puede crear un algoritmo asociado a un grafo de carreteras nulo.");
        }
        if (gestorPercepciones == null) {
            throw new IllegalArgumentException("No se puede crear un algoritmo asociado a un gestor de percepciones nulo.");
        }
        if (ciclosMaximos < 1) {
            throw new IllegalArgumentException("Los ciclos máximos a ejecutar por un algoritmo no pueden tener un valor menor que 1.");
        }
        if (localidadDestino == null) {
            throw new IllegalArgumentException("No se puede crear un algoritmo para encontrar caminos a una localidad de destino nula.");
        }
        if (nombre == null) {
            throw new IllegalArgumentException("El nombre identificativo asociado a un algoritmo no puede ser nulo.");
        }
        this.grafoCarreteras = grafoCarreteras;
        this.gestorPercepciones = gestorPercepciones;
        this.ciclosMaximos = ciclosMaximos;
        this.localidadDestino = localidadDestino;
        this.nombre = nombre;
        this.ciclo = new LongAdder();
        ciclo.increment(); // Comenzar número de ciclo en 1
    }

    /**
     * Valida e interpreta una lista de localidades recibida de una hormiga, que define el camino que ha recorrido.
     * Se asume que la lista de localidades es no nula, aunque sus elementos pueden serlo o no.
     * @param listaLocalidades La lista de localidades a validar.
     * @return El camino recorrido por la hormiga, generado a partir de lo que ha indicado en la lista, en un estado
     * inmutable.
     * @throws IllegalArgumentException Si la lista tiene menos de dos elementos, algún elemento de la
     * lista no es un átomo, un átomo de la lista no representa una localidad, o no se ha podido encontrar
     * una carretera que conecte a una localidad del camino con otra.
     */
    protected final Camino listaLocalidadesACamino(final List<Term> listaLocalidades) {
        final Camino camino = new Camino(grafoCarreteras);
        int i = 0;
        Localidad localidadAnterior = null;
        Localidad localidadActual = null;
        final Iterator<Term> iter = listaLocalidades.iterator();

        // Aunque quizás esta comprobación parezca defensiva de más, no lo es tanto: si un agente nos envía una
        // variable no instanciada, puede interpretarse como una lista, pero que no tiene iterador (fantástica
        // seguridad de tipos la de Jason)
        if (iter != null) {
            while (iter.hasNext()) {
                final Term t = iter.next();

                // Comprobar que la localidad sea del tipo esperado
                if (t instanceof Atom) {
                    localidadActual = gestorPercepciones.localidadAtomo((Atom) t);

                    // Comprobar que realmente se corresponde con una localidad existente
                    if (localidadActual != null) {
                        // Añadir carreteras del camino a partir de la segunda localidad, enlazándola
                        // con la inmediatamente anterior
                        if (i > 0) {
                            camino.addCarretera(localidadAnterior, localidadActual);
                        }
                    } else {
                        throw new IllegalArgumentException("La localidad " + ((Atom) t).getFunctor() + " indicada por una hormiga no existe en el mapa actual.");
                    }
                } else {
                    throw new IllegalArgumentException("Un elemento de la lista de localidades del camino encontrado por una hormiga no es un átomo.");
                }

                // Actualizar localidad inmediatamente anterior y contador de localidades recorridas
                localidadAnterior = localidadActual;
                ++i;
            }
        }

        // Tiene poco sentido considerar caminos que no consistan en al menos una carretera
        if (i < 2) {
            throw new IllegalArgumentException("El camino seguido por una hormiga no tiene al menos dos localidades.");
        }

        // Hemos terminado de añadir carreteras al camino. Hacerlo de solo lectura
        camino.terminarConstruccion();

        return camino;
    }

    /**
     * Comprueba si el camino especificado constituye una solución candidata del problema de búsqueda.
     * @param camino El camino a comprobar si conduce a una solución. Se asume que es no nulo.
     * @return Verdadero si y solo si el camino dado conduce a una solución del problema, falso en
     * caso contrario.
     */
    protected final boolean conduceASolucion(final Camino camino) {
        return localidadDestino.equals(camino.getUltimaLocalidad());
    }

    /**
     * Señaliza la conclusión de una iteración. Como efectos colaterales, este método
     * actualiza estructuras de datos internas cuyo contenido debe de variar por la finalización
     * de una iteración.
     */
    protected final void finalizarIteracion() {
        hormigasRecorridoArco.clear();
    }

    /**
     * Avanza el algoritmo al siguiente ciclo. Como efectos colaterales, este método
     * actualiza estructuras de datos internas cuyo contenido debe de variar por la
     * finalización de un ciclo. Nótese que la conclusión de un ciclo implica la
     * conclusión de una iteración: al realizar la última iteración de un ciclo,
     * una llamada a este método finaliza automáticamente la iteración actual.
     * Así pues, es innecesario (e incluso podría ser inseguro, desde un punto de vista
     * de concurrencia) finalizar la última iteración de un ciclo explícitamente,
     * mediante una llamada previa o posterior a {@link finalizarIteracion}.
     */
    protected final void siguienteCiclo() {
        hormigasRecorridoArco.clear();
        hormigasCaminoEncontrado.clear();
        carreterasCaminoSolucion.clear();
        ciclo.increment();
    }

    /**
     * Consulta las estructuras de datos internas del algoritmo, que se mantienen actualizadas
     * actualizadas con las apropiadas invocaciones de métodos señalizadores de eventos, para
     * determinar si todas las hormigas han realizado el mismo camino solución en el ciclo actual.
     * Cuando termine la invocación de este método, la estructura de datos interna que se encarga
     * de registrar qué caminos encontraron las hormigas se vaciará, por lo que una segunda llamada
     * a este método (u otros que dependan de esa información) puede dar un resultado diferente.
     * En caso de que alguna subclase sobreescriba alguna implementación de algún método, debe de cumplir
     * los términos del contrato aquí documentados para que este método siga funcionando correctamente.
     * @return Verdadero si y solo si todas las hormigas han ido por el mismo camino solución en este ciclo.
     */
    protected final boolean todasHormigasHicieronMismoCamino() {
        boolean toret = ciclo.intValue() > 1;
        Camino caminoPrimero;
        Camino caminoActual;

        // Solo considerar caminos repetidos si no es la primera iteración. Puede pasar que,
        // con pocas hormigas y localidades, tengamos la mala suerte de que vayan por el mismo
        // camino la primera vez. Que lo hagan dos veces es ya bastante más raro
        if (toret) {
            // Tomar el primer elemento como el primer anterior. Si no hay un primer elemento,
            // entonces no puede haber dos caminos iguales, por lo que debemos de devolver falso
            caminoPrimero = caminosEncontrados.poll();
            if (caminoPrimero != null) {
                // Si tiene un segundo elemento, y ese segundo elemento es igual al primero,
                // entonces seguir comparando hasta que lleguemos al final o encontremos un
                // camino distinto. Si no tiene un segundo elemento, entonces ninguna hormiga
                // hizo un camino o bien solo hay una hormiga en el sistema, con lo cual no
                // se considera que varias hormigas hiciesen el mismo
                caminoActual = caminosEncontrados.poll();
                do {
                    toret = caminoPrimero.equals(caminoActual);
                    caminoActual = caminosEncontrados.poll();
                } while (caminoActual != null && toret);
            } else {
                toret = false;
            }
        }

        // Eliminar caminos restantes que quedasen
        caminosEncontrados.clear();

        return toret;
    }

    /**
     * Comprueba si se debiera de avanzar al siguiente ciclo, dado el estado actual
     * de las estructuras de datos internas del algoritmo usadas para deducir tal conclusión.
     * @return Verdadero si y solo si se debiera de avanzar al siguiente ciclo, falso en
     * caso contrario.
     */
    protected final boolean procedeAvanzarASigCiclo() {
        // Cuando ninguna hormiga ha avanzado desde donde está, es porque ya todas están en el hormiguero,
        // y han encontrado un camino (o todas están bloqueadas, lo que no debiera de ocurrir, y avanzar al
        // siguiente ciclo quizás ayude a resolverlo)
        return hormigasRecorridoArco.isEmpty();
    }

    /**
     * Marca la ejecución de este algoritmo como terminada, señalizando el deseo de finalización
     * de su ejecución, por la causa dada.
     * @param razonTerminacion La causa de terminación del algoritmo.
     */
    protected final void terminar(final RazonTerminacion razonTerminacion) {
        this.razonTerminacion = razonTerminacion;
    }

    /**
     * Realiza un postprocesamiento de todos los caminos solución encontrados por las hormigas en el ciclo actual,
     * lo cual es útil, por ejemplo, para depositar feromonas al terminar un ciclo. Para ese caso de uso u otros
     * similares, es preferible implementar este método con un cuerpo significativo que sobreescribir otros.
     * @param caminosCarretera Un mapa que relaciona cada carretera con los caminos solución que pasan por ella.
     */
    protected abstract void procesarCaminosSolucionCiclo(final Map<Carretera, List<Camino>> caminosCarretera);

    /**
     * Realiza un postprocesamiento de un arco (carretera) recorrido por una hormiga en una iteración,
     * lo cual es útil, por ejemplo, para depositar feromonas. Para ese caso de uso u otros similares, es preferible
     * implementar este método con un cuerpo significativo que sobreescribir otros.
     * @param carretera La carretera que ha recorrido la hormiga.
     */
    protected abstract void procesarArco(final Carretera carretera);

    /**
     * {@inheritDoc} En el caso de esta implementación, se toma nota de las hormigas que recorrieron un
     * arco en la iteración actual. La mayoría de subclases no encontrarán utilidad a sobreescribir
     * este método.
     */
    @Override
    public void recorrerArco(final String hormiga, final Carretera carretera) {
        hormigasRecorridoArco.add(hormiga);

        // Avisar a subclases del arco recorrido
        procesarArco(carretera);
    }

    /**
     * {@inheritDoc} Esta implementación simplemente toma nota del camino recorrido por la hormiga,
     * actualizando estructuras de datos internas. La mayoría de subclases no encontrarán utilidad a
     * sobreescribir este método.
     */
    @Override
    public void notificarCaminoEncontrado(final String hormiga, final List<Term> listaLocalidades) {
        // Validar e interpretar parámetros de entrada
        final Camino camino = listaLocalidadesACamino(listaLocalidades);

        // Añadir a conjunto de hormigas que encontraron un camino ahora
        final boolean caminoEncontradoAhora = hormigasCaminoEncontrado.add(hormiga);

        // ¿Encontró un camino en el ciclo actual, y solo en el ciclo actual?
        if (caminoEncontradoAhora) {
            // Registrar el camino que encontró
            caminosEncontrados.add(camino);

            // Si conduce a una solución, registrarlo como tal
            if (conduceASolucion(camino)) {
                // Asociar el camino con las carreteras por las que pasa

                // TODO: paralelizar con Spliterators
                for (final Carretera carretera : camino) {
                    carreterasCaminoSolucion.compute(carretera, (final Carretera ignorada, final List<Camino> caminosAnteriores) -> {
                        List<Camino> toret = caminosAnteriores;

                        // Crear una lista si todavía no tenemos una para esta carretera. No hace falta que sea
                        // segura entre varios hilos de ejecución porque compute() y get() ya establecen relaciones
                        // pasa-antes respecto a su modificación
                        if (toret == null) {
                            toret = new LinkedList<>();
                        }
                        toret.add(camino);

                        return toret;
                    });
                }

                // Si esta es una mejor solución que la que actualmente tenemos, registrarla como tal
                synchronized (candadoMejorSolucionEncontrada) {
                    if (mejorSolucionEncontrada == null || camino.compareTo(mejorSolucionEncontrada) < 0) {
                        mejorSolucionEncontrada = camino;
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc} Esta implementación simplemente toma nota del suceso, actualizando estructuras de datos
     * internas. La mayoría de subclases no encontrarán utilidad a sobreescribir este método.
     */
    @Override
    public void notificarMuerteHormiga(final String hormiga) {
        hormigasRecorridoArco.remove(hormiga);
    }

    /**
     * {@inheritDoc} Esta implementación simplemente actualiza el estado interno del algoritmo, sin cambiar
     * los niveles de feromona de ninguna carretera. La mayoría de subclases no encontrarán utilidad a
     * sobreescribir este método. Se asume que se llama a este método tras la notificación por parte de las
     * hormigas de todos los eventos pertinentes, pues no tendría sentido razonar sobre información no
     * completa de un episodio del SMA.
     */
    @Override
    public boolean avanzarIteracion() {
        boolean toret = procedeAvanzarASigCiclo();

        // Si debemos de avanzar al siguiente ciclo, realizar las tareas pertinentes
        if (toret) {
            // Condiciones de terminación: hemos llegado al número de ciclos máximo, o todas
            // las hormigas siguieron el mismo camino
            if (getCiclo() >= getCiclosMaximos() - 1) {
                terminar(RazonTerminacion.CICLOS_CONSUMIDOS);
            } else if (todasHormigasHicieronMismoCamino()) {
                terminar(RazonTerminacion.ESTANCAMIENTO);
            }

            // Realizar procesamiento de los caminos solución encontrados, por ejemplo para
            // depositar feromonas (depende de la implementación)
            procesarCaminosSolucionCiclo(carreterasCaminoSolucion);

            // Finalmente, avanzar al siguiente ciclo
            siguienteCiclo();
        } else {
            // Ir a la siguiente iteración
            finalizarIteracion();
        }

        return toret;
    }

    @Override
    public final int getCiclo() {
        return ciclo.intValue();
    }

    @Override
    public final String getNombre() {
        return nombre;
    }

    @Override
    public final int getCiclosMaximos() {
        return ciclosMaximos;
    }

    @Override
    public final RazonTerminacion razonTerminacion() {
        return razonTerminacion;
    }

    @Override
    public final Camino getMejorCamino() {
        synchronized (candadoMejorSolucionEncontrada) {
            if (mejorSolucionEncontrada != null) {
                return mejorSolucionEncontrada;
            } else {
                throw new NoSuchElementException();
            }
        }
    }
}