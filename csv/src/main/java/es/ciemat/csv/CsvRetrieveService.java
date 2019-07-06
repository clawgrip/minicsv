package es.ciemat.csv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
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

/** Servicio de recuperaci&oacute;n de documentos con CSV. */
@WebServlet(description = "Servicio de recuperacion de documentos con CSV", urlPatterns = { "/CsvRetrieveService" })
public final class CsvRetrieveService extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final String PARAM_CSV = "csv"; //$NON-NLS-1$

	private static final String PARAM_DATA = "data"; //$NON-NLS-1$

	private static final Logger LOGGER = Logger.getLogger(CsvService.class.getName());

	@Override
	protected void doGet(final HttpServletRequest request,
			               final HttpServletResponse response) throws ServletException,
	                                                                  IOException {
		final String csv = request.getParameter(PARAM_CSV);
		if (csv != null && !csv.isEmpty()) {
			final CsvStorer storer = ServiceConfig.getCsvStorer();

			byte[] pdf = null;
			try {
				pdf = storer.retrievePdfWithCsv(
					new PdfId(
						URLDecoder.decode(csv, "UTF-8") //$NON-NLS-1$
					)
				);
			}
			catch (final CsvStorerException e) {
				response.sendError(
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Error obteniendo el PDF con CSV" //$NON-NLS-1$
				);
			}
			catch (final CsvFileNotFoundException e) {
				response.sendError(
					HttpServletResponse.SC_BAD_REQUEST,
					"No hay un PDF con CSV " + URLDecoder.decode(csv, "UTF-8") //$NON-NLS-1$ //$NON-NLS-2$
				);
			}

			// Ya tenemos el PDF, lo devolvemos en el response con el MIME-Type apropiado
			try (
				final OutputStream os = response.getOutputStream()
			) {
				response.setContentType("application/pdf"); //$NON-NLS-1$
			    os.write(pdf);
			    os.flush();
			    os.close();
			}

			return;
		}

		response.sendError(
			HttpServletResponse.SC_BAD_REQUEST,
			"Es necesario indicar un numero de CSV" //$NON-NLS-1$
		);
	}

	@Override
	protected void doPost(final HttpServletRequest request,
			              final HttpServletResponse response) throws IOException {

		final String base64Data = request.getParameter(PARAM_DATA);
		final byte[] fileData;
		if (base64Data != null && !base64Data.isEmpty()) {
			System.out.println(base64Data);
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
	    	LOGGER.severe("El PDF de entrada no tiene idnetificador: " + e); //$NON-NLS-1$
	    	response.sendError(
    			HttpURLConnection.HTTP_BAD_REQUEST,
    			"El PDF de entrada no tiene firmas indetificador" //$NON-NLS-1$
			);
			return;
		}

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
    			"Guardando el CSV" //$NON-NLS-1$
			);
		}

	    response.sendError(HttpURLConnection.HTTP_OK);

	}

}
