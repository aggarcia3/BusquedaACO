// Entorno para el proyecto BusquedaACO.mas2j

package esei.si.alejandrogg.busquedaACO;

import java.util.NoSuchElementException;
import java.lang.reflect.InvocationTargetException;
import java.io.File;
import java.io.IOException;

import java.awt.image.BufferedImage;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.imageio.ImageIO;

import esei.si.alejandrogg.busquedaACO.algoritmos.RazonTerminacion;

/**
 * Define un contrato que deben de cumplir todos los objetos que puedan considerarse
 * como la representación gráfica de un grafo de carreteras mediante una ventana de
 * Swing.
 * @see GrafoCarreteras
 * @author Alejandro González García
 */
public final class VistaGrafo {
	/**
	 * La separación en píxeles a dejar entre los bordes de la ventana.
	 */
	private static final short PIXELES_MARGEN = 10;
	/**
	 * La tipografía a usar para mostrar información adicional del sistema.
	 */
	private static final Font tipografiaInformacion = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
	/**
	 * La imagen que representa el grafo sobre el que actúa el SMA.
	 */
	private final BufferedImage imagenGrafo;
	/**
	 * El grafo de carreteras del cual este objeto es su vista.
	 */
	private final GrafoCarreteras grafoCarreteras;
	/**
	 * El panel de Swing que contiene la ventana principal de la vista.
	 */
	private volatile JFrame ventanaPpal;
    /**
     * Tarea asociada a la actualización de la vista.
     */
    private final Runnable tareaActualizacion = new TareaActualizacion();

	/**
	 * Crea una nueva vista del grafo de carreteras sobre el que el sistema está trabajando.
	 * @param grafoCarreteras El grafo de carreteras del cual este objeto es su vista.
	 * @param nombreImagen El nombre del fichero de imagen que representa el grafo.
	 * @throws NullPointerException Si nombreImagen es nulo.
	 * @throws IllegalArgumentException Si el fichero de nombre nombreImagen no se puede leer como
	 * imagen, o el grafo de carreteras pasado como parámetro es nulo.
     * @throws IllegalStateException Si el objeto queda parcialmente construido debido a
     * una interrupción de los hilos que ejecuta o una excepción imprevista.
	 */
	public VistaGrafo(final GrafoCarreteras grafoCarreteras, final String nombreImagen) {
		if (grafoCarreteras == null) {
			throw new IllegalArgumentException("El grafo de carreteras asociado a la vista no puede ser nulo.");
		}
		this.grafoCarreteras = grafoCarreteras;

		// Cargar la imagen que representa el grafo a memoria, si es posible
		this.imagenGrafo = cargarImagen("res" + File.separator + nombreImagen);

        // Crear la GUI en la EDT de Swing, porque Swing no es thread-safe
        try {
            SwingUtilities.invokeAndWait(() -> {
                // Crear la etiqueta de texto que mostrará información adicional del sistema
                final JLabel informacion = new JLabel(construirHTMLInformacion());
                informacion.setFont(tipografiaInformacion);

                // Crear el panel donde se dibujará la imagen, que incluye la lógica de dibujado necesaria
                final JPanel panelImagen = new ImagenEstado();
                panelImagen.setPreferredSize(new Dimension(imagenGrafo.getWidth() + PIXELES_MARGEN * 2, imagenGrafo.getHeight() + PIXELES_MARGEN * 2));
                panelImagen.setAlignmentX(Component.CENTER_ALIGNMENT);

                // TODO: botones para controlar el sistema
                
                // Crear la ventana principal y añadirle los componentes en el orden deseado
                VistaGrafo.this.ventanaPpal = new JFrame("SMA ACO: " + nombreImagen.replaceFirst("\\..*$", "")) {
                    @Override
                    public void repaint() {
                        super.repaint();
                        // Actualizar componentes de la ventana cuando sea necesario redibujarla
                        informacion.setText(construirHTMLInformacion());
                    }
                };
                ventanaPpal.setLayout(new BoxLayout(ventanaPpal.getContentPane(), BoxLayout.Y_AXIS));
                ventanaPpal.add(panelImagen);
                ventanaPpal.add(informacion);

                // Establecer propiedades de la ventana
                try {
                    ventanaPpal.setIconImage(cargarImagen("res" + File.separator + "Hormiga.png"));
                } catch (IllegalArgumentException exc) {
                    ventanaPpal.setIconImage(imagenGrafo);
                }
                ventanaPpal.getContentPane().setBackground(Color.WHITE);
                ventanaPpal.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                ventanaPpal.pack(); // Establecer tamaño automáticamente al más apropiado
                ventanaPpal.setMinimumSize(ventanaPpal.getSize()); // No permitirle al usuario que baje de ese tamaño

                // Finalmente, hacerla visible
                ventanaPpal.setVisible(true);
            });
        } catch (InterruptedException | InvocationTargetException exc) {
            throw new IllegalStateException(exc.getMessage(), exc);
        }
	}

