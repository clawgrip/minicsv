package es.ciemat.csv.cms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;

import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.core.misc.MimeHelper;

/** Gestor de carga y descarga de documentos en gestor documental. */
public final class CmsDocumentManager  {

	/** Ruta de los documentos en el gestor. */
	public static final String CMS_FOLDER;

	private static final String CMS_USR;
	private static final String CMS_PWD;
	private static final String CMIS_PATH;

	private static final Logger LOGGER = Logger.getLogger(CmsDocumentManager.class.getName());

	static {
		final Properties cfg = new Properties();
		try {
			cfg.load(CmsDocumentManager.class.getResourceAsStream("/csvgestdoc.properties")); //$NON-NLS-1$
		}
		catch (final IOException e) {
			LOGGER.severe("No se ha podido cargar el fichero de configuracion: " + e); //$NON-NLS-1$
			throw new IllegalStateException(
				"No se ha podido cargar el fichero de configuracion: " + e //$NON-NLS-1$
			);
		}

		final String pwd = cfg.getProperty("cmspassword"); //$NON-NLS-1$
		if (pwd == null) {
			LOGGER.severe("No se ha especificado la contrasena del gestor documental en el fichero de configuracion"); //$NON-NLS-1$
			throw new IllegalStateException(
				"No se ha especificado la contrasena del gestor documental en el fichero de configuracion" //$NON-NLS-1$
			);
		}
		CMS_PWD = pwd;

		final String usr = cfg.getProperty("cmsuser"); //$NON-NLS-1$
		if (usr == null) {
			LOGGER.severe("No se ha especificado el usuario del gestor documental en el fichero de configuracion"); //$NON-NLS-1$
			throw new IllegalStateException(
				"No se ha especificado el usuario del gestor documental en el fichero de configuracion" //$NON-NLS-1$
			);
		}
		CMS_USR = usr;

		final String folder = cfg.getProperty("cmsfolder"); //$NON-NLS-1$
		if (folder == null) {
			LOGGER.severe("No se ha especificado la carpeta del gestor documental en el fichero de configuracion"); //$NON-NLS-1$
			throw new IllegalStateException(
				"No se ha especificado la carpeta del gestor documental en el fichero de configuracion" //$NON-NLS-1$
			);
		}
		CMS_FOLDER = folder;

		final String cmisPath = cfg.getProperty("cmispath"); //$NON-NLS-1$
		if (cmisPath == null) {
			LOGGER.severe("No se ha especificado la URL del servicio del gestor documental en el fichero de configuracion"); //$NON-NLS-1$
			throw new IllegalStateException(
				"No se ha especificado la URL del servicio del gestor documental en el fichero de configuracion" //$NON-NLS-1$
			);
		}
		CMIS_PATH = cmisPath;
	}

	private CmsDocumentManager() {
		// No instanciable
	}

	private static Session getSession() {
		final SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
		final Map<String, String> parameter = new HashMap<>();

		parameter.put(SessionParameter.USER, CMS_USR);
		parameter.put(SessionParameter.PASSWORD, CMS_PWD);

		parameter.put(SessionParameter.ATOMPUB_URL, CMIS_PATH);
		parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());

