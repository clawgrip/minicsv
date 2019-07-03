package es.ciemat.csv;

/** Error relativo al almacenamiento de PDF con CSV.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class CsvStorerException extends Exception {

	private static final long serialVersionUID = 1063361141146801595L;

	/** Crea una excepci&oacute;n relativa al almacenamiento de PDF con CSV.
	 * @param msg Mensaje.
	 * @param cause Causa inicial. */
	public CsvStorerException(final String msg, final Throwable cause) {
		super(msg, cause);
	}

	/** Crea una excepci&oacute;n relativa al almacenamiento de PDF con CSV.
	 * @param cause Causa inicial. */
	public CsvStorerException(final Throwable cause) {
		super(cause);
	}

}
