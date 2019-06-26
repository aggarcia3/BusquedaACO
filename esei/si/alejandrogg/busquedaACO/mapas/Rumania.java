// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO.mapas;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;

import esei.si.alejandrogg.busquedaACO.GrafoCarreteras;
import esei.si.alejandrogg.busquedaACO.Localidad;
import esei.si.alejandrogg.busquedaACO.Carretera;

/**
 * Un grafo de carreteras parcial de Rumanía, idéntico al expuesto como ejemplo en
 * Peter Norvig &amp; Stuart J. Russell: "Inteligencia artificial: Un enfoque moderno".
 * @author Alejandro González García
 */
final class Rumania extends BaseMapa {
    /**
     * El nombre identificativo y unívoco del grafo de carreteras.
     * Escapamos la i minúscula con tilde para evitar inconsistencias dependiendo
     * del juego de caracteres que use el compilador para interpretar la cadena.
     */
    static final String NOMBRE = "Ruman\u00EDa";

	/**
	 * Localidades que forman parte del grafo (nodos).
	 */
	private static final Localidad arad = new Localidad("Arad", 65, 120);
	private static final Localidad bucarest = new Localidad("Bucarest", 525, 380);
	private static final Localidad craiova = new Localidad("Craiova", 305, 440);
	private static final Localidad dobreta = new Localidad("Dobreta", 165, 435);
	private static final Localidad eforie = new Localidad("Eforie", 675, 425);
	private static final Localidad fagaras = new Localidad("Fagaras", 388, 195);
	private static final Localidad giurgiu = new Localidad("Giurgiu", 545, 460);
	private static final Localidad hirsova = new Localidad("Hirsova", 700, 320);
	private static final Localidad iasi = new Localidad("Iasi", 630, 90);
	private static final Localidad lugoj = new Localidad("Lugoj", 170, 280);
	private static final Localidad mehadia = new Localidad("Mehadia", 175, 340);
	private static final Localidad neamt = new Localidad("Neamt", 535, 60);
	private static final Localidad oradea = new Localidad("Oradea", 130, 25);
	private static final Localidad pitesti = new Localidad("Pitesti", 365, 280);
	private static final Localidad rimnicu_vilcea = new Localidad("Rimnicu Vilcea", 295, 255);
	private static final Localidad sibiu = new Localidad("Sibiu", 235, 150);
	private static final Localidad timisoara = new Localidad("Timisoara", 65, 230);
	private static final Localidad urziceni = new Localidad("Urziceni", 600, 375);
	private static final Localidad vaslui = new Localidad("Vaslui", 685, 185);
	private static final Localidad zerind = new Localidad("Zerind", 5, 55);

    /**
     * El número de localidades presentes en el grafo (nodos).
     */
    private static final int NUM_LOCALIDADES = 20;
	/**
	 * Localidades que forman parte del grafo (nodos).
	 */
    private static final Set<Localidad> localidades;

    /**
     * El número de carreteras presentes en el grafo (arcos).
     */
    private static final int NUM_CARRETERAS = 23;
	/**
	 * Carreteras que forman parte del grafo (arcos), en un conjunto.
	 */
	private static final Set<Carretera> carreteras;
    /**
     * Carreteras que forman parte del grafo (arcos), en un array.
     */
    private static final Carretera[] carreterasArray = new Carretera[] {
		new Carretera(oradea, zerind, 71, 15, 30),
		new Carretera(oradea, sibiu, 151, 200, 90),
		new Carretera(zerind, arad, 75, 70, 100),
		new Carretera(arad, timisoara, 118, 60, 195),
		new Carretera(arad, sibiu, 140, 100, 170),
		new Carretera(timisoara, lugoj, 111, 65, 310),
		new Carretera(lugoj, mehadia, 70, 80, 330),
		new Carretera(mehadia, dobreta, 75, 80, 390),
		new Carretera(sibiu, fagaras, 99, 280, 160),
		new Carretera(sibiu, rimnicu_vilcea, 80, 180, 220),
		new Carretera(rimnicu_vilcea, pitesti, 97, 290, 320),
		new Carretera(rimnicu_vilcea, craiova, 146, 210, 335),
		new Carretera(craiova, dobreta, 120, 195, 440),
		new Carretera(fagaras, bucarest, 211, 480, 285),
		new Carretera(pitesti, craiova, 138, 375, 390),
		new Carretera(pitesti, bucarest, 101, 405, 375),
		new Carretera(bucarest, giurgiu, 90, 425, 420),
		new Carretera(bucarest, urziceni, 85, 520, 335),
		new Carretera(urziceni, hirsova, 98, 630, 315),
		new Carretera(hirsova, eforie, 86, 665, 385),
		new Carretera(urziceni, vaslui, 142, 575, 260),
		new Carretera(vaslui, iasi, 92, 580, 155),
		new Carretera(iasi, neamt, 87, 510, 95)
	};

