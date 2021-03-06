package es.ciemat.csv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;

import es.ciemat.csv.PdfExtraUtil.PdfId;
import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.core.misc.Base64;

/** Servicio de recuperaci&oacute;n de documentos con CSV. */
@WebServlet(name = "Servicio de recuperacion de documentos con CSV", urlPatterns = { "/CsvRetrieveService" })
@MultipartConfig
public final class CsvRetrieveService extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final String PARAM_CSV = "csv"; //$NON-NLS-1$

	private static final String PARAM_DATA = "data"; //$NON-NLS-1$

	private static final Logger LOGGER = Logger.getLogger(CsvService.class.getName());

	private static final String REDIR_MSG_TAG = "%msg%"; //$NON-NLS-1$

	@Override
	protected void doGet(final HttpServletRequest request,
			               final HttpServletResponse response) throws ServletException,
	                                                                  IOException {
		final String csv = request.getParameter(PARAM_CSV);
		final String redirUrl = ServiceConfig.getWebErrorRedirectUrl();
		if (csv != null && !csv.isEmpty()) {
			final CsvStorer storer = ServiceConfig.getCsvStorer();

			byte[] pdf = null;
			try {
				pdf = storer.retrievePdfWithCsv(
					new PdfId(
						URLDecoder.decode(csv.trim(), "UTF-8") //$NON-NLS-1$
					)
				);
			}
			catch (final CsvStorerException e) {
				final String msg = "Error obteniendo el PDF con CSV '" + URLDecoder.decode(csv, "UTF-8") + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				LOGGER.severe(msg + ": " + e); //$NON-NLS-1$
				response.sendRedirect(redirUrl.replace(REDIR_MSG_TAG, msg));
			}
			catch (final CsvFileNotFoundException e) {
				final String msg = "No hay un PDF con CSV '" + URLDecoder.decode(csv, "UTF-8") + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				LOGGER.severe(msg + ": " + e); //$NON-NLS-1$
				response.sendRedirect(redirUrl.replace(REDIR_MSG_TAG, msg));
			}
			catch (final CmisObjectNotFoundException e) {
				final String msg = "No se ha encontrado el objeto CMIS"; //$NON-NLS-1$
				LOGGER.severe(msg + ": " + e); //$NON-NLS-1$
				response.sendRedirect(redirUrl.replace(REDIR_MSG_TAG, msg));
			}

			// Ya tenemos el PDF, lo devolvemos en el response con el MIME-Type apropiado
			if (pdf != null) {
					try (
					final OutputStream os = response.getOutputStream()
				) {
					response.setContentType("application/pdf"); //$NON-NLS-1$
					os.write(pdf);
					os.flush();
					os.close();
				}
			}
			else {
				final String msg = "Se ha obtenido un PDF nulo"; //$NON-NLS-1$
				LOGGER.severe(msg);
				response.sendRedirect(redirUrl.replace(REDIR_MSG_TAG, msg));
			}

			return;
		}

		final String msg = "No se ha indicado un CSV"; //$NON-NLS-1$
		LOGGER.severe(msg);
		response.sendRedirect(redirUrl.replace(REDIR_MSG_TAG, msg));
	}

	@Override
	protected void doPost(final HttpServletRequest request,
			              final HttpServletResponse response) throws IOException {

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
			catch(final ServletException | IllegalStateException e) {
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

		// En este punto, fileData contiene el PDF de entrada, calculamos
		// su ID
		final String id = PdfExtraUtil.getPdfId(fileData);

		// Con el ID, obtenemos el PDF
		final CsvStorer storer = ServiceConfig.getCsvStorer();

		byte[] pdf = null;
		try {
			pdf = storer.retrievePdfWithCsv(
				new PdfId(
					URLDecoder.decode(id, "UTF-8") //$NON-NLS-1$
				)
			);
		}
		catch (final CsvStorerException e) {
			LOGGER.severe(
				"Error obteniendo el PDF con CSV '" + URLDecoder.decode(id, "UTF-8") + "': " + e //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			);
			response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Error obteniendo el PDF con CSV '" + URLDecoder.decode(id, "UTF-8") + "': " + e //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			);
			return;
		}
		catch (final CsvFileNotFoundException e) {
			LOGGER.severe(
				"No hay un PDF con CSV '" + URLDecoder.decode(id, "UTF-8") + "': " + e //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			);
			response.sendError(
				HttpServletResponse.SC_BAD_REQUEST,
				"No hay un PDF con CSV '" + URLDecoder.decode(id, "UTF-8") + "': " + e //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			);
			return;
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

	}

}
