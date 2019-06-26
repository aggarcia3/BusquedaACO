// Código de acción interna para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO.accionesInternas;

import java.util.List;
import java.util.ArrayList;

import jason.JasonException;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.InternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.ListTerm;

import esei.si.alejandrogg.busquedaACO.Mundo;
import esei.si.alejandrogg.busquedaACO.agentes.ArquitecturaHormiga;

/**
 * Obtiene una muestra aleatoria de una distribución discreta dada, definida por su función
 * masa de probabilidad.
 * @serial exclude
 */
public class muestraDistribucionDiscreta extends DefaultInternalAction {
    /**
     * La única instancia existente en la JVM de esta acción interna (patrón singleton).
     */
    private static InternalAction instancia = null;

    /**
     * Obtiene, creando si y solo si es necesario, la única instancia de esta clase.
     * @return La única instancia de esta clase.
     */
    public static InternalAction create() {
        if (instancia == null) {
            instancia = new muestraDistribucionDiscreta();
        }
        return instancia;
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public int getMaxArgs() {
        return 2;
    }

    @Override
    protected void checkArguments(final Term[] args) throws JasonException {
        super.checkArguments(args);

        if (!args[0].isList()) {
            throw JasonException.createWrongArgument(this, "El primer argumento debe de ser una función masa de probabilidad.");
        }
    }

    @Override
    public Object execute(final TransitionSystem ts, final Unifier un, final Term[] args) throws Exception {
        boolean toret = true;

        checkArguments(args);

        if (args[0] instanceof ListTerm) {
            // Lista para albergar los valores de la función masa de probabilidad
            final List<Double> f = new ArrayList<>(Mundo.get().getNumeroLocalidades()); // Como mucho, tendremos n valores para las n localidades

            // Reinterpretar los términos de la lista como números, si es posible, y añadirlos
            // a la lista de valores de la función masa de probabilidad
            for (Term t : (ListTerm) args[0]) {
                if (t.isNumeric()) {
                    f.add(((NumberTerm) t).solve());
                } else {
                    throw new JasonException("Un elemento de la función masa de probabilidad no es un número.");
                }
            }

            // Unificar el segundo parámetro con el resultado de la muestra
            toret = un.unifies(new NumberTermImpl(new DistribProbabilidadDiscreta(f).next()), args[1]);
        } else {
            // Esto no debería de ocurrir bajo condiciones normales de ejecución
            throw new AssertionError();
        }

        return toret;
    }
}