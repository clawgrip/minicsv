package es.ciemat.csv;

/** El PDF no tiene un identificador en el XMP.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class PdfLacksIdException extends Exception {

	private static final long serialVersionUID = -7549114113422032049L;

	PdfLacksIdException() {
		super();
	}

}
