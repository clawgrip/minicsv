package es.ciemat.csv;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.VolatileImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.Test;

import com.aowagie.text.DocumentException;
import com.aowagie.text.pdf.Barcode;
import com.aowagie.text.pdf.Barcode128;
import com.aowagie.text.pdf.PdfReader;
import com.aowagie.text.pdf.PdfStamper;

import es.gob.afirma.core.signers.AOSimpleSignInfo;
import es.gob.afirma.core.util.tree.AOTreeModel;
import es.gob.afirma.core.util.tree.AOTreeNode;
import es.gob.afirma.signers.pades.AOPDFSigner;
import es.gob.afirma.signers.pades.PdfPreProcessor;

/** Pruebas de tratamientos de PDF.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class TestPdfUtils {

	private static final byte[] DEFAULT_XMP = (
		"<?xpacket begin=\"﻿\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\r\n" + //$NON-NLS-1$
		"<x:xmpmeta xmlns:x=\"adobe:ns:meta/\">\r\n" + //$NON-NLS-1$
		"  <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\r\n" + //$NON-NLS-1$
		"  </rdf:RDF>\r\n" + //$NON-NLS-1$
		"</x:xmpmeta>" //$NON-NLS-1$
	).getBytes();

	private static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

	private static final String RDF_CLOSE_TAG = "</rdf:RDF>"; //$NON-NLS-1$
	private static final String XMP_CLOSE_TAG = "</x:xmpmeta>"; //$NON-NLS-1$
	private static final String DC_IDENTIFIER_OPEN_TAG = "<dc:identifier>"; //$NON-NLS-1$
	private static final String DC_IDENTIFIER_CLOSE_TAG = "</dc:identifier>"; //$NON-NLS-1$
	private static final String ID_OPEN_TAG = "<rdf:Description rdf:about=\"\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">" + DC_IDENTIFIER_OPEN_TAG; //$NON-NLS-1$
	private static final String ID_CLOSE_TAG = DC_IDENTIFIER_CLOSE_TAG + "</rdf:Description>"; //$NON-NLS-1$

	/** Prueba de generaci&oacute;n de im&aacute;genes de c&oacute;digos de barras.
	 * @throws IOException En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
	public void testBarcodeGeneration() throws IOException {
		final Barcode barcode = new Barcode128();
		barcode.setCodeType(Barcode.CODE128);
		barcode.setCode("ejemplodecodigo"); //$NON-NLS-1$
		final Image barcodeImage = barcode.createAwtImage(Color.BLACK, Color.WHITE);
		final RenderedImage ri = toBufferedImage(barcodeImage, BufferedImage.TYPE_BYTE_BINARY);
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(ri, "jpg", baos); //$NON-NLS-1$

		try (
			final OutputStream fos = new FileOutputStream(
				File.createTempFile("barcode_", ".jpeg") //$NON-NLS-1$ //$NON-NLS-2$
			)
		) {
			fos.write(baos.toByteArray());
			fos.flush();
		}
	}

	private static BufferedImage toBufferedImage(final Image image, final int type) {
		if (image instanceof BufferedImage) {
			return (BufferedImage) image;
		}
		if (image instanceof VolatileImage) {
			return ((VolatileImage) image).getSnapshot();
		}
		loadImage(image);
		final BufferedImage buffImg = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
		final Graphics2D g2 = buffImg.createGraphics();
		g2.drawImage(image, null, null);
		g2.dispose();
		return buffImg;
	}

	private static void loadImage(final Image image) {
		final class StatusObserver implements ImageObserver {
			boolean imageLoaded = false;
			@Override
			public boolean imageUpdate(final Image img, final int infoflags, final int x, final int y, final int width, final int height) {
				if (infoflags == ALLBITS) {
					synchronized (this) {
						this.imageLoaded = true;
						notify();
					}
					return true;
				}
				return false;
			}
		}
		final StatusObserver imageStatus = new StatusObserver();
		synchronized (imageStatus) {
			if (image.getWidth(imageStatus) == -1 || image.getHeight(imageStatus) == -1) {
				while (!imageStatus.imageLoaded) {
					try {
						imageStatus.wait();
					}
					catch (final InterruptedException ex) {
						ex.printStackTrace();
					}
				}
			}
		}
	}

	/** Prueba de inserci&oacute;n de ID en el XMP.
	 * @throws IOException En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
	public void testXmp() throws IOException {

		final byte[] pdfIn;
		try {
			pdfIn = Files.readAllBytes(
				Paths.get(
					TestPdfUtils.class.getResource("/hola.pdf").toURI() //$NON-NLS-1$
				)
			);
		}
		catch (final URISyntaxException e) {
			throw new IOException(e);
		}

		final PdfReader reader = new PdfReader(pdfIn);
		byte[] meta = reader.getMetadata();

		System.out.println(new String(meta));

		if (meta == null || new String(meta).trim().isEmpty()) {
			meta = DEFAULT_XMP;
		}

		final String metaStr = new String(meta);

		System.out.println(metaStr);

		// Si ya tiene un ID, lo devolvemos y salimos
		if (metaStr.contains(DC_IDENTIFIER_OPEN_TAG)) {
			if (!metaStr.contains(DC_IDENTIFIER_CLOSE_TAG)) {
				throw new IOException("El PDF tiene los metadatos mal formados:\n" + metaStr); //$NON-NLS-1$
			}
			System.out.println(
				metaStr.substring(
					metaStr.indexOf(DC_IDENTIFIER_OPEN_TAG) + DC_IDENTIFIER_OPEN_TAG.length(),
					metaStr.indexOf(DC_IDENTIFIER_CLOSE_TAG)
				)
			);
			return;
		}

		// Comprobamos que el XMP tenga la estructura que necesitamos
		if (!metaStr.contains(XMP_CLOSE_TAG)) {
			throw new IOException("El PDF tiene los metadatos mal formados:\n" + metaStr); //$NON-NLS-1$
		}
		if (!metaStr.contains(RDF_CLOSE_TAG)) {
			// Anadimos una seccion RDF vacia
			metaStr.replace(
				XMP_CLOSE_TAG,
				"<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"></rdf:RDF>" + XMP_CLOSE_TAG); //$NON-NLS-1$
		}

		// Generamos un ID aleatorio
		final String id = UUID.randomUUID().toString();

		// Lo insertamos en el XMP
		final String newMetaStr = metaStr.replace(
			RDF_CLOSE_TAG,
			ID_OPEN_TAG + id + ID_CLOSE_TAG + RDF_CLOSE_TAG
		);

		// E insertamos el XMP en el PDF
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final PdfStamper stamper;
		try {
			stamper = new PdfStamper(reader, baos);
		}
		catch(final DocumentException ex) {
			throw new IOException(ex);
		}
		try {
			stamper.setXmpMetadata(newMetaStr.getBytes(DEFAULT_ENCODING));
			stamper.close();
		}
		catch(final DocumentException ex) {
			throw new IOException(ex);
		}

		reader.close();

		final byte[] pdfOut = baos.toByteArray();

		try (
			final OutputStream fos = new FileOutputStream(
				File.createTempFile("withID_", ".pdf") //$NON-NLS-1$ //$NON-NLS-2$
			)
		) {
			fos.write(pdfOut);
			fos.flush();
		}

	}

	/** Prueba de eliminaci&oacute;n de firmas de un PDF.
	 * @throws Exception En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
	public void testRemovingPdfSignatures() throws Exception {
		final byte[] inPdf = Files.readAllBytes(
			Paths.get(
				TestPdfUtils.class.getResource("/cosigned.pdf").toURI() //$NON-NLS-1$
			)
		);
		final byte[] outPdf = PdfExtraUtil.removeSignaturesFromPdf(inPdf);
		try (
			final OutputStream fos = new FileOutputStream(File.createTempFile("removed_", ".pdf")) //$NON-NLS-1$ //$NON-NLS-2$
		) {
			fos.write(outPdf);
			fos.flush();
		}
	}

	/** Prueba de creaci&oacute;n de CSV.
	 * @throws Exception En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
	public void testCreateCSV() throws Exception {

		final byte[] inPdf = Files.readAllBytes(
			Paths.get(
				TestPdfUtils.class.getResource("/cosigned.pdf").toURI() //$NON-NLS-1$
			)
		);

		final AOTreeModel tree = new AOPDFSigner().getSignersStructure(inPdf, true);
		final AOTreeNode root = (AOTreeNode) tree.getRoot();

		final AOSimpleSignInfo[] infos = new AOSimpleSignInfo[root.getChildCount()];
		for (int i=0; i<root.getChildCount(); i++) {
			final AOSimpleSignInfo ssi = (AOSimpleSignInfo) root.getChildAt(0).getUserObject();
			infos[i] = ssi;
		}

		final String id = UUID.randomUUID().toString();

		final byte[] csv = PdfExtraUtil.createCsvAsJpeg(
			id,
			infos,
			null, // textTemplate
			null // linkTemplate
		);

		try (
			final OutputStream fos = new FileOutputStream(
				File.createTempFile("csv_", ".jpeg") //$NON-NLS-1$ //$NON-NLS-2$
			)
		) {
			fos.write(csv);
			fos.flush();
		}

	}

	/** Prueba de la obtenci&oacute;n de informaci&oacute;n de firmas.
	 * @throws Exception En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
	public void testGetSignersStructure() throws Exception {
		final byte[] inPdf = Files.readAllBytes(
			Paths.get(
				TestPdfUtils.class.getResource("/cosigned.pdf").toURI() //$NON-NLS-1$
			)
		);
		final AOTreeModel tree = new AOPDFSigner().getSignersStructure(inPdf, true);
		final AOTreeNode root = (AOTreeNode) tree.getRoot();
		for (int i=0; i<root.getChildCount(); i++) {
			final AOSimpleSignInfo ssi = (AOSimpleSignInfo) root.getChildAt(0).getUserObject();
			System.out.println(ssi);
		}
	}

	/** Pruena de inserci&oacute;n de imagen en PDF.
	 * @throws Exception En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
	public void testImageStamping() throws Exception {

		final byte[] inPdf = Files.readAllBytes(
			Paths.get(
				TestPdfUtils.class.getResource("/cosigned.pdf").toURI() //$NON-NLS-1$
			)
		);

		final byte[] outPdf = PdfExtraUtil.removeSignaturesFromPdf(inPdf);

		final PdfReader reader = new PdfReader(outPdf);
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final byte[] inJpg = Files.readAllBytes(
			Paths.get(
				TestPdfUtils.class.getResource("/workboy.jpg").toURI() //$NON-NLS-1$
			)
		);
		final PdfStamper stamper = new PdfStamper(reader, baos);
		PdfPreProcessor.addImage(
			inJpg,
			264,
			288,
			400,
			200,
			1, // Pagina
			"http://www.google.com/", //$NON-NLS-1$
			stamper
		);
		stamper.close();
		reader.close();
		try (
			final OutputStream fos = new FileOutputStream(
				File.createTempFile("imaged_", ".pdf") //$NON-NLS-1$ //$NON-NLS-2$
			)
		) {
			fos.write(baos.toByteArray());
			fos.flush();
		}
	}

	/** Prueba completa de aplicaci&oacute;n de CSV.
	 * @throws Exception En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
	public void testCompleteCsv() throws Exception {

		final byte[] inPdf = Files.readAllBytes(
			Paths.get(
				TestPdfUtils.class.getResource("/cosigned.pdf").toURI() //$NON-NLS-1$
			)
		);

		final byte[] outPdf = SimplePdfCsvStamer.stampCsv(inPdf).getPdf();

		try (
			final OutputStream fos = new FileOutputStream(
				File.createTempFile("csved_", ".pdf") //$NON-NLS-1$ //$NON-NLS-2$
			)
		) {
			fos.write(outPdf);
			fos.flush();
		}

	}

}
