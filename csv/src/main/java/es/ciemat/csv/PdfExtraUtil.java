package es.ciemat.csv;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import com.aowagie.text.DocumentException;
import com.aowagie.text.pdf.Barcode;
import com.aowagie.text.pdf.Barcode128;
import com.aowagie.text.pdf.PdfReader;
import com.aowagie.text.pdf.PdfStamper;

import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.core.misc.Base64;
import es.gob.afirma.core.signers.AOSimpleSignInfo;
import es.gob.afirma.signers.pades.PdfPreProcessor;

/** Utilidades adicionales de tratamiento de PDF.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class PdfExtraUtil {

	private static final String CSV_TEXT_TEMPLATE_SUBJECTCN = "$$SUBJECTCN$$"; //$NON-NLS-1$
	private static final String CSV_TEXT_TEMPLATE_SIGNDATE = "$$SIGNDATE$$"; //$NON-NLS-1$

	private static final String CSV_LINK_TEMPLATE_CVS = "%csv%"; //$NON-NLS-1$

	private static final String DEFAULT_CSV_TEXT_TEMPLATE = "Firmado por " + CSV_TEXT_TEMPLATE_SUBJECTCN + " en fecha " + CSV_TEXT_TEMPLATE_SIGNDATE; //$NON-NLS-1$ //$NON-NLS-2$

	private static final int CSV_SPACING = 2;

	private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd 'de' MMMM 'de' yyyy 'a las' hh:mm:ss", Locale.forLanguageTag("es-ES")); //$NON-NLS-1$ //$NON-NLS-2$

	private static final JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
	static {
		jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpegParams.setCompressionQuality(1f);
	}

	private PdfExtraUtil() {
		// No instanciable
	}

	/** Obtiene la URL del CSV dado dentro del sistema.
	 * @param id CSV del PDF
	 * @return URL del enlace. */
	public static String getLink(final String id) {
		return ServiceConfig.getCsvRetrieveUrl().replace(CSV_LINK_TEMPLATE_CVS, id);
	}

	static byte[] createCsvAsJpeg(final String id,
			                      final AOSimpleSignInfo[] signatures,
			                      final String textTemplate) throws IOException {

		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("Es necesario proporcionar un ID"); //$NON-NLS-1$
		}

		if (signatures == null || signatures.length < 1) {
			throw new IllegalArgumentException("Es necesrio proporcionar al menos una firma"); //$NON-NLS-1$
		}

		final StringBuilder sb = new StringBuilder(
			getLink(id)
		);
		sb.append('\n');
		for (final AOSimpleSignInfo ssi : signatures) {
			sb.append(
				(textTemplate != null ? textTemplate : DEFAULT_CSV_TEXT_TEMPLATE)
					.replace(CSV_TEXT_TEMPLATE_SUBJECTCN, AOUtil.getCN(ssi.getCerts()[0]))
					.replace(CSV_TEXT_TEMPLATE_SIGNDATE, DATE_FORMATTER.format(ssi.getSigningTime()))
			);
			sb.append('\n');
		}
		final String csvText = sb.toString();

		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g2d = img.createGraphics();
		final Font font = new Font("Arial", Font.PLAIN, 10); //$NON-NLS-1$
		g2d.setFont(font);
		FontMetrics fm = g2d.getFontMetrics();
        final int textWidth = fm.stringWidth(csvText);
        final int textHeight = fm.getHeight();
        g2d.dispose();

		final Image barcode = generateBarcode128AsJpeg(id);
		final int barcodeWidth = barcode.getWidth(null);
		final int barcodeHeight = barcode.getHeight(null);


        img = new BufferedImage(
    		barcodeWidth > textWidth ? barcodeWidth : textWidth,
			barcodeHeight + (textHeight + CSV_SPACING) * (signatures.length + 1),
    		BufferedImage.TYPE_BYTE_GRAY
		);

        g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, img.getWidth(), img.getHeight());

        g2d.setFont(font);
        fm = g2d.getFontMetrics();
        g2d.setColor(Color.BLACK);
        int y = 0;
        for (final String line : csvText.split("\n")) { //$NON-NLS-1$
			g2d.drawString(line, 0, y += fm.getAscent() + CSV_SPACING);
		}
        g2d.drawImage(barcode, 0, y + CSV_SPACING * 3, null);
        g2d.dispose();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next(); //$NON-NLS-1$
        try (
    		final ImageOutputStream ios = new MemoryCacheImageOutputStream(baos)
		) {
        	writer.setOutput(ios);
        	writer.write(null, new IIOImage(img, null, null), jpegParams);
        }

        return baos.toByteArray();

	}

	static Image generateBarcode128AsJpeg(final String code) {
		if (code == null || code.isEmpty()) {
			throw new IllegalArgumentException(
				"El texto del codigo de barras no puede ser nulo ni vacio" //$NON-NLS-1$
			);
		}
		final Barcode barcode = new Barcode128();
		barcode.setCodeType(Barcode.CODE128);
		barcode.setCode(code);
		return barcode.createAwtImage(Color.BLACK, Color.WHITE);
	}

	static byte[] removeSignaturesFromPdf(final byte[] inPdf) throws IOException {
		final PdfReader reader = new PdfReader(inPdf);
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			final PdfStamper stamper = new PdfStamper(reader, baos);
	        stamper.setFormFlattening(true);
	        stamper.close();
		}
		catch (final DocumentException e) {
			throw new IOException(e);
		}
        return baos.toByteArray();
	}

	/** A&ntilde;ade una imagen JPEG a un PDF.
	 * @param inPdf PDF de entrada.
	 * @param inJpg Imagen a a&ntilde;adir.
	 * @param left Distancia de la imagen al borde izquiero de la p&aacute;gina del PDF.
	 * @param bottom Distancia de la imagen al borde inferior de la p&aacute;gina del PDF.
	 * @param pageNum N&uacute;mero de p&aacute;gina del PDF donde insertar la imagen
	 *                (la numeraci&oacute;n comienza en 1).
	 * @param url URL a la que enlazar&aacute; la imagen si queremos que esta sea un hiperv&iacute;nculo
	 *            (puede ser <code>null</code>).
	 * @return PDF con la imagen a&ntilde;adida.
	 * @throws IOException Si hay cualquier problema durante el proceso. */
	static byte[] addImageToPdf(final byte[] inPdf,
								final byte[] inJpg,
			                    final int left,
			                    final int bottom,
			                    final int pageNum,
			                    final String url) throws IOException {
		if (inJpg == null) {
			throw new IllegalArgumentException(
				"La imagen JPEG no puede ser nula" //$NON-NLS-1$
			);
		}

		// Obtenemos las dimensiones de la imagen
		final Image img = ImageIO.read(new ByteArrayInputStream(inJpg));
		final int imgWidth = img.getWidth(null);
		final int imgheight = img.getHeight(null);


		final PdfReader reader = new PdfReader(inPdf);
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			final PdfStamper stamper = new PdfStamper(reader, baos);
			// Si se indica 0 se estampa en todas las paginas
			if (pageNum == 0) {
				for (int i=1;i<=reader.getNumberOfPages();i++) {
					PdfPreProcessor.addImage(
						inJpg,
						imgWidth,
						imgheight,
						left,
						bottom,
						i,
						url,
						stamper
					);
				}
			}
			else {
				PdfPreProcessor.addImage(
					inJpg,
					imgWidth,
					imgheight,
					left,
					bottom,
					pageNum,
					url,
					stamper
				);
			}
			stamper.close();
		}
		catch (final DocumentException e) {
			throw new IOException(e);
		}
		reader.close();
		return baos.toByteArray();
	}

	/** Obtiene el identificador de un PDF.
	 * @param inPdf PDF de entrada.
	 * @return Identificador del PDF de entrada.
	 * @throws IOException Si hay errores en la obtenci&oacute;n. */
	public static String getPdfId(final byte[] inPdf) throws IOException {
		if (inPdf == null) {
			throw new IllegalArgumentException(
				"El PDF no puede ser nulo" //$NON-NLS-1$
			);
		}

		try {
			return Base64.encode(MessageDigest.getInstance("SHA-1").digest(inPdf), true); //$NON-NLS-1$
		}
		catch (final NoSuchAlgorithmException e) {
			throw new IOException(e);
		}

	}

	/** PDF con identificador externo. */
	public static final class PdfId {

		private final byte[] pdf;
		private final String id;

		PdfId(final byte[] pdfBytes, final String strId) {
			this.pdf = pdfBytes != null ? pdfBytes.clone() : null;
			this.id = strId;
		}

		PdfId(final byte[] pdfBytes) {
			this(pdfBytes, null);
		}

		PdfId(final String strId) {
			this(null, strId);
		}

		/** Obtiene el identificador del PDF.
		 * @return Identificador del PDF. */
		public String getId() {
			return this.id;
		}

		/** Obtiene el contenido del PDF.
		 * @return Contenido del PDF. */
		public byte[] getPdf() {
			return this.pdf != null ? this.pdf.clone() : null;
		}

	}

}
