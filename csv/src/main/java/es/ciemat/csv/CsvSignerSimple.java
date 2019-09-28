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

import es.gob.afirma.core.AOException;
import es.gob.afirma.signers.pades.AOPDFSigner;

final class CsvSignerSimple implements CsvSigner {

	private static final String CFG_KEY_KEYSTORE_TYPE = "keystore.type"; //$NON-NLS-1$
	private static final String CFG_KEY_KEYSTORE_FILE = "keystore.file"; //$NON-NLS-1$
	private static final String CFG_KEY_KEYSTORE_PASSWORD = "keystore.password"; //$NON-NLS-1$
	private static final String CFG_KEY_KEYSTORE_ENTRYPASSWORD = "keystore.entrypassword"; //$NON-NLS-1$
	private static final String CFG_KEY_KEYSTORE_ENTRYALIAS = "keystore.entryalias"; //$NON-NLS-1$

	private static final String DEFAULT_SIGN_ALGO = "SHA512withRSA"; //$NON-NLS-1$
	private static final PrivateKey DEFAULT_SIGN_KEY;
	private static final Certificate[] DEAULT_SIGN_CHAIN;

	private static final Logger LOGGER = Logger.getLogger(CsvSignerSimple.class.getName());

	private static final AOPDFSigner PDFSIGNER = new AOPDFSigner();

	private static final Properties CFG = new Properties();
	static {
		try {
			CFG.load(SimplePdfCsvStamper.class.getResourceAsStream("/csvsignerconfig.properties")); //$NON-NLS-1$
		}
		catch (final IOException | NullPointerException e) {
			LOGGER.severe("Error cargando la configuracion del firmador de CSV: " + e); //$NON-NLS-1$
			throw new IllegalStateException(
				"Error cargando la configuracion del firmador de CSV: " + e, e //$NON-NLS-1$
			);
		}
		final KeyStore ks;
		try {
			ks = KeyStore.getInstance(CFG.getProperty(CFG_KEY_KEYSTORE_TYPE, KeyStore.getDefaultType()));
		}
		catch (final KeyStoreException e) {
			LOGGER.severe("Error instanciando un KeyStore del tipo '" + CFG.getProperty(CFG_KEY_KEYSTORE_TYPE, KeyStore.getDefaultType()) + "': " + e); //$NON-NLS-1$ //$NON-NLS-2$
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
			LOGGER.severe("Error cargando el KeyStore desde '" + CFG.getProperty(CFG_KEY_KEYSTORE_FILE) + "': " + e); //$NON-NLS-1$ //$NON-NLS-2$
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
			LOGGER.severe("Error cargando la clave privada con el alias '" + CFG.getProperty(CFG_KEY_KEYSTORE_ENTRYALIAS) + "': " + e); //$NON-NLS-1$ //$NON-NLS-2$
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
			LOGGER.severe("Error cargando la cadena de certificados para el alias '" + CFG.getProperty(CFG_KEY_KEYSTORE_ENTRYALIAS) + "': " + e); //$NON-NLS-1$ //$NON-NLS-2$
			throw new IllegalStateException(
				"Error cargando la cadena de certificados para el alias '" + CFG.getProperty(CFG_KEY_KEYSTORE_ENTRYALIAS) + "': " + e, e //$NON-NLS-1$ //$NON-NLS-2$
			);
		}
	}

	@Override
	public byte[] signPdf(final byte[] unsignedPdfWithCsv) throws IOException {
		try {
			return CsvSignerSimple.PDFSIGNER.sign(
				unsignedPdfWithCsv,
				DEFAULT_SIGN_ALGO,
				DEFAULT_SIGN_KEY,
				DEAULT_SIGN_CHAIN,
				CFG
			);
		}
		catch (final AOException e) {
			throw new IOException(e);
		}
	}

}
