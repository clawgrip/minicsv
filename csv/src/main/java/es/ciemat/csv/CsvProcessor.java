package es.ciemat.csv;

import java.io.IOException;

import es.gob.afirma.core.AOException;
import es.gob.afirma.core.AOFormatFileException;

/** Estampa un CSV y lo almacena adecuadamente.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public interface CsvProcessor {

	/** Estampa un CSV y lo almacena adecuadamente.
	 * @param signedPdf PDF de entrada, debe contener firmas.
	 * @throws PdfLacksSignaturesException Si el PDF de entrada no contiene
	 *                                     ninguna firma.
	 * @throws AOFormatFileException Si la entrada no es un PDF.
	 * @throws IOException Si hay errores tratando los datos.
	 * @throws AOException Si hay errores relacionados con los sellos electr&oacute;nicos.
	 * @throws PdfLacksIdException Si no se le puede asignar un CSV al PDF.
	 * @throws CsvStorerException Si hay problemas almacenando el CSV. */
	void doCsv(final byte[] signedPdf) throws PdfLacksSignaturesException,
                                              AOFormatFileException,
                                              IOException,
                                              AOException,
                                              PdfLacksIdException,
                                              CsvStorerException;

}
