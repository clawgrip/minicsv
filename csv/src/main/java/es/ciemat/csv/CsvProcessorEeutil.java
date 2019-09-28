package es.ciemat.csv;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import es.ciemat.csv.PdfExtraUtil.PdfId;
import es.gob.afirma.core.AOException;
import es.mpt.dsic.inside.ws.service.ApplicationLogin;
import es.mpt.dsic.inside.ws.service.ContenidoInfo;
import es.mpt.dsic.inside.ws.service.CopiaInfo;
import es.mpt.dsic.inside.ws.service.CopiaInfoExtended;
import es.mpt.dsic.inside.ws.service.EeUtilServiceProxy;

/** Procesador de CSV usando informes de firma de EEUTIL.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class CsvProcessorEeutil implements CsvProcessor {

	private static final Logger LOGGER = Logger.getLogger(CsvProcessorEeutil.class.getName());

	private static final String EEUTIL_USER;
	private static final String EEUTIL_PASSWORD;
	private static final String RECOVER_URL;
	private static final String SITE_URL;

	private static final Properties CFG = new Properties();
	private static final String CFG_KEY_USER = "user"; //$NON-NLS-1$
	private static final String CFG_KEY_PASSWORD = "password"; //$NON-NLS-1$
	private static final String CFG_KEY_RECOVER_URL = "recoverUrl"; //$NON-NLS-1$
	private static final String CFG_KEY_SITE_URL = "sedeUrl"; //$NON-NLS-1$
	static {
		try {
			CFG.load(CsvProcessorEeutil.class.getResourceAsStream("/eeutil.properties")); //$NON-NLS-1$
		}
		catch (final IOException | NullPointerException e) {
			LOGGER.severe("Error cargando la configuracion del estampador CSV con EEUTIL: " + e); //$NON-NLS-1$
			throw new IllegalStateException(
				"Error cargando la configuracion del estampador CSV con EEUTIL: " + e, e //$NON-NLS-1$
			);
		}
		EEUTIL_USER     = CFG.getProperty(CFG_KEY_USER);
		EEUTIL_PASSWORD = CFG.getProperty(CFG_KEY_PASSWORD);
		RECOVER_URL     = CFG.getProperty(CFG_KEY_RECOVER_URL);
		SITE_URL        = CFG.getProperty(CFG_KEY_SITE_URL);
	}

	private static final String CSV_URL_TAG = "%%CSV%%"; //$NON-NLS-1$
	private static final String PDF_MIMETYPE = "application/pdf"; //$NON-NLS-1$

	@Override
	public void doCsv(final byte[] signedPdf) throws PdfLacksSignaturesException,
	                                                 IOException,
	                                                 AOException,
	                                                 PdfLacksIdException,
	                                                 CsvStorerException {
		// Obtenemos el CSV
		final String csv = PdfExtraUtil.getPdfId(signedPdf);

		// URL de recuperacion del CSV
		final String recoverURL = RECOVER_URL.replace(CSV_URL_TAG, csv);

		// Obtenemos el informe de firma de EEUTIL como array de octetos

    	final ApplicationLogin aplicacionInfo = new ApplicationLogin(EEUTIL_USER, EEUTIL_PASSWORD);
    	final CopiaInfoExtended copiaInfoExtended = new CopiaInfoExtended();
    	final EeUtilServiceProxy eeUtilServiceProxy = new EeUtilServiceProxy();
    	copiaInfoExtended.setIdAplicacion("GEN");
    	copiaInfoExtended.setCsv(csv);
    	copiaInfoExtended.setFecha("");
    	copiaInfoExtended.setExpediente("");
    	copiaInfoExtended.setNif("");
    	copiaInfoExtended.setUrlSede(SITE_URL);
    	final ContenidoInfo contenido = new ContenidoInfo();
    	contenido.setContenido(signedPdf);
    	contenido.setTipoMIME(PDF_MIMETYPE);
    	copiaInfoExtended.setContenido(contenido);
    	copiaInfoExtended.setTituloAplicacion("ÁMBITO");
    	copiaInfoExtended.setTituloCSV("CSV");
    	copiaInfoExtended.setTituloURL("DIRECCIÓN DE VALIDACIÓN");
    	copiaInfoExtended.setEstamparLogo(false);
    	copiaInfoExtended.setLateral(
			"Código seguro de Verificación :" csv + ". Puede verificar la integridad de este documento en la siguiente dirección:" + recoverURL
		);
    	copiaInfoExtended.setUrlQR(recoverURL);
    	copiaInfoExtended.setFirma(signedPdf);
    	copiaInfoExtended.setSimple(true);

    	final CopiaInfo informe = eeUtilServiceProxy.generarInforme(aplicacionInfo, copiaInfoExtended);
    	final byte[] informeB = informe.getContenido().getContenido();

    	// Firmamos el informe
    	final byte[] signedInformeB = ServiceConfig.getCsvSigner().signPdf(informeB);

    	final PdfId idInforme = new PdfId(informeB, PdfExtraUtil.getPdfId(signedInformeB));

	    // Enviamos el PDF
	    final CsvStorer storer = ServiceConfig.getCsvStorer();
	    storer.storePdfWithCsv(
    		idInforme,
    		signedPdf
		);
	}

}
