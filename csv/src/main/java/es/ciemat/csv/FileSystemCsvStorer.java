package es.ciemat.csv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

import es.ciemat.csv.PdfExtraUtil.PdfId;

/** Almacenador de CSV como ficheros temporales.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class FileSystemCsvStorer implements CsvStorer {

	@Override
	public void storePdfWithCsv(final PdfId pdfWithCsv,
			                    final byte[] pdfWithSignatures) throws CsvStorerException {
		if (pdfWithCsv == null) {
			throw new IllegalArgumentException(
				"El PDF con CSV no puede ser nulo" //$NON-NLS-1$
			);
		}
		try (
			final OutputStream fos = new FileOutputStream(
				File.createTempFile(pdfWithCsv.getId() + "_CSV_", ".pdf") //$NON-NLS-1$ //$NON-NLS-2$
			)
		) {
			fos.write(pdfWithCsv.getPdf());
			fos.flush();
		}
		catch (final IOException e) {
			throw new CsvStorerException(
				"Error guardando el PDF con CSV: " + e, e //$NON-NLS-1$
			);
		}
		if (pdfWithSignatures != null && pdfWithSignatures.length > 0) {
			try (
				final OutputStream fos = new FileOutputStream(
					File.createTempFile(pdfWithCsv.getId() + "_ORI_", ".pdf") //$NON-NLS-1$ //$NON-NLS-2$
				)
			) {
				fos.write(pdfWithSignatures);
				fos.flush();
			}
			catch (final IOException e) {
				throw new CsvStorerException(
					"Error guardando el PDF con CSV: " + e, e //$NON-NLS-1$
				);
			}
		}
		else {
			Logger.getLogger(FileSystemCsvStorer.class.getName()).warning(
				"No se ha proporcionado el PDF original con las firmas" //$NON-NLS-1$
			);
		}

	}

}
