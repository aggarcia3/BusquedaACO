// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO;

import java.util.logging.Level;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabricación pura que se responsabiliza de mantener el control sobre
 * qué hormiga se encuentra en qué localidad en todo momento.
 * @author Alejandro González García
 */
final class LocalizadorHormigas {
    /**
     * El mundo asociado a este localizador de hormigas, sobre el que brinda sus servicios.
     */
    private final Mundo mundo;
    /**
     * El mapa que relaciona a cada hormiga con la localidad en la que se encuentra.
     */
    private final Map<String, Localidad> ubicacionHormiga = new ConcurrentHashMap<>();

    /**
     * Crea un nuevo localizador de hormigas, asociado con un determinado mundo.
     * @param mundo El mundo asociado con el nuevo localizador.
     * @throws IllegalArgumentException Si el parámetro es nulo.
     */
    LocalizadorHormigas(final Mundo mundo) {
        if (mundo == null) {
            throw new IllegalArgumentException("No se puede crear un localizador de hormigas que localice hormigas de un mundo nulo.");
        }
        this.mundo = mundo;
    }

    /**
     * Registra el movimiento de una hormiga de la localidad en la que esté (si está en alguna)
     * a otra localidad. Este método es seguro en caso de ser llamado por varios hilos de manera concurrente.
     * @param hormiga La hormiga cuyo movimiento registrar.
     * @param localidad La localidad a la que pasa a estar la hormiga.
     * @throws IllegalArgumentException Si algún parámetro es nulo.
     */
    void mover(final String hormiga, final Localidad localidad) {
        if (hormiga == null) {
            throw new IllegalArgumentException("No se puede colocar una hormiga de nombre nulo en una localidad.");
        }
        if (localidad == null) {
            throw new IllegalArgumentException("No se puede colocar una hormiga en una localidad nula.");
        }

        // Javadoc del método compute: "The entire method invocation is performed atomically".
        // De esta forma, nos ahorramos sincronización externa para hacer atómicas operaciones no atómicas:
        // comprobar la localidad anterior en el mapa, y si es distinta de la nueva, borrar y añadir una hormiga
        // en las localidades correspondientes
        ubicacionHormiga.compute(hormiga, (final String ignorada, final Localidad locAnterior) -> {
            if (locAnterior != localidad) {
                // Eliminar la hormiga de la localidad anterior
                if (locAnterior != null) {
                    locAnterior.borrarHormiga();
                    mundo.getLogger().log(Level.FINE, "La hormiga " + hormiga + " se desplaza de " + locAnterior + " a " + localidad + ".");
                }

                // Añadirla a la nueva localidad
                localidad.addHormiga();
            }

            return localidad;
        });
    }

    /**
     * Registra el movimiento de todas las hormigas de la localidad en la que estén a otra localidad.
     * Este método no es seguro en caso de ser llamado por varios hilos de manera concurrente, pues
     * no realiza sus acciones de manera atómica, y sería posible que otros hilos viesen resultados
     * parciales.
     * @param localidad La localidad a la que pasan a estar las hormigas.
     * @throws IllegalArgumentException Si algún parámetro es nulo.
     */
    void moverTodasALocalidad(final Localidad localidad) {
        if (localidad == null) {
            throw new IllegalArgumentException("No se pueden colocar hormigas en una localidad nula.");
        }

        // Las expresiones lambda fueron un buen añadido a Java 8. ¿Acaso no es elegante, conciso y expresivo lo siguiente? :)
        ubicacionHormiga.replaceAll((final String hormiga, final Localidad locAnterior) -> localidad);
    }

    /**
     * Provoca la desaparición de una hormiga del mapa, eliminando su ubicación registrada.
     * @param hormiga La hormiga a borrar del mapa.
     * @throws IllegalArgumentException Si el parámetro es nulo.
     */
    void desaparecer(final String hormiga) {
        if (hormiga == null) {
            throw new IllegalArgumentException("No se puede hacer desaparecer una hormiga nula.");
        }

        ubicacionHormiga.remove(hormiga).borrarHormiga();
    }

    /**
     * Obtiene la localidad en la que se encuentra actualmente una hormiga.
     * @param hormiga La hormiga cuya localidad actual obtener.
     * @return La localidad en la que está la hormiga. Si la hormiga no se encuentra
     * en ninguna localidad, lo cual puede pasar porque todavía no se ha registrado
     * como tal, se devuelve el valor nulo.
     * @throws IllegalArgumentException Si el parámetro es nulo.
     */
    Localidad ubicacion(final String hormiga) {
        if (hormiga == null) {
            throw new IllegalArgumentException("No se puede obtener la ubicación de una hormiga nula.");
        }

        return ubicacionHormiga.get(hormiga);
    }
}