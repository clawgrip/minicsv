package es.ciemat.csv;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.core.misc.Base64;

/** Servicio para obtenci&oacute;n del identificador &uacute;nico de un PDF.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
@WebServlet("/IdService")
public final class IdService extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = Logger.getLogger(IdService.class.getName());

	private static final String PARAM_DATA = "data"; //$NON-NLS-1$

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		final String base64Data = request.getParameter(PARAM_DATA);
		final byte[] fileData;
		if (base64Data != null && !base64Data.isEmpty()) {
			fileData = Base64.decode(base64Data.replace("-", "+").replace("_", "/"));   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
		}
		else {
		    Part filePart;
			try {
				filePart = request.getPart("file"); // Recupera <input type="file" name="file"> //$NON-NLS-1$
			}
			catch(final ServletException e) {
				// No es multiparte
				LOGGER.info(
					"La entrada no es multiparte, se intentara recuperar el PDF del cuerpo del POST: " + e //$NON-NLS-1$
				);
				filePart = null;
			}

		    try (
	    		final InputStream fileContent = filePart != null ?
					filePart.getInputStream():
						request.getInputStream()
			) {
		    	fileData = AOUtil.getDataFromInputStream(fileContent);
		    }
		}

		final String id;
		try {
			id = PdfExtraUtil.getPdfId(fileData);
		}
		catch (final IOException e) {
			LOGGER.log(
    			Level.SEVERE,
    			"No se ha podido obtener el identifiador del PDF: " + e, //$NON-NLS-1$
    			e
			);
	    	response.sendError(
    			HttpURLConnection.HTTP_INTERNAL_ERROR,
    			"No se ha podido obtener el identifiador del PDF" //$NON-NLS-1$
			);
			return;
		}

		try (
			final PrintWriter pw = response.getWriter()
		) {
			pw.write(id);
			pw.flush();
		}

	}

}