	/**
	 * Actualiza la información mostrada al usuario mediante esta vista.
	 * Debe de llamarse a este método cada vez que el sistema realice un paso o una
	 * acción que pueda tener efecto sobre su estado.
	 */
	void actualizar() {
        SwingUtilities.invokeLater(tareaActualizacion);
	}

	/**
	 * Lee una imagen desde un fichero en la memoria secundaria, transfiriéndola a la memoria
	 * principal.
	 * @param ruta La ruta, absoluta o relativa al directorio de trabajo actual, al fichero de imagen.
	 * @return La imagen cargada en memoria.
	 */
	private static BufferedImage cargarImagen(final String ruta) {
		BufferedImage toret;
		final File f = new File(ruta);

		// Ver si merece la pena intentar leer la imagen desde el fichero
		if (!f.exists() || !f.isFile() || !f.canRead()) {
			throw new IllegalArgumentException("Se ha intentado leer el fichero " + f.getAbsolutePath() + ", pero no existe, no es un fichero o la aplicación no tiene permiso para leerlo.");
		}

		// Cargar la imagen referenciada a memoria, si es posible
		try {
			toret = ImageIO.read(f);
		} catch (IOException exc) {
			throw new IllegalArgumentException("Excepción de E/S ocurrida durante la lectura de un fichero: " + exc.getMessage());
		}

		return toret;
	}

	/**
	 * Genera las etiquetas HTML a establecer como texto de la etiqueta de información extendida
	 * del sistema en la vista.
	 * @return El devandicho HTML.
	 */
	private static String construirHTMLInformacion() {
        final Mundo m = Mundo.get();
        final Algoritmo alg = m.getAlgoritmo();

		final StringBuilder sb = new StringBuilder("<html><body style=\"margin: auto; padding: ");
		sb.append(PIXELES_MARGEN);
		sb.append("px;\">");

		sb.append("<p><span style=\"font-weight: bold;\">Hormigas totales</span>: ");
		sb.append(m.getCoordinadorHormigas().esperandoLlegadaHormigas() ? "Esperando por hormigas..." : m.getNHormigas());
		sb.append("</p>");

		sb.append("<p><span style=\"font-weight: bold;\">Localidad de inicio</span>: ");
		sb.append(m.getLocalidadInicio().getNombre());
		sb.append("</p>");

		sb.append("<p><span style=\"font-weight: bold;\">Localidad destino</span>: ");
		sb.append(m.getLocalidadDestino().getNombre());
		sb.append("</p>");

		sb.append("<p><span style=\"font-weight: bold;\">Algoritmo</span>: ");
		sb.append(alg.getNombre());
		sb.append("</p>");

		sb.append("<p><span style=\"font-weight: bold;\">\u03B1</span>: ");
		sb.append(m.getAlfa());
		sb.append("</p>");

		sb.append("<p><span style=\"font-weight: bold;\">\u03B2</span>: ");
		sb.append(m.getBeta());
		sb.append("</p>");

		sb.append("<p><span style=\"font-weight: bold;\">Ciclo</span>: ");
		sb.append(alg.getCiclo() + "/" + alg.getCiclosMaximos());
        final RazonTerminacion razonTerminacion = alg.razonTerminacion();
        if (razonTerminacion != null) {
            sb.append(" (terminado: ");
            sb.append(razonTerminacion);
            sb.append(")");
        }
		sb.append("</p>");

		sb.append("<p><span style=\"font-weight: bold;\">Mejor camino actual</span>: ");
        try {
            final Camino camino = alg.getMejorCamino();
            sb.append(camino);
            sb.append(" (");
            sb.append(camino.distanciaTotal());
            sb.append(" km)");
        } catch (NoSuchElementException exc) {
            sb.append("\u2014");
        }
		sb.append("</p>");

		sb.append("<div></div>");

		sb.append("<p>Las etiquetas de texto con fondo marr\u00F3n representan el n\u00FAmero de hormigas actual para la localidad m\u00E1s cercana.</p>");
		sb.append("<p>Las etiquetas de texto con fondo rojo son la cantidad de feromona depositada en la carretera asociada.</p>");

		sb.append("</body></html>");

		return sb.toString();
	}

    /**
     * Tarea asociada a la actualización de la vista, a ejecutar en el hilo de
     * atención de eventos de AWT.
     * @author Alejandro González García
     */
    private final class TareaActualizacion implements Runnable {
        @Override
        public void run() {
            ventanaPpal.repaint();
        }
    }

