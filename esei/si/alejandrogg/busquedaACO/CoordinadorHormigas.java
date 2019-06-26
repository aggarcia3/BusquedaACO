// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO;

import java.util.Set;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.function.Consumer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.System.currentTimeMillis;

/**
 * Esta clase provee servicios y toma responsabilidades relacionadas con la correcta
 * sincronización entre agentes de hormigas, para garantizar la corrección y consistencia
 * de la simulación independientemente de la velocidad de ejecución de las acciones
 * individuales de los agentes. Es una fabricación pura.
 *
 * CoordinadorHormigas tiene una relación relativamente estrecha con {@link Mundo}, que se refleja
 * en un contrato relativamente estricto, con dos términos: esta clase no debe invocar métodos
 * de {@link Mundo} sin asegurarse previamente de que se han inicializado los datos que usan.
 * Destacablemente, invocar los métodos getLogger() y getNHormigas() es, por construcción de la
 * clase {@link Mundo}, siempre seguro. Por otro lado, el {@link Mundo} se debe de comprometer a no propagar
 * instancias de CoordinadorHormigas antes de que complete su propia inicialización, lo cual actualmente
 * hace mediante una espera por una condición en su método get(). Este último término es necesario para
 * garantizar el correcto funcionamiento de la espera inicial por las hormigas.
 *
 * @see Mundo
 * @author Alejandro González García
 */
public final class CoordinadorHormigas {
    /**
     * El mundo sobre el que este coordinador presta sus servicios.
     */
    private final Mundo mundo;
    /**
     * Candado que se utiliza para sincronizar varios hilos de ejecución
     * cuando se inicia la espera por hormigas.
     */
    private final Lock candadoInicioEspera = new ReentrantLock();
    /**
     * Condición de inicio de espera por hormigas del SMA.
     */
    private final Condition condicionInicioEspera = candadoInicioEspera.newCondition();
    /**
     * Candado (lock) que se utiliza para sincronizar varios hilos de ejecución
     * cuando llegan hormigas.
     */
    private final Lock candadoLlegadaHormigas = new ReentrantLock();
    /**
     * Condición de llegada de hormigas al SMA.
     */
    private final Condition condicionLlegada = candadoLlegadaHormigas.newCondition();
    /**
     * El candado que utilizamos para establecer relaciones pasa-antes necesarias
     * para la atomicidad y visibilidad de la llegada de las respuestas de las hormigas.
     * Aunque la clase Phaser de Java provee funcionalidades similares, por simplicidad
     * es deseable no tener que gestionar también la visibilidad de la referencia al Phaser
     * creado, de forma que se pueda saber en todo momento cuántas hormigas quedan por
     * responder, y si se está esperando por la respuesta de hormigas, con tan solo consultar
     * el valor de un entero. Además, así también podríamos reducir el número de candados
     * que los hilos tienen que adquirir para hacer su cometido.
     */
    private final Lock candadoEsperaRespuesta = new ReentrantLock();
    /**
     * La condición que la última hormiga en responder indicará que ha ocurrido, para notificar
     * a quien espera por la respuesta de todas las hormigas de que su espera se ha completado.
     */
    private final Condition condicionEsperaRespuesta = candadoEsperaRespuesta.newCondition();

    /**
     * Tarea cuyo código se ejecutará cuando llegue una hormiga al sistema.
     */
    private final Consumer<String> tareaLlegadaHormiga;
    /**
     * Tarea cuyo código se ejecutará cuando una hormiga deje de formar parte del
     * sistema.
     */
    private final Consumer<String> tareaMuerteHormiga;
    /**
     * Indica si ha llegado recientemente una hormiga al sistema que todavía
     * no fue atendida por esta clase. Los accesoa a esta variable, tanto de lectura
     * como de escritura, deben de sincronizarse con el monitor de {@link condicionLlegada}.
     * Se usa para evitar que tengan influencia en el sistema los "spurious wakeups".
     */
    private boolean hormigaRecienLlegada = false;
    /**
     * Toma el valor verdadero si y solo si nos encontramos esperando por la llegada
     * de hormigas actualmente.
     */
    private volatile boolean esperandoHormigas = false;
    /**
     * El conjunto de hormigas que enviaron una respuesta durante la espera actual por respuestas.
     * Se usa para no contar más de una vez respuestas múltiples. Los accesos a este TAD deben
     * de sincronizarse con el candado {@link candadoEsperaRespuesta}.
     */
    private final Set<String> hormigasQueRespondieron = new HashSet<>();
    /**
     * El número de hormigas que quedan por responder en el momento presente.
     * Este número es negativo si no se está esperando a que las hormigas respondan.
     * Los accesos a esta variable deben de sincronizarse con el candado {@link candadoEsperaRespuesta}.
     */
    private int hormigasEsperandoRespuesta = Integer.MIN_VALUE;

