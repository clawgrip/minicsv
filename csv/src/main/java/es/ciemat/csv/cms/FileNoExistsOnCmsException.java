package es.ciemat.csv.cms;

import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;

/** El documento no existe en el gestor documental.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class FileNoExistsOnCmsException extends CmsException {

	FileNoExistsOnCmsException(final CmisObjectNotFoundException e) {
		super(e);
	}

	/** Constructor vac&ioacute;o de la excepci&oacute;n.*/
	public FileNoExistsOnCmsException() {
		// VACIO
	}

	private static final long serialVersionUID = 4751852851194814303L;

}