    static {
        // Inicializar conjunto de localidades
        final Set<Localidad> localidadesTemp = new HashSet<>(NUM_LOCALIDADES);
        localidadesTemp.add(arad);
        localidadesTemp.add(bucarest);
        localidadesTemp.add(craiova);
        localidadesTemp.add(dobreta);
        localidadesTemp.add(eforie);
        localidadesTemp.add(fagaras);
        localidadesTemp.add(giurgiu);
        localidadesTemp.add(hirsova);
        localidadesTemp.add(iasi);
        localidadesTemp.add(lugoj);
        localidadesTemp.add(mehadia);
        localidadesTemp.add(neamt);
        localidadesTemp.add(oradea);
        localidadesTemp.add(pitesti);
        localidadesTemp.add(rimnicu_vilcea);
        localidadesTemp.add(sibiu);
        localidadesTemp.add(timisoara);
        localidadesTemp.add(urziceni);
        localidadesTemp.add(vaslui);
        localidadesTemp.add(zerind);

        // Inicializar conjunto de carreteras
        final Set<Carretera> carreterasTemp = new HashSet<>(NUM_CARRETERAS);
        carreterasTemp.add(carreterasArray[0]);
        carreterasTemp.add(carreterasArray[1]);
        carreterasTemp.add(carreterasArray[2]);
        carreterasTemp.add(carreterasArray[3]);
        carreterasTemp.add(carreterasArray[4]);
        carreterasTemp.add(carreterasArray[5]);
        carreterasTemp.add(carreterasArray[6]);
        carreterasTemp.add(carreterasArray[7]);
        carreterasTemp.add(carreterasArray[8]);
        carreterasTemp.add(carreterasArray[9]);
        carreterasTemp.add(carreterasArray[10]);
        carreterasTemp.add(carreterasArray[11]);
        carreterasTemp.add(carreterasArray[12]);
        carreterasTemp.add(carreterasArray[13]);
        carreterasTemp.add(carreterasArray[14]);
        carreterasTemp.add(carreterasArray[15]);
        carreterasTemp.add(carreterasArray[16]);
        carreterasTemp.add(carreterasArray[17]);
        carreterasTemp.add(carreterasArray[18]);
        carreterasTemp.add(carreterasArray[19]);
        carreterasTemp.add(carreterasArray[20]);
        carreterasTemp.add(carreterasArray[21]);
        carreterasTemp.add(carreterasArray[22]);

        // Finalmente, inicializar los atributos estáticos con una vista no modificable de los conjuntos.
        // Esto garantiza que el mapa no será modificado en tiempo de ejecución y facilita la
        // sincronización entre hilos
        localidades = Collections.unmodifiableSet(localidadesTemp);
        carreteras = Collections.unmodifiableSet(carreterasTemp);
    }

