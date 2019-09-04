package es.ciemat.csv;

import java.io.IOException;

import es.ciemat.csv.PdfExtraUtil.PdfId;
import es.gob.afirma.core.AOException;

/** Procesador de CSV usando informes de firma de EEUTIL.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class CsvProcessorEeutil implements CsvProcessor {

	@Override
	public void doCsv(final byte[] signedPdf) throws PdfLacksSignaturesException,
	                                                 IOException,
	                                                 AOException,
	                                                 PdfLacksIdException,
	                                                 CsvStorerException {
		// Obtenemos el ID del PDF
		final String pdfHash = PdfExtraUtil.getPdfId(signedPdf);

		// Obtenmos la URL del documento en el sistema de CSV
		final String scvUrl = PdfExtraUtil.getLink(pdfHash);

		// Obtenemos el informe de firma de EEUTIL (PDF sellado) como array de octetos
		//TODO: Aqui llamamos al SOAP de EEUTIL para obtener el informe de firma. Le pasamos el
		//      PDF firmado (signedPdf) y la URL que hemos obtenido antes como parametro para
		//      que la ponga en el PDF
		final byte[] eeutilSigReport = null;

	    // Enviamos el PDF
	    final CsvStorer storer = ServiceConfig.getCsvStorer();
	    storer.storePdfWithCsv(
    		new PdfId(
				signedPdf,
				pdfHash
			),
    		signedPdf
		);
	}

}
