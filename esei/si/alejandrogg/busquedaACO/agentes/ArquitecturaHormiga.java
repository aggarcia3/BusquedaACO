package esei.si.alejandrogg.busquedaACO.agentes;

import jason.architecture.AgArch;

import esei.si.alejandrogg.busquedaACO.Mundo;

/**
 * Modela la arquitectura de agente hormiga, que es muy similar a una
 * convencional, pero avisa al entorno en caso de que el agente
 * se inicie, muera o se detenga.
 * @author Alejandro González García
 */
public final class ArquitecturaHormiga extends AgArch {
    /**
     * {@inheritDoc}
     * @implNote. El método asume que el entorno es accesible desde la JVM local.
     * Para funcionar en una arquitectura distribuida, requiere ser reimplementado con
     * la <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/rmi/">API de RMI de Java</a>.
     */
    @Override
    public void init() throws Exception {
        super.init();

        // Avisar al coordinador de nuestra llegada
        Mundo.get().getCoordinadorHormigas().llegadaHormiga(getAgName());
    }

    /**
     * {@inheritDoc}
     * @implNote. El método asume que el entorno es accesible desde la JVM local.
     * Para funcionar en una arquitectura distribuida, requiere ser reimplementado con
     * la <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/rmi/">API de RMI de Java</a>.
     */
    @Override
    public void stop() {
        super.stop();

        // Avisar al coordinador de nuestra muerte
        Mundo.get().getCoordinadorHormigas().muerteHormiga(getAgName());
    }
}