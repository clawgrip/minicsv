package es.ciemat.csv;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import es.ciemat.csv.PdfExtraUtil.PdfId;
import es.gob.afirma.core.AOException;
import es.gob.afirma.core.AOFormatFileException;
import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.core.misc.Base64;

/** Servicio de estampaci&oacute;n de CSV.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
@WebServlet("/CsvService")
@MultipartConfig
public final class CsvService extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final String PARAM_DATA = "data"; //$NON-NLS-1$

	private static final Logger LOGGER = Logger.getLogger(CsvService.class.getName());

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
	                                                                                          IOException {
		try (
			final PrintWriter pw = resp.getWriter();
		) {
			pw.write("Servicio de generacion de CSV, admite solo llamadas POST de tipo REST"); //$NON-NLS-1$
		}
	}

	@Override
	protected void doPost(final HttpServletRequest request,
			              final HttpServletResponse response) throws IOException {

		LOGGER.info("Solicitada estampacion de CSV en documento"); //$NON-NLS-1$

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

		if (fileData == null) {
			LOGGER.severe(
				"No se ha recibido un documento valido" //$NON-NLS-1$
			);
			response.sendError(
				HttpServletResponse.SC_BAD_REQUEST,
				"No se ha recibido un documento valido" //$NON-NLS-1$
			);
			return;
		}

		LOGGER.info("Obtenido un documento de " + fileData.length +  " octetos"); //$NON-NLS-1$ //$NON-NLS-2$

	    final PdfId pdfId;
	    try {
	    	pdfId = SimplePdfCsvStamper.stampCsv(fileData);
		}
	    catch (final AOFormatFileException e) {
	    	LOGGER.severe("La entrada no es un documento PDF: " + e); //$NON-NLS-1$
	    	response.sendError(
    			HttpURLConnection.HTTP_BAD_REQUEST,
    			"La entrada no es un documento PDF" //$NON-NLS-1$
			);
			return;
		}
	    catch (final PdfLacksSignaturesException e) {
	    	LOGGER.severe("El PDF de entrada no tiene firmas electronicas: " + e); //$NON-NLS-1$
	    	response.sendError(
    			HttpURLConnection.HTTP_BAD_REQUEST,
    			"El PDF de entrada no tiene firmas electronicas" //$NON-NLS-1$
			);
			return;
		}
	    catch (final IOException e) {
	    	LOGGER.log(
    			Level.SEVERE,
    			"Error estampando el CSV en el PDF: " + e, //$NON-NLS-1$
    			e
			);
	    	response.sendError(
    			HttpURLConnection.HTTP_INTERNAL_ERROR,
    			"Error estampando el CSV en el PDF" //$NON-NLS-1$
			);
			return;
	    }
	    catch (final AOException e) {
	    	LOGGER.log(
    			Level.SEVERE,
    			"Error sellando electronicamente el PDF: " + e, //$NON-NLS-1$
    			e
			);
	    	response.sendError(
    			HttpURLConnection.HTTP_INTERNAL_ERROR,
    			"Error sellando electronicamente el PDF" //$NON-NLS-1$
			);
			return;
		}
	    catch (final PdfLacksIdException e) {
	    	LOGGER.severe("El PDF de entrada no tiene identificador: " + e); //$NON-NLS-1$
	    	response.sendError(
    			HttpURLConnection.HTTP_BAD_REQUEST,
    			"El PDF de entrada no tiene identificador" //$NON-NLS-1$
			);
			return;
		}
	    catch(final Exception | Error e) {
	    	LOGGER.log(Level.SEVERE, "Error indefinido durante la estampacion: " + e, e); //$NON-NLS-1$
	    	response.sendError(
    			HttpURLConnection.HTTP_BAD_REQUEST,
    			"Error indefinido durante la estampacion" //$NON-NLS-1$
			);
			return;
	    }

	    LOGGER.info("El ID del documento recibido es: " + pdfId.getId()); //$NON-NLS-1$

	    // Enviamos el PDF
	    final CsvStorer storer = ServiceConfig.getCsvStorer();
	    try {
			storer.storePdfWithCsv(pdfId, fileData);
		}
	    catch (final CsvStorerException e) {
	    	LOGGER.log(
    			Level.SEVERE,
    			"Error guardando el CSV: " + e, //$NON-NLS-1$
    			e
			);
	    	response.sendError(
    			HttpURLConnection.HTTP_INTERNAL_ERROR,
    			"Error guardando el CSV" //$NON-NLS-1$
			);
	    	return;
		}

	    LOGGER.info("Proceso terminado con exito"); //$NON-NLS-1$

	    response.sendError(HttpURLConnection.HTTP_OK);

	}

}
