package es.ciemat.csv.cms;

import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;

/** Excepci&oacute;n relacionada con la carga o recuperaci&oacute;n de documentos
 * en el gestor documental.
 * @author Tom@aacute;s Garc&iacute;a-Mer&aacute;s. */
public abstract class CmsException extends Exception {

	CmsException(final CmisObjectNotFoundException e) {
		super(e);
	}

	CmsException() {
		// VACIO
	}

	private static final long serialVersionUID = 8574802166932306521L;

}
