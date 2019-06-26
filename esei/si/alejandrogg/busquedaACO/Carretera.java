// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

/**
 * Modela una carretera, que se trata de un nombre más semántico para un arco
 * etiquetado y no dirigido de un grafo, sobre el que hormigas pueden depositar
 * feromonas (que se puede considerar como otro parámetro de la etiqueta).
 * @author Alejandro González García
 */
public final class Carretera {
	/**
	 * La localidad que está en un extremo de la carretera.
	 */
	private final Localidad a;
	/**
	 * La localidad que está en otro extremo de la carretera.
	 */
	private final Localidad b;
	/**
	 * La distancia entre ambas localidades implicadas (etiqueta del grafo).
	 */
	private final double distancia;
	/**
	 * La coordenada X (abscisa) del punto en la imagen que representa el grafo
	 * al que pertenece donde se mostrará información sobre la carretera.
	 * Tal imagen está asociada con su vista.
	 */
	private final int infoX;
	/**
	 * La coordenada Y (ordenada) del punto en la imagen que representa el grafo
	 * al que pertenece donde se mostrará información sobre la carretera.
	 * Tal imagen está asociada con su vista.
	 */
	private final int infoY;
	/**
	 * La cantidad de feromona depositada en esta carretera por las hormigas
	 * (agentes).
	 */
	private final AtomicReference<Double> nivelFeromona = new AtomicReference<>(Mundo.FEROMONA_INICIAL);

	/**
	 * Crea una nueva carretera.
	 * @param a La localidad que está en un extremo de la carretera.
	 * @param b La localidad que está en otro extremo de la carretera.
	 * @param distancia La distancia entre ambas localidades implicadas (etiqueta del grafo).
	 * @param infoX La coordenada X (abscisa) del punto donde se mostrará información de esta carretera.
	 * @param infoY La coordenada Y (ordenada) del punto donde se mostrará información de esta carretera.
	 * @throws NullPointerException Si alguno de los parámetros a o b son nulos.
	 * @throws IllegalArgumentException Si la distancia es menor que 0.
	 */
	public Carretera(final Localidad a, final Localidad b, final double distancia, final int infoX, final int infoY) {
		if (a == null || b == null) {
			throw new NullPointerException("Las localidades que conforman una carretera no pueden ser nulas.");
		}
		if (distancia < 0) {
			throw new IllegalArgumentException("La distancia no puede ser menor que 0.");
		}
		if (infoX < 0 || infoY < 0) {
			throw new IllegalArgumentException("Alguna coordenada es negativa, cuando no puede serlo.");
		}
		this.a = a;
		this.b = b;
		this.distancia = distancia;
		this.infoX = infoX;
		this.infoY = infoY;
	}

	/**
	 * Obtiene las dos localidades por las que pasa esta carretera.
	 * @return Las devandichas localidades.
	 */
	Localidad[] getLocalidades() {
		return new Localidad[]{ a, b };
	}

	/**
	 * Obtiene la longitud de la propia carretera; es decir, la distancia para ir
	 * de una localidad a la otra usando esta carretera.
	 * @return La devandicha distancia.
	 */
	double getDistancia() {
		return distancia;
	}

	/**
	 * Obtiene la coordenada X (abscisa) del punto donde se mostrará información de esta carretera.
	 * @return La devandicha coordenada.
	 */
	int getInfoX() {
		return infoX;
	}

	/**
	 * Obtiene la coordenada Y (ordenada) del punto donde se mostrará información de esta carretera.
	 * @return La devandicha coordenada.
	 */
	int getInfoY() {
		return infoY;
	}

    /**
     * Establece el nivel de feromona depositado actualmente en esta carretera, a partir
     * de un cálculo realizado sobre el nivel de feromona anterior. Dicho cálculo se
     * realiza de manera atómica: ningún otro hilo podrá ver resultados de la operación
     * hasta que se complete, y se garantiza que otros hilos lo verán en cuanto se complete.
     * @param gestorPercepciones El gestor de percepciones a usar para informar a las hormigas
     * del cambio del nivel de feromona. Si es nulo, no se informará a las hormigas de ello.
     * @param operacion La operación a realizar sobre el nivel de feromona anterior.
     */
    public void actualizarNivelFeromona(final GestorPercepciones gestorPercepciones, final UnaryOperator<Double> operacion) {
        if (gestorPercepciones != null) {
            gestorPercepciones.actualizarNivelFeromona(this, nivelFeromona.updateAndGet(operacion));
        } else {
            nivelFeromona.updateAndGet(operacion);
        }
    }

	/**
	 * Obtiene el nivel de feromona depositada actualmente en esta carretera.
	 * @return El descrito dato.
	 */
	double getNivelFeromona() {
		return nivelFeromona.get();
	}

    /**
     * {@inheritDoc} Dos carreteras son iguales si sus localidades inicio y destino son iguales,
     * sin importar el orden.
     */
    @Override
    public boolean equals(final Object otra) {
        boolean toret = otra instanceof Carretera;

        if (toret) {
            final Carretera otraCarretera = (Carretera) otra;
            toret = a.equals(otraCarretera.a) && b.equals(otraCarretera.b) || a.equals(otraCarretera.b) && b.equals(otraCarretera.a);
        }

        return toret;
    }

    @Override
    public int hashCode() {
        return a.hashCode() ^ b.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(a.toString());

        sb.append(", ");
        sb.append(b);
        sb.append(" (");
        sb.append(distancia);
        sb.append(")");

        return sb.toString();
    }
}