    /**
     * Crea un nuevo coordinador de hormigas, con sus tareas asociadas a ejecutar
     * de modo asíncrono cuando ocurran ciertos eventos.
     * @param mundo El mundo cuyas hormigas este objeto coordinará. Como contrato general, esta clase no debe
     * invocar métodos en este objeto sin asegurarse previamente de que se han inicializado los datos que usan.
     * Destacablemente, invocar los métodos getLogger() y getNHormigas() es, por construcción de la clase Mundo,
     * siempre seguro.
     * @param tareaLlegadaHormiga La tarea cuyo código se ejecutará cuando llegue una hormiga al sistema.
     * @param tareaMuerteHormiga La tarea cuyo código se ejecutará cuando una hormiga deje de formar parte del SMA.
     * @throws IllegalArgumentException Si algún parámetro es nulo.
     */
    CoordinadorHormigas(final Mundo mundo, final Consumer<String> tareaLlegadaHormiga, final Consumer<String> tareaMuerteHormiga) {
        if (mundo == null) {
            throw new IllegalArgumentException("El mundo asociado a un coordinador de hormigas no puede ser nulo.");
        }
        if (tareaLlegadaHormiga == null) {
            throw new IllegalArgumentException("La tarea a realizar cuando llegue una hormiga no puede ser nula.");
        }
        if (tareaMuerteHormiga == null) {
            throw new IllegalArgumentException("La tarea a realizar cuando una hormiga deje de formar parte del sistema no puede ser nula.");
        }
        this.mundo = mundo;
        this.tareaLlegadaHormiga = tareaLlegadaHormiga;
        this.tareaMuerteHormiga = tareaMuerteHormiga;
    }

    /**
     * Implementa una tarea que espera a que aparezcan hormigas en el sistema, esperando
     * a lo sumo un tiempo configurable desde la llegada de la última hormiga o el inicio
     * de la espera.
     * @author Alejandro González García
     */
    private class EsperadorHormigas implements Runnable {
        /**
         * Los milisegundos a esperar a que llegue una nueva hormiga, contados
         * desde la llegada de la última hormiga o desde el inicio de la espera.
         */
        private final int msEspera;
        /**
         * La tarea cuyo método run() se ejecutará cuando se termine de realizar
         * la espera.
         */
        private final Runnable tareaFinEspera;
        /**
         * Toma el valor verdadero si el método run() ha comenzado su ejecución,
         * y está listo para procesar la llegada de hormigas.
         */
        private volatile boolean hiloIniciado = false;

        /**
         * Crea un nuevo esperador de hormigas, con un determinado tiempo de espera máximo.
         * @param segundosEspera El devandicho tiempo de espera.
         * @param tareaFinEspera La tarea a ejecutar cuando finalice la espera.
         */
        EsperadorHormigas(final short segundosEspera, final Runnable tareaFinEspera) {
            if (segundosEspera <= 0) {
                throw new IllegalArgumentException("No se puede esperar una cantidad de tiempo negativa o nula a que lleguen hormigas.");
            }
            if (tareaFinEspera == null) {
                throw new IllegalArgumentException("La tarea a ejecutar cuando se termine de esperar por la creación de hormigas no puede ser nula.");
            }

            this.msEspera = ((int) segundosEspera) * 1000;
            this.tareaFinEspera = tareaFinEspera;
        }

        /**
         * Devuelve verdadero si y solo si el objeto está listo para procesar
         * la llegada de hormigas.
         * @return El descrito valor de retorno.
         */
        boolean hiloIniciado() {
            return hiloIniciado;
        }

