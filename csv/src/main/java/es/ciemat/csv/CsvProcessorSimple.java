package es.ciemat.csv;

import java.io.IOException;

import es.ciemat.csv.PdfExtraUtil.PdfId;
import es.gob.afirma.core.AOException;

/** Procesador simple de CSV.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class CsvProcessorSimple implements CsvProcessor {

	@Override
	public void doCsv(final byte[] signedPdf) throws PdfLacksSignaturesException,
	                                                 IOException,
	                                                 AOException,
	                                                 PdfLacksIdException,
	                                                 CsvStorerException {
		// Estampamos el CSV
	    final PdfId pdfId = SimplePdfCsvStamper.stampCsv(signedPdf);

	    // Enviamos el PDF
	    final CsvStorer storer = ServiceConfig.getCsvStorer();
	    storer.storePdfWithCsv(pdfId, signedPdf);
	}

}
