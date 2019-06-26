// Agente hormiga en proyecto BusquedaACO.mas2j

/* ********************** */
/* Reglas de razonamiento */
/* ********************** */

// Las carreteras tienen dos sentidos (grafo no dirigido)
carretera(A, B, Distancia, IntFeromona) :- carretera(B, A, Distancia, IntFeromona)[source(percept)].

// He visitado la localidad en la que estoy
localidadVisitada(L) :- estoyEn(L).

// He visitado la localidad de inicio
localidadVisitada(L) :- localidadInicio(L).

// He llegado al destino si estoy en él
heLlegado :-
	localidadDestino(Destino) &
	estoyEn(Destino).

// Las probabilidades de elección son función de las feromonas depositadas y de
// la distancia en línea recta al destino
probabilidadEleccion(Distancia, IntFeromona, IntFeromona ** Alfa + (1 / Distancia) ** Beta) :-
	importanciaFeromona(Alfa) &
	importanciaDistancia(Beta).

// La probabilidad de escoger una determinada carretera se calcula a partir de
// varias probabilidades de elección
probabilidadEleccionCarretera(A, B, P1 / P2) :-
	carretera(A, B, Distancia, IntFeromona) &
	probabilidadEleccion(Distancia, IntFeromona, P1) &
	sumaTodasProbabilidadesEleccion(A, P2).
// Calcula la suma de todas las probabilidades de elección posibles a partir de
// la localidad actual. Para evitar tener que iterar dos veces sobre listas, se
// aprovechan efectos colaterales sobre la base de conocimiento, en la que se va
// guardando el resultado parcial obtenido hasta el momento, según se va vaciando
// la resolvente
sumaTodasProbabilidadesEleccion(LocalidadActual, P) :-
	.asserta(resultadoSumaTodasProbabilidadesEleccion(0)) &
	.findall(_, sumaTodasProbabilidadesEleccion_impl(LocalidadActual), _) &
	resultadoSumaTodasProbabilidadesEleccion(P) &
	.abolish(resultadoSumaTodasProbabilidadesEleccion(_)).
sumaTodasProbabilidadesEleccion_impl(LocalidadActual) :-
	carretera(LocalidadActual, B, Distancia, IntFeromona) &
	not localidadVisitada(B) &
	probabilidadEleccion(Distancia, IntFeromona, P) &
	resultadoSumaTodasProbabilidadesEleccion(PAnt) &
	.abolish(resultadoSumaTodasProbabilidadesEleccion(PAnt)) &
	.asserta(resultadoSumaTodasProbabilidadesEleccion(PAnt + P)).

// Las localidades candidatas son las adyacentes a la actual que no haya visitado
// aún, teniendo una probabilidad asociada de que las escoja
localidadCandidata(L, Probabilidad) :-
	estoyEn(Actual) &
	carretera(Actual, L, _, _) &
	not localidadVisitada(L) &
	probabilidadEleccionCarretera(Actual, L, Probabilidad) &
	// Optimización: como solo resolvemos este predicado para obtener todas
	// todas las localidades siguientes, ir asociando índices crecientes a
	// cada localidad obtenida en orden. Así luego podemos interpretar de manera
	// más rápida los resultados del muestreo de una distribución de probabilidad,
	// ahorrándonos iterar dos veces sobre listas
	ultimoIndiceTempLocalidad(I) &
	.asserta(indiceTempLocalidadALocalidad(I, L)) &
	.abolish(ultimoIndiceTempLocalidad(I)) &
	.asserta(ultimoIndiceTempLocalidad(I + 1)).

// Escoger la localidad a la que desplazarse en el siguiente instante de tiempo
// siguiendo una distribución de probabilidad discreta, donde la probabilidad de
// elección de una alternativa u otra depende de varios factores
localidadSiguiente(Siguiente) :-
	// Si he llegado a la localidad destino, no hay una siguiente
	not heLlegado &
	.abolish(ultimoIndiceTempLocalidad(_)) & // Por si se abortó la ejecución de la regla antes de llamar a la función interna
	.asserta(ultimoIndiceTempLocalidad(0)) &
	// Obtener la función masa de probabilidad a partir de las probabilidades individuales
	// de las localidades candidatas
	.findall(P, localidadCandidata(_, P), FuncMasaProb) &
	// Antes de llamar a nuestra función interna, ver si la función masa de probabilidad
	// está definida (es decir, hay localidades candidatas). Así evitamos mostrar un feo
	// error en la consola del SMA. No usamos .length porque es menos eficiente
	ultimoIndiceTempLocalidad(I) &
	I > 0 &
	// Obtener una muestra de la distribución y convertirla a una localidad eficientemente
	esei.si.alejandrogg.busquedaACO.accionesInternas.muestraDistribucionDiscreta(FuncMasaProb, Id) &
	indiceTempLocalidadALocalidad(Id, Siguiente) &
	.abolish(indiceTempLocalidadALocalidad(_, _)) &
	.abolish(ultimoIndiceTempLocalidad(_)).

