package es.ciemat.csv;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Ignore;
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
	@Ignore
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

	/** Prueba del servicio de estampaci&oacute;n de CSV.
	 * @throws Exception En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
	@Ignore
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

}
