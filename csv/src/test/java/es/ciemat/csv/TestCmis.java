package es.ciemat.csv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import es.ciemat.csv.cms.CmsDocumentManager;
import es.ciemat.csv.cms.FileNoExistsOnCmsException;

/** Pruebas de acceso a gestor documental.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class TestCmis {

	/** Pruebas de carga y descarga de documentos en gestor documental.
	 * @throws Exception En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
	//@Ignore
	public void testCmsDoc() throws Exception {

		final String contenido = "jskjdhskjdfhs"; //$NON-NLS-1$
		final String fileName = "pepe.txt"; //$NON-NLS-1$

		CmsDocumentManager.sendDocument(
			contenido.getBytes(StandardCharsets.UTF_8),
			fileName
		);

		if (!CmsDocumentManager.fileExists(
				CmsDocumentManager.CMS_FOLDER + (CmsDocumentManager.CMS_FOLDER.endsWith("/") ? "" : "/") + fileName,  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				null // Sesion
		)) {
			throw new FileNoExistsOnCmsException();
		}

		final byte[] content = CmsDocumentManager.loadDocument(fileName);

		if (!contenido.equals(new String(content, StandardCharsets.UTF_8))) {
			throw new IOException("El contenido enviado no coincide con el recuperado"); //$NON-NLS-1$
		}
	}

}
