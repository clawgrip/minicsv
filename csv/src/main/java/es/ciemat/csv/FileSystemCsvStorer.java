package es.ciemat.csv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import es.ciemat.csv.PdfExtraUtil.PdfId;
import es.gob.afirma.core.misc.AOUtil;

/** Almacenador de CSV como ficheros temporales.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class FileSystemCsvStorer implements CsvStorer {

	private static final String SUFFIX_ORI = "_ORI"; //$NON-NLS-1$
	private static final String SUFFIX_CSV = "_CSV"; //$NON-NLS-1$
	private static final String SUFFIX_PDF = ".pdf"; //$NON-NLS-1$

	private static final Logger LOGGER = Logger.getLogger(FileSystemCsvStorer.class.getName());

	private static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
	static {
		if (!TMP_DIR.isDirectory() || !TMP_DIR.canWrite() || !TMP_DIR.canRead()) {
			throw new IllegalStateException("Directorio temporal invalido: " + System.getProperty("java.io.tmpdir")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/** Construye el almacenador. */
	public FileSystemCsvStorer() {
		LOGGER.warning(
			"Se esta usando el almacenador en temporal para pruebas, NO USAR EN PRODUCCION" //$NON-NLS-1$
		);
	}

	@Override
	public String toString() {
		return "Almacenador de documentos (para pruebas) en sistema de ficheros"; //$NON-NLS-1$
	}

	@Override
	public byte[] retrievePdfWithCsv(final PdfId pdfId) throws CsvStorerException, CsvFileNotFoundException {
		final String id;
		try {
			id = pdfId.getId() != null ? pdfId.getId() : PdfExtraUtil.getPdfId(pdfId.getPdf());
		}
		catch (final IOException e) {
			LOGGER.severe(
				pdfId.getId() == null ?
					"No se ha indicado el CSV del documento: " + e : //$NON-NLS-1$
						"Id de documento invalido (" + pdfId.getId() + "): " + e //$NON-NLS-1$ //$NON-NLS-2$
			);
			throw new CsvStorerException(
				"No se ha indicado el CSV del documento: " + e, e //$NON-NLS-1$
			);
		}
		final File f = new File(TMP_DIR, id + SUFFIX_CSV + SUFFIX_PDF);
		if (!f.isFile()) {
			throw new CsvFileNotFoundException(
				"No existe el documento con CSV con identificador '" + id + "'" //$NON-NLS-1$ //$NON-NLS-2$
			);
		}
		try (
			final InputStream fis = new FileInputStream(f)
		) {
			return AOUtil.getDataFromInputStream(fis);
		}
		catch (final IOException e) {
			throw new CsvStorerException(
				"No se ha podido leer el documento con identificador '" + id + "' del repositorio: " + e, e //$NON-NLS-1$ //$NON-NLS-2$
			);
		}
	}

	@Override
	public void storePdfWithCsv(final PdfId pdfWithCsv,
			                    final byte[] pdfWithSignatures) throws CsvStorerException {
		if (pdfWithCsv == null) {
			LOGGER.severe(
				"El PDF con CSV no puede ser nulo" //$NON-NLS-1$
			);
			throw new IllegalArgumentException(
				"El PDF con CSV no puede ser nulo" //$NON-NLS-1$
			);
		}

		LOGGER.info("Solicitado almacen de documento en directorio de temporales"); //$NON-NLS-1$

		final File fCsv = new File(
			TMP_DIR,
			pdfWithCsv.getId() + SUFFIX_CSV + SUFFIX_PDF
		);
		try (
			final OutputStream fos = new FileOutputStream(
				fCsv
			)
		) {
			fos.write(pdfWithCsv.getPdf());
			fos.flush();
			fos.close();
		}
		catch (final IOException e) {
			LOGGER.severe(
				"Error guardando el PDF con CSV: " + e //$NON-NLS-1$
			);
			throw new CsvStorerException(
				"Error guardando el PDF con CSV: " + e, e //$NON-NLS-1$
			);
		}
		LOGGER.info(
			"PDF con CSV '" + pdfWithCsv.getId() + "' guardado en: " + fCsv.getAbsolutePath() //$NON-NLS-1$ //$NON-NLS-2$
		);

		final File fSig = new File(
			TMP_DIR,
			pdfWithCsv.getId() + SUFFIX_ORI + SUFFIX_PDF
		);
		if (pdfWithSignatures != null && pdfWithSignatures.length > 0) {
			try (
				final OutputStream fos = new FileOutputStream(
					fSig
				)
			) {
				fos.write(pdfWithSignatures);
				fos.flush();
			}
			catch (final IOException e) {
				LOGGER.severe(
					"Error guardando el PDF con CSV: " + e //$NON-NLS-1$
				);
				throw new CsvStorerException(
					"Error guardando el PDF con CSV: " + e, e //$NON-NLS-1$
				);
			}
		}
		else {
			LOGGER.warning(
				"No se ha proporcionado el PDF original con las firmas" //$NON-NLS-1$
			);
		}

		LOGGER.info(
			"PDF con firmas guardado en: " + fSig.getAbsolutePath() //$NON-NLS-1$
		);

	}

}
