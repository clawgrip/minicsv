package es.ciemat.csv;

/** El PDF ya contiene al menos una firma electr&oacute;nica.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class PdfIsAlreadySignedException extends Exception {

	private static final long serialVersionUID = 7400171585338366701L;

	PdfIsAlreadySignedException(final String msg) {
		super(msg);
	}

}