// Recordar la nueva localidad que forma parte del camino recorrido, añadiéndola
// a la lista expresada como diferencias de listas que tenemos
anadirACamino(Localidad) :-
	camino_dl(CaminoAnt) &
	append_dl(CaminoAnt, difListas([Localidad|Cdr], Cdr), NuevoCamino) &
	.abolish(camino_dl(CaminoAnt)) &
	.asserta(camino_dl(NuevoCamino)).
// Si esta es la primera localidad del camino que hemos recorrido, inicializar el
// camino recorrido
anadirACamino(Localidad) :-
	not camino_dl(_) &
	.asserta(camino_dl(difListas([Localidad|CdrInicio], CdrInicio))).

// El camino que he recorrido es el resultado de convertir el camino expresado
// como diferencias de listas a una lista convencional. Esta operación no es
// destructiva (es decir, se pueden seguir añadiendo localidades al camino sin
// coste de tiempo adicional)
camino(C) :-
	camino_dl(Camino_DL) &
	difListasAListaCerrada(Camino_DL, C).
camino([]) :- not camino_dl(_).

/* ***************** */
/* Reglas auxiliares */
/* ***************** */

// Añade los elementos de la segunda lista al final de la primera, estando ambas
// listas expresadas como diferencias de listas, por lo que la complejidad de la
// operación es O(1)
append_dl(difListas(Inicio1, Fin1), difListas(Fin1, Fin2), difListas(Inicio1, Fin2)).

// Cierra una lista expresada como diferencia de listas, de forma que se pueda
// interpretar a todos los efectos como una lista convencional
difListasAListaCerrada(difListas([Car|Cdr], []), [Car|Cdr]).

/* ****** */
/* Planes */
/* ****** */

// Si hay una localidad siguiente a la que desplazarme, hacerlo
+!avanzar : not enHormiguero & localidadSiguiente(Siguiente) <-
	// Obtener nombres de localidades implicadas
	?estoyEn(Actual);
	?localidadANombre(Actual, NombreActual);
	?localidadANombre(Siguiente, NombreSiguiente);

	// Recordar la elección que he tomado, para tenerla en cuenta en el futuro
	+localidadVisitada(Siguiente);
	-+estoyEn(Siguiente);
	?anadirACamino(Siguiente);

	if (heLlegado) {
		.print("He llegado a ", NombreSiguiente, ". Vuelvo al hormiguero.");
		?camino(C);
		+enHormiguero;
		irAHormiguero(C);
	} else {
		.print("Me desplazo de ", NombreActual, " a ", NombreSiguiente, ".");
		irA(Siguiente);
	}.
// No hay una localidad siguiente, pero sí tengo un camino formado, y no he
// llegado al destino. Eso significa que he llegado a una situación en la que
// no puedo seguir avanzando, así que vuelvo al hormiguero a reponer fuerzas
+!avanzar : not enHormiguero & camino(C) <-
	.print("No puedo avanzar. Vuelvo al hormiguero.");
	+enHormiguero;
	irAHormiguero(C).
// Si estoy en el hormiguero, es porque he acabado mi labor por el momento
+!avanzar : enHormiguero <-
	.print("Ya estoy en el hormiguero.");
	listaParaContinuar.
// Si ninguno de los planes anteriores sirve, entonces estoy bloqueada y no sé
// qué hacer
+!avanzar <-
	.print("Desconozco como avanzar en mi caso.");
	listaParaContinuar.

/* ******* */
/* Eventos */
/* ******* */

// Cuando llegue el siguiente instante de tiempo discreto, avanzar a la siguiente
// localidad
+pasoTiempo(Id)[source(percept)] <-
	-pasoTiempo(Id);
	!avanzar.

// Si el entorno me dice la localidad de inicio, y no sé dónde estoy, añadirla
// al camino recorrido y considerar que estoy ahí
+localidadInicio(L)[source(percept)] : not estoyEn(_) <-
	+estoyEn(L);
	?anadirACamino(L).
// Si el entorno me dice la localidad de inicio, pero sé dónde estoy, solamente
// tener en cuenta el dato
+localidadInicio(_)[source(percept)].

// Cuando el entorno me diga que llegó un nuevo ciclo, olvidar lo que estaba
// haciendo, considerar que estoy en la localidad de inicio, y avisarle de que
// estoy lista para empezar a buscar caminos
+siguienteCiclo(Id)[source(percept)] : localidadInicio(L) <-
	-siguienteCiclo(Id);
	-+estoyEn(L);
	.abolish(localidadVisitada(_));
	.abolish(camino_dl(_));
	?anadirACamino(L);
	-enHormiguero;
	listaParaContinuar.
