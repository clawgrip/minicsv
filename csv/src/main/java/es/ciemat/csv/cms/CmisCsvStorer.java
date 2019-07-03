package es.ciemat.csv.cms;

import es.ciemat.csv.CsvStorer;
import es.ciemat.csv.CsvStorerException;
import es.ciemat.csv.PdfExtraUtil.PdfId;

/** Almacenador de CSV en gestor documental.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class CmisCsvStorer implements CsvStorer {

	private static final String SUFFIX_ORI = "_ORI"; //$NON-NLS-1$
	private static final String SUFFIX_CSV = "_CSV"; //$NON-NLS-1$
	private static final String SUFFIX_PDF = ".pdf"; //$NON-NLS-1$

	@Override
	public void storePdfWithCsv(final PdfId pdfWithCsv,
			                    final byte[] pdfWithSignatures) throws CsvStorerException {
		try {
			CmsDocumentManager.sendDocument(
				pdfWithSignatures,
				pdfWithCsv.getId() + SUFFIX_ORI + SUFFIX_PDF
			);
		}
		catch (CmsFolderNotFoundException | DocumentAlreadyExistsOnCmsException e) {
			throw new CsvStorerException(
				"Error almacenando el documento original", //$NON-NLS-1$
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
			throw new CsvStorerException(
				"Error almacenando el documento con CSV", //$NON-NLS-1$
				e
			);
		}
	}

}