    Rumania() {
        super(NUM_CARRETERAS);

        // Carreteras en un sentido
        // Se recomienda generar estas instrucciones con sustituciones aplicadas
        // sobre expresiones regulares, para evitar errores de transcripción
        carreterasLocalidades.put(new SimpleEntry<>(oradea, zerind), carreterasArray[0]);
        carreterasLocalidades.put(new SimpleEntry<>(oradea, sibiu), carreterasArray[1]);
        carreterasLocalidades.put(new SimpleEntry<>(zerind, arad), carreterasArray[2]);
        carreterasLocalidades.put(new SimpleEntry<>(arad, timisoara), carreterasArray[3]);
        carreterasLocalidades.put(new SimpleEntry<>(arad, sibiu), carreterasArray[4]);
        carreterasLocalidades.put(new SimpleEntry<>(timisoara, lugoj), carreterasArray[5]);
        carreterasLocalidades.put(new SimpleEntry<>(lugoj, mehadia), carreterasArray[6]);
        carreterasLocalidades.put(new SimpleEntry<>(mehadia, dobreta), carreterasArray[7]);
        carreterasLocalidades.put(new SimpleEntry<>(sibiu, fagaras), carreterasArray[8]);
        carreterasLocalidades.put(new SimpleEntry<>(sibiu, rimnicu_vilcea), carreterasArray[9]);
        carreterasLocalidades.put(new SimpleEntry<>(rimnicu_vilcea, pitesti), carreterasArray[10]);
        carreterasLocalidades.put(new SimpleEntry<>(rimnicu_vilcea, craiova), carreterasArray[11]);
        carreterasLocalidades.put(new SimpleEntry<>(craiova, dobreta), carreterasArray[12]);
        carreterasLocalidades.put(new SimpleEntry<>(fagaras, bucarest), carreterasArray[13]);
        carreterasLocalidades.put(new SimpleEntry<>(pitesti, craiova), carreterasArray[14]);
        carreterasLocalidades.put(new SimpleEntry<>(pitesti, bucarest), carreterasArray[15]);
        carreterasLocalidades.put(new SimpleEntry<>(bucarest, giurgiu), carreterasArray[16]);
        carreterasLocalidades.put(new SimpleEntry<>(bucarest, urziceni), carreterasArray[17]);
        carreterasLocalidades.put(new SimpleEntry<>(urziceni, hirsova), carreterasArray[18]);
        carreterasLocalidades.put(new SimpleEntry<>(hirsova, eforie), carreterasArray[19]);
        carreterasLocalidades.put(new SimpleEntry<>(urziceni, vaslui), carreterasArray[20]);
        carreterasLocalidades.put(new SimpleEntry<>(vaslui, iasi), carreterasArray[21]);
        carreterasLocalidades.put(new SimpleEntry<>(iasi, neamt), carreterasArray[22]);

        // Carreteras en el sentido contrario
        // Expresión regular para generar estas instrucciones rápidamente a partir de las
        // carreteras en un sentido (asume entradas bien formadas):
        // carreterasLocalidades.put\(new SimpleEntry<>\((.+), (.+)\), carreteras\[(.+)\]\);
        carreterasLocalidades.put(new SimpleEntry<>(zerind, oradea), carreterasArray[0]);
        carreterasLocalidades.put(new SimpleEntry<>(sibiu, oradea), carreterasArray[1]);
        carreterasLocalidades.put(new SimpleEntry<>(arad, zerind), carreterasArray[2]);
        carreterasLocalidades.put(new SimpleEntry<>(timisoara, arad), carreterasArray[3]);
        carreterasLocalidades.put(new SimpleEntry<>(sibiu, arad), carreterasArray[4]);
        carreterasLocalidades.put(new SimpleEntry<>(lugoj, timisoara), carreterasArray[5]);
        carreterasLocalidades.put(new SimpleEntry<>(mehadia, lugoj), carreterasArray[6]);
        carreterasLocalidades.put(new SimpleEntry<>(dobreta, mehadia), carreterasArray[7]);
        carreterasLocalidades.put(new SimpleEntry<>(fagaras, sibiu), carreterasArray[8]);
        carreterasLocalidades.put(new SimpleEntry<>(rimnicu_vilcea, sibiu), carreterasArray[9]);
        carreterasLocalidades.put(new SimpleEntry<>(pitesti, rimnicu_vilcea), carreterasArray[10]);
        carreterasLocalidades.put(new SimpleEntry<>(craiova, rimnicu_vilcea), carreterasArray[11]);
        carreterasLocalidades.put(new SimpleEntry<>(dobreta, craiova), carreterasArray[12]);
        carreterasLocalidades.put(new SimpleEntry<>(bucarest, fagaras), carreterasArray[13]);
        carreterasLocalidades.put(new SimpleEntry<>(craiova, pitesti), carreterasArray[14]);
        carreterasLocalidades.put(new SimpleEntry<>(bucarest, pitesti), carreterasArray[15]);
        carreterasLocalidades.put(new SimpleEntry<>(giurgiu, bucarest), carreterasArray[16]);
        carreterasLocalidades.put(new SimpleEntry<>(urziceni, bucarest), carreterasArray[17]);
        carreterasLocalidades.put(new SimpleEntry<>(hirsova, urziceni), carreterasArray[18]);
        carreterasLocalidades.put(new SimpleEntry<>(eforie, hirsova), carreterasArray[19]);
        carreterasLocalidades.put(new SimpleEntry<>(vaslui, urziceni), carreterasArray[20]);
        carreterasLocalidades.put(new SimpleEntry<>(iasi, vaslui), carreterasArray[21]);
        carreterasLocalidades.put(new SimpleEntry<>(neamt, iasi), carreterasArray[22]);
    }

	@Override
	public Set<Carretera> carreteras() {
		return carreteras;
	}

	@Override
	public Set<Localidad> localidades() {
		return localidades;
	}

    @Override
    public String getNombre() {
        return NOMBRE;
    }
}