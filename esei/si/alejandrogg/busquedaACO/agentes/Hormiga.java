package esei.si.alejandrogg.busquedaACO.agentes;

import java.util.List;

import jason.asSemantics.Agent;
import jason.asSemantics.Message;

/**
 * Modela un agente hormiga, que es muy similar a un agente convencional,
 * pero no acepta comunicación por mensajes de otros agentes, y tampoco
 * se deja matar por sus congéneres.
 * @author Alejandro González García
 */
public final class Hormiga extends Agent {
    @Override
    public boolean socAcc(final Message m) {
        return false;
    }

    @Override
    public boolean killAcc(final String agente) {
        return !agente.startsWith("hormiga");
    }
}