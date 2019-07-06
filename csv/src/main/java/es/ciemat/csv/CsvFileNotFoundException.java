package es.ciemat.csv;

/** No existe documento para el CSV proporcionado.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class CsvFileNotFoundException extends Exception {

	private static final long serialVersionUID = -335528768357752513L;

	/** Crea una excepci&oacute;n cuando el CSV no se corresponde con ning&uacute;n documento.
	 * @param csv CSV del documento.*/
	public CsvFileNotFoundException(final String csv) {
		super("No se ha encontrado el documento con CSV: " + csv); //$NON-NLS-1$
	}

}