	/**
	 * Representa un JPanel, que al ser representado muestra ciertas propiedades del estado
	 * actual del sistema en una imagen, dentro de una ventana.
	 * @author Alejandro González García
	 */
	private final class ImagenEstado extends JPanel {
		/**
		 * La tipografía a usar para mostrar el número de hormigas que hay en una localidad.
		 */
		private final Font tipografiaNumEtiqueta = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
		/**
		 * El color de fondo a usar para el rectángulo del texto de número de hormigas.
		 */
		private final Color colorFondoHormigas = new Color(166, 97, 58);
		/**
		 * El color a usar para el texto de número de hormigas.
		 */
		private final Color colorHormigas = new Color(255, 244, 241);
		/**
		 * El color de fondo a usar para el rectángulo del texto de cantidad de feromona.
		 */
		private final Color colorFondoFeromonas = Color.RED;
		/**
		 * El color a usar para el texto de cantidad de feromona.
		 */
		private final Color colorFeromonas = Color.WHITE;

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2d;
			FontRenderContext frc;
			final int xInicio = (getWidth() - imagenGrafo.getWidth()) / 2; // Para tener en cuenta el posible cambio de tamaño del componente

			// Convertir objeto Graphics a su subclase Graphics2D, pues tiene más métodos,
			// que usaremos
			if (g instanceof Graphics2D) {
				g2d = (Graphics2D) g;
				frc = g2d.getFontRenderContext();
			} else {
				throw new AssertionError("El objeto Graphics recibido no es una instancia de Graphics2D.");
			}

			// Primero, dibujar la imagen de fondo, que representa el grafo
			g2d.drawImage(imagenGrafo, xInicio, PIXELES_MARGEN, Color.WHITE, null);

			// Indicar cuántas hormigas hay actualmente en cada nodo
			for (final Localidad l : grafoCarreteras.localidades()) {
                dibujarEtiquetaTexto(
                    g2d, tipografiaNumEtiqueta, colorFondoHormigas, colorHormigas,
                    frc, Integer.toString(l.numeroHormigas()), l.getX(), l.getY(),
                    xInicio, PIXELES_MARGEN
                );
			}

			// Indicar la cantidad de feromona depositada en cada carretera
			for (final Carretera c : grafoCarreteras.carreteras()) {
				dibujarEtiquetaTexto(
					g2d, tipografiaNumEtiqueta, colorFondoFeromonas, colorFeromonas,
					frc, String.format("%+,6.4f", c.getNivelFeromona()), c.getInfoX(), c.getInfoY(),
					xInicio, PIXELES_MARGEN
				);
			}
		}

		/**
		 * Dibuja una etiqueta de texto en el JPanel, con las propiedades especificadas.
		 * @param g El objeto que permite realizar operaciones de rasterizado en el JPanel.
		 * @param tipografia La tipografía a usar para mostrar el texto.
		 * @param fondo El color de fondo de la etiqueta de texto.
		 * @param colorTexto El color del texto de la etiqueta.
		 * @param frc El contexto de renderizado de fuente del objeto de gráficos.
		 * @param texto El texto a mostrar en la etiqueta.
		 * @param x La coordenada horizontal de inicio de la línea base del texto a dibujar.
		 * @param y La coordenada vertical de inicio de la línea base del texto a dibujar.
		 * @param offX La distancia en X desde el origen de coordenadas del componente a la esquina superior izquierda
		 * de la imagen.
		 * @param offY La distancia en Y desde el origen de coordenadas del componente a la esquina superior izquierda
		 * de la imagen.
		 */
		private void dibujarEtiquetaTexto(final Graphics2D g, final Font tipografia, final Color fondo, final Color colorTexto, final FontRenderContext frc, final String texto, final int x, final int y, final int offX, final int offY) {
			final TextLayout tl = new TextLayout(texto, tipografia, frc);

			// Obtener un rectángulo, cuya esquina superior izquierda está
			// en el origen, que contiene totalmente el texto a mostrar
			final Rectangle2D rectTexto = tl.getBounds();
			// Transformar las coordenadas relativas al origen a relativas
			// al punto donde debemos de dibujar texto para esta localidad.
			// Aumentar el tamaño del rectángulo en 4 píxeles y desplazarlo 2
			// hacia la izquierda y hacia arriba para añadirle borde, y que
			// quede bonito
			rectTexto.setRect(rectTexto.getX() + x + offX - 2,
				rectTexto.getY() + y + offY - 2,
				rectTexto.getWidth() + 4,
				rectTexto.getHeight() + 4
			);
			g.setColor(fondo);
			g.fill(rectTexto);

			g.setColor(colorTexto);
			tl.draw(g, (float) (x + offX), (float) (y + offY));
		}
	}
}