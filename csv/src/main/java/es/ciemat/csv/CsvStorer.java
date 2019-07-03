package es.ciemat.csv;

import es.ciemat.csv.PdfExtraUtil.PdfId;

/** Almacenador de PDF con CSV.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public interface CsvStorer {

	/** Almacena un PDF con CSV.
	 * @param pdfWithCsv PDF con el CSV y valor del CSV.
	 * @param pdfWithSignatures PDF original con las firmas electr&oacute;nicas.
	 * @throws CsvStorerException Si hay errores durante el proceso. */
	void storePdfWithCsv(final PdfId pdfWithCsv,
			             final byte[] pdfWithSignatures) throws CsvStorerException;

}
