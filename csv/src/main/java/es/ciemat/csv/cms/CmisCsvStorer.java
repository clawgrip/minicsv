package es.ciemat.csv.cms;

import java.io.IOException;
import java.util.logging.Logger;

import es.ciemat.csv.CsvFileNotFoundException;
import es.ciemat.csv.CsvStorer;
import es.ciemat.csv.CsvStorerException;
import es.ciemat.csv.PdfExtraUtil;
import es.ciemat.csv.PdfExtraUtil.PdfId;

/** Almacenador de CSV en gestor documental.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class CmisCsvStorer implements CsvStorer {

	private static final String SUFFIX_ORI = "_ORI"; //$NON-NLS-1$
	private static final String SUFFIX_CSV = "_CSV"; //$NON-NLS-1$
	private static final String SUFFIX_PDF = ".pdf"; //$NON-NLS-1$

	private static final Logger LOGGER = Logger.getLogger(CmisCsvStorer.class.getName());

	/** Construye el almacenador. */
	public CmisCsvStorer() {
		LOGGER.info(
			"Se esta usando el almacenador en CMIS" //$NON-NLS-1$
		);
	}

	@Override
	public String toString() {
		return "Almmacenador de documentos CMIS"; //$NON-NLS-1$
	}

	@Override
	public void storePdfWithCsv(final PdfId pdfWithCsv,
			                    final byte[] pdfWithSignatures) throws CsvStorerException {

		LOGGER.info("Solicitado almacen de un documento en CMIS"); //$NON-NLS-1$

		try {
			CmsDocumentManager.sendDocument(
				pdfWithSignatures,
				pdfWithCsv.getId() + SUFFIX_ORI + SUFFIX_PDF
			);
		}
		catch (CmsFolderNotFoundException | DocumentAlreadyExistsOnCmsException e) {
			LOGGER.info("Error almacenando el documento original: " + e); //$NON-NLS-1$
			throw new CsvStorerException(
				"Error almacenando el documento original: " + e, //$NON-NLS-1$
				e
			);
		}
		try {
			CmsDocumentManager.sendDocument(
				pdfWithCsv.getPdf(),
				pdfWithCsv.getId() + SUFFIX_CSV + SUFFIX_PDF
			);
		}
		catch (CmsFolderNotFoundException | DocumentAlreadyExistsOnCmsException e) {
			LOGGER.info("Error almacenando el documento con CSV: " + e); //$NON-NLS-1$
			throw new CsvStorerException(
				"Error almacenando el documento con CSV: " + e, //$NON-NLS-1$
				e
			);
		}
	}

	@Override
	public byte[] retrievePdfWithCsv(final PdfId pdfId) throws CsvStorerException, CsvFileNotFoundException {
		final String id;
		try {
			id = pdfId.getId() != null ? pdfId.getId() : PdfExtraUtil.getPdfId(pdfId.getPdf());
		}
		catch (final IOException e) {
			LOGGER.info("No se ha indicado el CSV del documento: " + e); //$NON-NLS-1$
			throw new CsvStorerException(
				"No se ha indicado el CSV del documento: " + e, e //$NON-NLS-1$
			);
		}

		try {
			return CmsDocumentManager.loadDocument(id + SUFFIX_CSV + SUFFIX_PDF);
		}
		catch (CmsFolderNotFoundException | IOException e) {
			LOGGER.info("No se ha podido leer el documento con identificador '" + id + "' del repositorio: " + e); //$NON-NLS-1$ //$NON-NLS-2$
			throw new CsvStorerException(
				"No se ha podido leer el documento con identificador '" + id + "' del repositorio: " + e, e //$NON-NLS-1$ //$NON-NLS-2$
			);
		}
		catch (final FileNoExistsOnCmsException e) {
			LOGGER.info("No hay ningun documento para el identificador: '" + id + "' : " + e); //$NON-NLS-1$ //$NON-NLS-2$
			throw new CsvFileNotFoundException(id);
		}
	}

}
