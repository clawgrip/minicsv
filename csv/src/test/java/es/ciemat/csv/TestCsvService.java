package es.ciemat.csv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import es.gob.afirma.core.misc.Base64;
import es.gob.afirma.core.misc.http.UrlHttpManager;
import es.gob.afirma.core.misc.http.UrlHttpManagerFactory;
import es.gob.afirma.core.misc.http.UrlHttpMethod;

/** Pruebas de los servicios web.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class TestCsvService {

	/** Prueba del servicio de estampaci&oacute;n de CSV.
	 * @throws Exception En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
	//@Ignore
	public void testCsvStampService() throws Exception {

		final byte[] inPdf = Files.readAllBytes(
			Paths.get(
				TestPdfUtils.class.getResource("/cosigned.pdf").toURI() //$NON-NLS-1$
			)
		);

		final UrlHttpManager urlmgr = UrlHttpManagerFactory.getInstalledManager();
		urlmgr.readUrl(
			"http://localhost:8080/csv/CsvService?data=" + Base64.encode(inPdf, true), //$NON-NLS-1$
			UrlHttpMethod.POST
		);

	}

	/** Prueba del servicio de recuperaci&oacute;n de PDF
	 * @throws Exception En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
	//@Ignore
	public void testRetrieveService() throws Exception {
		final UrlHttpManager urlmgr = UrlHttpManagerFactory.getInstalledManager();
		final byte[] pdf = urlmgr.readUrl(
			"http://localhost:8080/csv/CsvRetrieveService?csv=" + "4Vo9P7Oh5fmspCbp9Fzc9x5ZAOw=", //$NON-NLS-1$ //$NON-NLS-2$
			UrlHttpMethod.GET
		);
		final File f = File.createTempFile("CSVRECUPERADO_", ".pdf"); //$NON-NLS-1$ //$NON-NLS-2$
		try (
			final OutputStream fis = new FileOutputStream(f)
		) {
			fis.write(pdf);
			fis.flush();
			fis.close();
		}
		System.out.println("PDF recuperado guardado en: " + f.getAbsolutePath()); //$NON-NLS-1$
	}

	/** Prueba del servicio de estampaci&oacute;n de CSV.
	 * @throws Exception En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
	//@Ignore
	public void testPdfIdService() throws Exception {

		final byte[] inPdf = Files.readAllBytes(
			Paths.get(
				TestPdfUtils.class.getResource("/cosigned.pdf").toURI() //$NON-NLS-1$
			)
		);

		final UrlHttpManager urlmgr = UrlHttpManagerFactory.getInstalledManager();
		final byte[] response = urlmgr.readUrl(
			"http://localhost:8080/csv/IdService?data=" + Base64.encode(inPdf, true), //$NON-NLS-1$
			UrlHttpMethod.POST
		);

		System.out.println(new String(response));
	}

	/** Prueba del servicio de estampaci&oacute;n de CSV.
	 * @throws Exception En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
	//@Ignore
	public void testRetrievePdfDoc() throws Exception {

		final byte[] inPdf = Files.readAllBytes(
			Paths.get(
				TestPdfUtils.class.getResource("/cosigned.pdf").toURI() //$NON-NLS-1$
			)
		);

		final UrlHttpManager urlmgr = UrlHttpManagerFactory.getInstalledManager();
		final byte[] response = urlmgr.readUrl(
			"http://localhost:8080/csv/CsvRetrieveService?file=" + Base64.encode(inPdf, true), //$NON-NLS-1$
			UrlHttpMethod.POST
		);

		System.out.println(new String(response));
	}

}
