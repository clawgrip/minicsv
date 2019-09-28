package es.ciemat.csv;

import java.io.IOException;

interface CsvSigner {

	byte[] signPdf(final byte[] unsignedPdfWithCsv) throws IOException;

}
