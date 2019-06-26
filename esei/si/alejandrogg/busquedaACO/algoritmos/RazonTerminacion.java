// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO.algoritmos;

/**
 * Un enumerado que representa las posibles causas de terminación de un algoritmo de búsqueda
 * basado en ACO.
 * @author Alejandro González García
 */
public enum RazonTerminacion {
    /**
     * El algoritmo ha terminado, pero no se sabe por qué, o ninguna otra razón es apropiada.
     * @deprecated Ningún código debería de usar esta causa normalmente. Se provee a modo de
     * "placeholder" para pruebas, depuración u otros casos similares.
     */
    @Deprecated
    DESCONOCIDA {
        @Override
        public String toString() {
            return "causa de terminación desconocida";
        }
    },
    /**
     * El algoritmo ha terminado porque todas las hormigas han recorrido el mismo camino
     * en el ciclo actual.
     */
    ESTANCAMIENTO {
        @Override
        public String toString() {
            return "comportamiento estancado";
        }
    },
    /**
     * El algoritmo ha terminado porque ha consumido el número de ciclos máximos que podía hacer.
     */
    CICLOS_CONSUMIDOS {
        @Override
        public String toString() {
            return "ciclos consumidos";
        }
    }
}