		final Repository repository = sessionFactory.getRepositories(parameter).get(0);
		return repository.createSession();
	}


	/** Recupera un documento desde el CMS.
	 * @param fileName Nombre del fichero a recuperar.
	 * @return Contenido del documento.
	 * @throws CmsFolderNotFoundException Si no existe la carpeta de documentos en el CMS.
	 * @throws FileNoExistsOnCmsException Si el documento no existe en el CMS.
	 * @throws IOException Si no se puede leer el contenido del fichero. */
	public static byte[] loadDocument (final String fileName) throws CmsFolderNotFoundException,
                                                                     FileNoExistsOnCmsException,
                                                                     IOException {
		final Session session = getSession();
		final byte[] ret = leerDocumento(
			fileName,
			CMS_FOLDER,
			session
		);
		session.clear();
		return ret;
	}

	/** Lee un documento en el gestor documental.
	 * @param nombre Nombre del documento.
	 * @param pathAlfresco Ruta del documento en el gestor documental.
	 * @param session Sesi&oacute;n contra el gestor documental.
	 * @return Contenido del documento.
	 * @throws FileNoExistsOnCmsException SI no existe ese documento en la ruta indicada.
	 * @throws IOException Si no se puede leer el contenido del documento. */
	private static byte[] leerDocumento(final String nombre,
			                            final String pathAlfresco,
			                            final Session session) throws FileNoExistsOnCmsException,
	                                                                  IOException {
		   final CmisObject object;
		   try {
			   object = session.getObjectByPath(
				   pathAlfresco + (pathAlfresco.endsWith("/") ? "" : "/") + nombre //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			   );
		   }
		   catch (final CmisObjectNotFoundException e) {
			   LOGGER.severe(
				   "No existe el documento con nombre '" +nombre+ "' en la ruta '" +pathAlfresco+ "'" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			   );
			   throw new FileNoExistsOnCmsException(e);
		   }
		   if (!(object instanceof Document)) {
			   LOGGER.severe(
				   "El objeto recuperado por CMIS " + (object == null ? //$NON-NLS-1$
					   "es nulo" : //$NON-NLS-1$
						   "no es un documento, sino un tipo " + object.getClass().getName()) //$NON-NLS-1$
			   );
			   throw new FileNoExistsOnCmsException();
		   }
		   final Document doc = (Document) object;
		   final ContentStream cs = doc.getContentStream();
		   try (
			   final InputStream stream = cs.getStream();
		   ) {
			   return AOUtil.getDataFromInputStream(stream);
		   }
	}


	/** Sube un documento al gestor documental.
	 * @param fileBytes Contenido del documento local a subir.
	 * @param fileName Nombre en gestor documental del documento a subir.
	 * @throws DocumentAlreadyExistsOnCmsException Si el fichero ya exsite en el gestor documental.
	 * @throws CmsFolderNotFoundException Si no existe la carpeta de destino en el gestor documental. */
	public static void sendDocument(final byte[] fileBytes, final String fileName) throws CmsFolderNotFoundException,
	                                                                                DocumentAlreadyExistsOnCmsException {
		final Session session = getSession();
		enviarDocumento(
			fileName,
			fileBytes,
			CMS_FOLDER,
			session
		);
		session.clear();
	}

	/** Env&iacute;a un documento al gestor documental.
	 * @param nombre Nombre en el gestor documental.
	 * @param pathArchivoreal Nombre del fichero local.
	 * @param pathAlfr Ruta en el gestor documental donde almacenar el fichero.
	 * @throws CmsFolderNotFoundException Si la carpeta del gestor documental no existe.
	 * @throws DocumentAlreadyExistsOnCmsException Si ya existe un fichero con ese nombre en el gestor documental. */
	private static void enviarDocumento(final String nombre,
			                            final byte[] fileBytes,
			                            final String pathAlfresco,
			                            final Session session) throws CmsFolderNotFoundException,
	                                                                  DocumentAlreadyExistsOnCmsException {
		final CmisObject object = session.getObjectByPath(pathAlfresco);

		if(object!=null && object instanceof Folder) {

			if (
				!fileExists(pathAlfresco + (pathAlfresco.endsWith("/") ? "" : "/") + nombre, session) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			) {

				final Map<String, Object> properties2 = new HashMap<>();
				properties2.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document"); //$NON-NLS-1$
				properties2.put(PropertyIds.NAME, nombre);

				final ContentStream contentStream =session.getObjectFactory().createContentStream(nombre, 0, "text/plain",  null); //$NON-NLS-1$

				final Document newDoc = ((Folder)object).createDocument(properties2, contentStream, VersioningState.MAJOR);
				final InputStream stream2 = new ByteArrayInputStream(fileBytes);

				// Obtenemos el MIME-Type
				String mimeType;
				try {
					mimeType = new MimeHelper(fileBytes).getMimeType();
				}
				catch (final IOException e) {
					LOGGER.warning(
						"No se ha podido determinar el tipo del contenido: " + e //$NON-NLS-1$
					);
					mimeType = "application/pdf"; //$NON-NLS-1$
				}

				final ContentStream contentStream2 = session.getObjectFactory().createContentStream(
					nombre,
					fileBytes.length,
					mimeType != null ? mimeType : "application/pdf",  //$NON-NLS-1$
					stream2
				);

				newDoc.setContentStream(contentStream2,true,true);
			}
			else {
				LOGGER.warning(
					"El documento con nombre '" + nombre + "' ya existe en el gestor documental" //$NON-NLS-1$ //$NON-NLS-2$
				);
				throw new DocumentAlreadyExistsOnCmsException();
			}
		}
		else {
			LOGGER.severe("No se ha encontrado la carpeta en el gestor documental"); //$NON-NLS-1$
			throw new CmsFolderNotFoundException();
		}
	}

	/** Indica si un documento existe en el gestor documental
	 * @param fileNameWithPath Nombre (incluyendo ruta en el gestor) del documento a comprobar
	 *                         su existencia.
	 * @param session Sesi&oacute;n contra el gestor documental (si se indica <code>null</code> se
	 *                abre una nueva.
	 * @return <code>true</code> si el documento existe en el gestor, <code>false</code> en casa
	 *         contrario. */
	public static boolean fileExists(final String fileNameWithPath, final Session session)  {
		final Session s = session != null ? session : getSession();
		try {
			return s.getObjectByPath(fileNameWithPath) != null;
		}
		catch (final CmisObjectNotFoundException e) {
			Logger.getLogger(CmsDocumentManager.class.getName()).info(
				"El documento '" + fileNameWithPath + "' no existe en el CMS en la sesion actual: " + e //$NON-NLS-1$ //$NON-NLS-2$
			);
			return false;
		}
	}

}
