package es.ciemat.csv;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.logging.Logger;

import es.ciemat.csv.PdfExtraUtil.PdfId;
import es.gob.afirma.core.AOException;
import es.gob.afirma.core.AOFormatFileException;
import es.gob.afirma.core.signers.AOSimpleSignInfo;
import es.gob.afirma.core.util.tree.AOTreeModel;
import es.gob.afirma.core.util.tree.AOTreeNode;
import es.gob.afirma.signers.pades.AOPDFSigner;

/** Estampador de CSV en PDF.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class SimplePdfCsvStamer {

	private static final Logger LOGGER = Logger.getLogger(SimplePdfCsvStamer.class.getName());

	private static final String CFG_KEY_KEYSTORE_TYPE = "keystore.type"; //$NON-NLS-1$
	private static final String CFG_KEY_KEYSTORE_FILE = "keystore.file"; //$NON-NLS-1$
	private static final String CFG_KEY_KEYSTORE_PASSWORD = "keystore.password"; //$NON-NLS-1$
	private static final String CFG_KEY_KEYSTORE_ENTRYPASSWORD = "keystore.entrypassword"; //$NON-NLS-1$
	private static final String CFG_KEY_KEYSTORE_ENTRYALIAS = "keystore.entryalias"; //$NON-NLS-1$

	private static final String DEFAULT_SIGN_ALGO = "SHA512withRSA"; //$NON-NLS-1$
	private static final PrivateKey DEFAULT_SIGN_KEY;
	private static final Certificate[] DEAULT_SIGN_CHAIN;

	private static final Properties CFG = new Properties();
	static {
		try {
			CFG.load(SimplePdfCsvStamer.class.getResourceAsStream("/csvconfig.properties")); //$NON-NLS-1$
		}
		catch (final IOException | NullPointerException e) {
			throw new IllegalStateException(
				"Error cargando la configuracion del estampador CSV: " + e, e //$NON-NLS-1$
			);
		}
		final KeyStore ks;
		try {
			ks = KeyStore.getInstance(CFG.getProperty(CFG_KEY_KEYSTORE_TYPE, KeyStore.getDefaultType()));
		}
		catch (final KeyStoreException e) {
			throw new IllegalStateException(
				"Error instanciando un KeyStore del tipo '" + CFG.getProperty(CFG_KEY_KEYSTORE_TYPE, KeyStore.getDefaultType()) + "': " + e, e //$NON-NLS-1$ //$NON-NLS-2$
			);
		}
		try (
			final InputStream fis = new FileInputStream(CFG.getProperty(CFG_KEY_KEYSTORE_FILE))
		) {
			ks.load(
				fis,
				CFG.getProperty(CFG_KEY_KEYSTORE_PASSWORD, "").toCharArray() //$NON-NLS-1$
			);
		}
		catch (NoSuchAlgorithmException | CertificateException | IOException | NullPointerException e) {
			throw new IllegalStateException(
				"Error cargando el KeyStore desde '" + CFG.getProperty(CFG_KEY_KEYSTORE_FILE) + "': " + e, e //$NON-NLS-1$ //$NON-NLS-2$
			);
		}
		try {
			DEFAULT_SIGN_KEY = (PrivateKey) ks.getKey(
				CFG.getProperty(CFG_KEY_KEYSTORE_ENTRYALIAS),
				CFG.getProperty(
					CFG_KEY_KEYSTORE_ENTRYPASSWORD,
					CFG.getProperty(CFG_KEY_KEYSTORE_PASSWORD, "") //$NON-NLS-1$
				).toCharArray()
			);
		}
		catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
			throw new IllegalStateException(
				"Error cargando la clave privada con el alias '" + CFG.getProperty(CFG_KEY_KEYSTORE_ENTRYALIAS) + "': " + e, e //$NON-NLS-1$ //$NON-NLS-2$
			);
		}
		try {
			DEAULT_SIGN_CHAIN = ks.getCertificateChain(
				CFG.getProperty(CFG_KEY_KEYSTORE_ENTRYALIAS)
			);
		}
		catch (final KeyStoreException e) {
			throw new IllegalStateException(
				"Error cargando la cadena de certificados para el alias '" + CFG.getProperty(CFG_KEY_KEYSTORE_ENTRYALIAS) + "': " + e, e //$NON-NLS-1$ //$NON-NLS-2$
			);
		}
	}

	private SimplePdfCsvStamer() {
		// No instanciable
	}

	/** Estampa un CSV en un PDF.
	 * @param inPdf PDF de entrada.
	 * @return PDF con el CSV estampado junto a su identificador.
	 * @throws IOException Si hay problemas durante el proceso.
	 * @throws PdfLacksSignaturesException Si el PDF de entrada no tiene firmas electr&oacute;nicas.
	 * @throws AOException Si hay problemas aplicando el sello electr&oacute;nico al PDF.
	 * @throws PdfLacksIdException Si el PDF no tiene identificador. */
	public static PdfId stampCsv(final byte[] inPdf) throws IOException,
	                                                        PdfLacksSignaturesException,
	                                                        AOException,
	                                                        PdfLacksIdException {
		final AOPDFSigner pdfSigner = new AOPDFSigner();

		if (!pdfSigner.isValidDataFile(inPdf)) {
			if (ServiceConfig.DEBUG) {
				LOGGER.warning("Datos recibidos:\n" + new String(inPdf)); //$NON-NLS-1$
			}
			throw new AOFormatFileException(
				"La entrada no es un PDF" //$NON-NLS-1$
			);
		}

		if (!pdfSigner.isSign(inPdf)) {
			throw new PdfLacksSignaturesException();
		}

		// Obtenemos el ID del documento
		final String pdfId = PdfExtraUtil.getPdfId(inPdf);

		// Obtenemos las firmas del documento
		final AOTreeModel tree = pdfSigner.getSignersStructure(inPdf, true);
		final AOTreeNode root = (AOTreeNode) tree.getRoot();
		final AOSimpleSignInfo[] infos = new AOSimpleSignInfo[root.getChildCount()];
		for (int i=0; i<root.getChildCount(); i++) {
			final AOSimpleSignInfo ssi = (AOSimpleSignInfo) root.getChildAt(0).getUserObject();
			infos[i] = ssi;
		}

		// Eliminamos las firmas del PDF de entrada
		final byte[] flatPdf = PdfExtraUtil.removeSignaturesFromPdf(inPdf);

		// Creamos el CSV
		final byte[] csv = PdfExtraUtil.createCsvAsJpeg(
			pdfId,
			infos,
			null, // textTemplate
			null //linkTemplate
		);

		// Estampamos el CSV
		final byte[] pdfOut = PdfExtraUtil.addImageToPdf(
			flatPdf,
			csv,
			5,
			5,
			1,
			PdfExtraUtil.getLink(pdfId, null)
		);

		// Sellamos el PDF y lo devolvemos todo
		return new PdfId(
			pdfSigner.sign(
				pdfOut,
				DEFAULT_SIGN_ALGO,
				DEFAULT_SIGN_KEY,
				DEAULT_SIGN_CHAIN,
				CFG
			),
			pdfId
		);
	}

}
