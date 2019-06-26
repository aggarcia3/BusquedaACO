// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO;

import java.util.logging.Level;
import java.util.concurrent.Callable;

import static java.lang.System.currentTimeMillis;

/**
 * Una fabricación pura que se responsabiliza del correcto avance del tiempo
 * discreto del sistema, en coordinación con el {@link Mundo}. Se ejecuta en
 * otro hilo. El comportamiento señalizador del siguiente instante de tiempo
 * es completamente personalizable. Proporciona más control sobre la variabilidad
 * del periodo de avance que la interfaz ScheduledExecutorService de Java.
 * @author Alejandro González García
 */
final class Tiempo implements Runnable {
    /**
     * El mundo sobre el que esta clase brindará sus servicios.
     */
    private final Mundo mundo;
    /**
     * El hilo encargado de realizar el avance periódico del tiempo.
     */
    private final Thread hilo;
    /**
     * Contiene el código a ejecutar en su método {@link Callable#call} para notificar
     * el paso del tiempo.
     */
    private final Callable<Boolean> tick;
    /**
     * Los milisegundos que pasan entre dos instantes de tiempo discretos.
     */
    private volatile int msPeriodo;
    /**
     * Toma el valor verdadero si se ha iniciado el avance del tiempo discreto,
     * falso en caso contrario.
     */
    private volatile boolean iniciado = false;

    /**
     * Crea un nuevo objeto responsable de ejecutar el avance periódico del tiempo.
     * @param mundo El mundo (entorno) sobre el que se pasará el tiempo.
     * @param tick El código a ejecutar para notificar el paso del tiempo. Si devuelve
     * verdadero, se detendrá la ejecución del avance periódico del tiempo.
     * @param msPeriodo Los milisegundos que deben transcurrir entre dos instantes
     * de tiempo consecutivos.
     * @throws IllegalArgumentException Si msPeriodo es estrictamente menor que 0,
     * tick es nulo o mundo es nulo.
     */
    Tiempo(final Mundo mundo, final Callable<Boolean> tick, final int msPeriodo) {
        if (mundo == null) {
            throw new IllegalArgumentException("El tiempo de un mundo nulo no puede avanzar.");
        }
        if (tick == null) {
            throw new IllegalArgumentException("No tiene sentido que la tarea a ejecutar para notificar el paso del tiempo sea nula.");
        }

        this.mundo = mundo;
        this.tick = tick;
        this.hilo = new Thread(this, "Tiempo");
        hilo.setDaemon(true);

        setPeriodo(msPeriodo);
    }

    /**
     * Cambia el periodo que transcurre entre dos instantes de tiempo consecutivos. La acción
     * de este método tendrá efecto tras la próxima ejecución de {@link tick}.
     * @param msPeriodo El nuevo periodo a establecer.
     * @throws IllegalArgumentException Si msPeriodo es estrictamente menor que 0.
     */
    void setPeriodo(final int msPeriodo) {
        if (msPeriodo < 0) {
            throw new IllegalArgumentException("No puede pasar un tiempo negativo entre dos instantes de tiempo discretos.");
        }
        this.msPeriodo = msPeriodo;
    }

    /**
     * Comienza el avance periódico del tiempo del SMA, en otro hilo de ejecución. Si el hilo actual ya
     * ha comenzado el avance del tiempo del SMA, este método no tiene efecto. Este método no es seguro
     * para ser ejecutado de manera concurrente. El avance periódico del tiempo no se puede reiniciar
     * una vez parado.
     * @return La instancia del objeto sobre el que se llama a este método.
     */
    Tiempo iniciar() {
        if (!iniciado) {
            hilo.start();
            iniciado = true;
        }
        return this;
    }

    /**
     * Detiene de manera ordenada el avance del tiempo. Tras la ejecución de este método
     * se ha hecho un mejor intento para detener la ejecución de {@link tick}.
     */
    void parar() {
        if (iniciado) {
            // Avisar al hilo para que se detenga
            hilo.interrupt();

            // Esperar hasta 10 segundos para que acate la orden
            try {
                hilo.join(10000);
            } catch (InterruptedException ignorar) {}

            // Si el hilo sigue ejecutándose pese a todo, avisar de ello
            // (como es un hilo de fondo, daemon, no debería de afectar al cierre de la JVM)
            if (!hilo.getState().equals(Thread.State.TERMINATED)) {
                mundo.getLogger().log(Level.WARNING, "Se intentó parar el hilo responsable de avanzar el tiempo en el SMA, pero no respondió en 10 segundos. Continuando de todas formas.");
            }
        }
    }

    @Override
    public void run() {
        final Thread esteHilo = Thread.currentThread();
        boolean seDeseaParar = false;

        while (!seDeseaParar && !esteHilo.interrupted()) {
            // Avanzamos al siguiente instante, observando lo que hemos tardado en procesar
            // el tick
            mundo.getLogger().log(Level.INFO, "Comienza la simulación del siguiente instante de tiempo.");
            final long timestampInicioTick = currentTimeMillis();
            try {
                seDeseaParar = tick.call();
            } catch (Exception exc) {
                mundo.getLogger().log(Level.WARNING, "Ha ocurrido un error no esperado mientras se ejecutaba la tarea de avance de tiempo del entorno. Detalles: ", exc);
            }
            final long timestampFinTick = currentTimeMillis();
            mundo.getLogger().log(Level.INFO, "Finaliza la simulación del siguiente instante de tiempo.");

            // Dormirnos si nos sobra tiempo hasta el siguiente tick, y no queremos pararnos
            if (!seDeseaParar && timestampFinTick - timestampInicioTick < msPeriodo) {
                try {
                    esteHilo.sleep(msPeriodo - (timestampFinTick - timestampInicioTick));
                } catch (InterruptedException exc) {
                    // Restaurar bandera de interrupción, para salir del bucle
                    esteHilo.interrupt();
                } catch (IllegalArgumentException ignorar) {
                    // No interrumpir la ejecución aunque el resultado de la operación
                    // sea negativo (overflow). Preferimos hacer el siguiente tick a destiempo
                }
            } else if (!seDeseaParar && msPeriodo > 0) {
                // Avisar si la ejecución fue lenta o muy ajustada, lo que puede generar
                // retrasos en la simulación
                mundo.getLogger().log(Level.WARNING, "Se ha tardado tanto tiempo o más en avanzar al siguiente instante de tiempo como el periodo de avance. ¿Está sobrecargado el sistema?");
            }
        }
    }
}