        @Override
        public void run() {
            final long inicioEspera = currentTimeMillis();
            final Thread hiloActual = Thread.currentThread();
            boolean tiempoExpirado = false;

            // Mientras no expire el tiempo desde que llegó la última hormiga, esperar
            while (!tiempoExpirado) {
                candadoLlegadaHormigas.lock();
                try {
                    final long finalEspera = currentTimeMillis() + msEspera;

                    // Avisar a otros hilos de que nos hemos iniciado, y estamos listos para
                    // atender la llegada de hormigas
                    if (!hiloIniciado) {
                        candadoInicioEspera.lock();
                        try {
                            condicionInicioEspera.signal();
                            hiloIniciado = true;
                        } finally {
                            candadoInicioEspera.unlock();
                        }
                    }

                    while (!hormigaRecienLlegada && !tiempoExpirado) {
                        // Esperar msEspera ms a que la llegada de una hormiga active la condición
                        // de espera. Si no lo hace ninguna hormiga en ese plazo, entonces el tiempo
                        // expira y dejamos de esperar
                        final long msEspera = finalEspera - currentTimeMillis();
                        if (msEspera > 0) {
                            try {
                                condicionLlegada.await(msEspera, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException exc) {
                                // Informar de que hemos recibido la petición de interrupción,
                                // pero no hacerle caso
                                mundo.getLogger().log(Level.INFO, "Todavía se espera por la siguiente hormiga...");
                            }
                        }

                        // Si hemos esperado tanto que hemos llegado al momento límite, entonces es
                        // que ha expirado el tiempo
                        if (msEspera <= 0) {
                            mundo.getLogger().log(Level.INFO, "Se ha esperado por " + mundo.getNHormigas() + " hormiga/s durante " + ((currentTimeMillis() - inicioEspera) / 1000.0) + " s.");
                            tiempoExpirado = true;
                        }
                    }

                    // Si tras la espera ha llegado una hormiga, dejar de considerarla como recién llegada
                    if (hormigaRecienLlegada) {
                        hormigaRecienLlegada = false;
                    }
                } finally {
                    candadoLlegadaHormigas.unlock();
                }
            }

            // Hemos completado la espera, así que ejecutar la tarea pertinente
            CoordinadorHormigas.this.esperandoHormigas = false;
            tareaFinEspera.run();
        }
    }

    /**
     * Espera a que lleguen (se creen) hormigas iniciales en el SMA, deteniendo la espera si pasa un
     * tiempo desde la llegada de la última hormiga, o pasa ese tiempo sin que llegue ninguna hormiga.
     * Cuando termine la espera se ejecutará una tarea, que se pasa como parámetro. La espera es finita y
     * terminará en algún momento, si llega un número finito de hormigas al sistema. Cuando este
     * método devuelve el control a quien lo llama, se garantiza que la espera a la llegada de hormigas se
     * haya puesto en marcha, y el coordinador se encuentra, a todos los efectos, listo para atender tales
     * eventos.
     * @param tareaFinEspera La tarea a ejecutar cuando termine la espera.
     * @param segundosEspera El tiempo máximo a esperar desde la llegada de la última hormiga o el inicio
     * de la espera.
     * @throws IllegalArgumentException Si segundosEspera es menor o igual que 0, o tareaFinEspera es nula.
     */
    void esperarLlegadaHormigas(final Runnable tareaFinEspera, final short segundosEspera) {
        // Estamos esperando por la llegada de hormigas sí o sí
        esperandoHormigas = true;

        // Avisamos al usuario de la intención de espera
        mundo.getLogger().log(Level.INFO, "Esperando a que lleguen las hormigas al sistema multiagente...");

        // Creamos el hilo para la espera, y le decimos a la JVM que lo inicie
        final EsperadorHormigas eh = new EsperadorHormigas(segundosEspera, tareaFinEspera);
        new Thread(eh).start();

        // No sabemos cuándo se va a iniciar el hilo (la JVM lo hará, en coordinación con el
        // SO, en algún momento futuro desconocido). Por tanto, debemos de esperar por él para
        // garantizar una postcondición de salida de este método
        candadoInicioEspera.lock();
        try {
            while (!eh.hiloIniciado()) {
                try {
                    final boolean tiempoNoExpirado = condicionInicioEspera.await(10, TimeUnit.SECONDS);
                    if (!tiempoNoExpirado) {
                        mundo.getLogger().log(Level.WARNING, "El hilo de espera de llegada de hormigas no se ha iniciado en 10 segundos. Si la espera tarda demasiado, por favor reinicie el sistema.");
                    }
                } catch (InterruptedException exc) {
                    mundo.getLogger().log(Level.INFO, "Esperando a que se inicie el hilo de espera de llegada de hormigas...");
                }
            }
        } finally {
            candadoInicioEspera.unlock();
        }
    }

