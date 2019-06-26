// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO;

import java.util.concurrent.atomic.LongAdder;

/**
 * Abstrae una localidad, que es un nodo de un grafo con un nombre textual.
 * @author Alejandro González García
 */
public final class Localidad {
	/**
	 * El nombre de la localidad.
	 */
	private final String nombre;
	/**
	 * La coordenada X (abscisa) del punto central de esta localidad en la imagen que
	 * representa el grafo al que pertenece. Tal imagen está asociada con su vista.
	 */
	private final int x;
	/**
	 * La coordenada Y (ordenada) del punto central de esta localidad en la imagen que
	 * representa el grafo al que pertenece. Tal imagen está asociada con su vista.
	 */
	private final int y;
    /**
     * El sumador concurrente que mantiene el conteo del número de hormigas que hay
     * actualmente en esta localidad.
     */
    private final LongAdder nHormigas = new LongAdder();

	/**
	 * Crea una nueva localidad.
	 * @param nombre El nombre de la localidad.
	 * @param x La coordenada X (abscisa) del punto central de esta localidad.
	 * @param y La coordenada Y (ordenada) del punto central de esta localidad.
	 * @throws IllegalArgumentException Si el nombre es nulo o vacío (no contiene
	 * caracteres, o los únicos caracteres que contiene son espacios), o alguna
	 * coordenada es negativa.
	 */
	public Localidad(final String nombre, final int x, final int y) {
		if (nombre == null || nombre.trim().isEmpty()) {
			throw new IllegalArgumentException("Las localidades no pueden tener un nombre nulo o vacío.");
		}
		if (x < 0 || y < 0) {
			throw new IllegalArgumentException("Alguna coordenada es negativa, cuando no puede serlo.");
		}

		this.nombre = nombre;
		this.x = x;
		this.y = y;
	}

	/**
	 * Obtiene el nombre de esta localidad.
	 * @return El devandicho nombre.
	 */
	String getNombre() {
		return nombre;
	}

	/**
	 * Obtiene la coordenada X (abscisa) del punto central de esta localidad.
	 * @return La devandicha coordenada.
	 */
	int getX() {
		return x;
	}

	/**
	 * Obtiene la coordenada Y (ordenada) del punto central de esta localidad.
	 * @return La devandicha coordenada.
	 */
	int getY() {
		return y;
	}

    /**
     * Obtiene el número de hormigas que hay actualmente en esta localidad.
     * @return El devandicho número.
     */
    int numeroHormigas() {
        return nHormigas.intValue();
    }

    /**
     * Suma una hormiga más al número de hormigas que hay en esta localidad.
     */
    void addHormiga() {
        nHormigas.increment();
    }

    /**
     * Resta una hormiga del número de hormigas que hay en esta localidad.
     */
    void borrarHormiga() {
        nHormigas.decrement();
    }

    /**
     * {@inheritDoc} Se considera que dos localidades son iguales cuando tienen el mismo nombre.
     */
    @Override
    public boolean equals(final Object otra) {
        boolean toret = otra instanceof Localidad;

        if (toret) {
            toret = nombre.equals(((Localidad) otra).nombre);
        }

        return toret;
    }

    @Override
    public int hashCode() {
        return nombre.hashCode();
    }

    @Override
    public String toString() {
        return getNombre();
    }
}