    /**
     * Espera a que todas las hormigas de las que se tiene constancia en el SMA terminen
     * su reacción a la percepción de paso de tiempo. Este método no devuelve el control a
     * quien lo llama hasta que todas las hormigas han reaccionado, o bien ha pasado un tiempo
     * máximo.
     * @param tareaNotificacionRespuesta La tarea que se responsabiliza de enviar la percepción de
     * que el entorno está listo para esperar por la respuesta de las hormigas. Se ejecutará en
     * el mismo hilo que ha llamado a este método.
     * @param segundosEspera Los segundos a esperar como máximo a que todas las hormigas
     * respondan. Un valor menor o igual que 0 significa esperar indefinidamente.
     * @throws IllegalStateException Si se vuelve a intentar esperar por la respuesta de hormigas
     * mientras tal espera está teniendo lugar.
     * @throws IllegalArgumentException Si el parámetro tareaNotificacionRespuesta es nulo.
     */
    void esperarRespuestaHormigas(final Runnable tareaNotificacionRespuesta, final short segundosEspera) {
        if (tareaNotificacionRespuesta == null) {
            throw new IllegalArgumentException("La tarea de notificación de que el SMA está listo para esperar por la respuesta de hormigas no puede ser nula.");
        }

        candadoEsperaRespuesta.lock();
        try {
            // Comprobar que no estamos ya esperando por la respuesta de hormigas
            if (hormigasEsperandoRespuesta >= 0) {
                throw new IllegalStateException("No se puede esperar por la respuesta de las hormigas mientras tal espera está ya teniendo lugar.");
            }

            // Avisar al usuario del inicio de la espera
            mundo.getLogger().log(Level.INFO, "Esperando a que todas las hormigas ejecuten su respuesta...");

            // Inicialmente, esperaremos por la respuesta de tantas hormigas como haya.
            // Las acciones de las hormigas (llegada, muerte y respuesta) influirán en el
            // número de hormigas por las que se espera la respuesta
            hormigasEsperandoRespuesta = mundo.getNHormigas();

            // Solo tiene sentido esperar si hay al menos una hormiga
            final long inicioEspera = currentTimeMillis();
            if (hormigasEsperandoRespuesta > 0) {
                // Notificar a los agentes hormigas que estamos listos para atender sus respuestas.
                // Si hiciésemos esta notificación en otro momento, fuera de esta sección crítica, no garantizaríamos
                // que viesen el inicio de la espera
                tareaNotificacionRespuesta.run();

                // Cuando todas las hormigas hayan terminado, terminar la espera
                if (segundosEspera > 0) {
                    long inicioIteracionEspera;
                    long tiempoRestante = segundosEspera * 1000;

                    while (hormigasEsperandoRespuesta > 0 && tiempoRestante > 0) {
                        inicioIteracionEspera = currentTimeMillis();

                        try {
                            condicionEsperaRespuesta.await(tiempoRestante, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException ignorar) {}

                        // Descontar el tiempo transcurrido esperando del tiempo restante
                        tiempoRestante -= currentTimeMillis() - inicioIteracionEspera;
                    }
                } else {
                    try {
                        while (hormigasEsperandoRespuesta > 0) {
                            condicionEsperaRespuesta.await();
                        }
                    } catch (InterruptedException exc) {
                        mundo.getLogger().log(Level.WARNING, "Se ha interrumpido la espera por la respuesta de las hormigas antes de que se completase. No se garantiza que se hayan recibido todas las respuestas posibles, y el resultado podría ser inconsistente.");
                    }
                }
            } else {
                mundo.getLogger().log(Level.INFO, "No hay hormigas por las que esperar.");
            }

            mundo.getLogger().log(Level.INFO, "Espera por respuesta de " + hormigasQueRespondieron.size() + " hormiga/s completada en " + ((currentTimeMillis() - inicioEspera) / 1000.0) + " s.");

            // Ya no estamos esperando por la respuesta de hormigas
            hormigasEsperandoRespuesta = Integer.MIN_VALUE;
            hormigasQueRespondieron.clear();
        } finally {
            candadoEsperaRespuesta.unlock();
        }
    }

    /**
     * Comprueba si se está esperando actualmente por la llegada de hormigas al sistema.
     * @return El valor de verdad de la proposición descrita.
     */
    boolean esperandoLlegadaHormigas() {
        return esperandoHormigas;
    }

    /**
     * Maneja el evento de llegada de una hormiga al SMA, enviando las notificaciones
     * pertinentes.
     * @param nombre La hormiga que llega.
     */
    public void llegadaHormiga(final String nombre) {
        // Notificar a la espera por hormigas, si está teniendo lugar tal espera
        // (asumimos que la inicialización del entorno se completa antes de que la creación
        // asíncrona de agentes obtenga una referencia a este coordinador)
        if (esperandoHormigas) {
            candadoLlegadaHormigas.lock();
            try {
                condicionLlegada.signal();
                hormigaRecienLlegada = true;
            } finally {
                candadoLlegadaHormigas.unlock();
            }
        }

        // Si hay una espera por la respuesta de hormigas en curso, registrarnos
        // como participante
        candadoEsperaRespuesta.lock();
        try {
            if (hormigasEsperandoRespuesta >= 0) {
                ++hormigasEsperandoRespuesta;
            }
        } finally {
            candadoEsperaRespuesta.unlock();
        }

        // Realizar la tarea de llegada de hormiga
        tareaLlegadaHormiga.accept(nombre);
    }

    /**
     * Maneja el evento de abandono de una hormiga del SMA, enviando las notificaciones
     * pertinentes.
     * @param nombre La hormiga que deja de formar parte del SMA.
     */
    public void muerteHormiga(final String nombre) {
        // Si hay una espera por la respuesta de hormigas en curso, cancelar
        // nuestra participación en la respuesta
        candadoEsperaRespuesta.lock();
        try {
            if (hormigasEsperandoRespuesta > 0) {
                // Avisar a quien espera por la respuesta de todas las hormigas si esta fue la última
                if (--hormigasEsperandoRespuesta == 0) {
                    condicionEsperaRespuesta.signal();
                }
            }
        } finally {
            candadoEsperaRespuesta.unlock();
        }

        // Realizar la tarea de muerte de hormiga
        tareaMuerteHormiga.accept(nombre);
    }

    /**
     * Maneja el evento de respuesta de una hormiga a la percepción de paso del tiempo,
     * enviando las notificaciones pertinentes.
     * @param nombre El nombre de la hormiga que responde a la percepción.
     */
    public void respuestaHormiga(final String nombre) {
        // En principio, solo tiene sentido que las hormigas envíen respuestas si estamos esperando
        // por hormigas
        candadoEsperaRespuesta.lock();
        try {
            if (hormigasEsperandoRespuesta > 0) {
                // Solo contar una respuesta por espera
                if (!hormigasQueRespondieron.contains(nombre)) {
                    hormigasQueRespondieron.add(nombre);

                    // Avisar a quien espera por la respuesta de todas las hormigas si esta fue la última
                    if (--hormigasEsperandoRespuesta == 0) {
                        condicionEsperaRespuesta.signal();
                    }
                }
            } else {
                // Esto no debería de ocurrir: se espera recibir la respuesta solo si estamos esperando la respuesta de al menos una hormiga
                mundo.getLogger().log(Level.WARNING, "La hormiga " + nombre + " ha respondido, pero el entorno no esperaba que lo hiciese en este momento. Esto puede ser síntoma de algún problema.");
            }
        } finally {
            candadoEsperaRespuesta.unlock();
        }
    }
}