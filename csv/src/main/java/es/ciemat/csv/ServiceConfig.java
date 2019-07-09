package es.ciemat.csv;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/** Configuraci&oacute;n del servicio.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
@WebListener
public final class ServiceConfig implements ServletContextListener {

	/** Modo de depuraci&oacute;n. */
	public static final boolean DEBUG = true;

	private static final Properties CFG = new Properties();

	private static final String KEY_STORER_CLASSNAME = "csvstorer"; //$NON-NLS-1$

	private static CsvStorer csvStorer = null;

	private static final Logger LOGGER = Logger.getLogger(ServiceConfig.class.getName());

    @Override
	public void contextInitialized(final ServletContextEvent event) {
    	if (DEBUG) {
    		LOGGER.info(
				"Inicializando la lectura de configuracion del servicio de CSV..." //$NON-NLS-1$
			);
    	}
        try {
			CFG.load(ServiceConfig.class.getResourceAsStream("/service.properties")); //$NON-NLS-1$
		}
        catch (final IOException | NullPointerException e) {
        	LOGGER.severe(
				"No se ha podido cargar el fichero de configuracion del servicio '" +  //$NON-NLS-1$
					ServiceConfig.class.getResource("/service.properties") + "': " + e //$NON-NLS-1$ //$NON-NLS-2$
			);
			throw new IllegalStateException(
				"No se ha podido cargar el fichero de configuracion del servicio '" +  //$NON-NLS-1$
					ServiceConfig.class.getResource("/service.properties") + "': " + e //$NON-NLS-1$ //$NON-NLS-2$
			);
		}
        if (DEBUG) {
        	Logger.getLogger(ServiceConfig.class.getName()).warning(
    			"Modo de depuracion activo para el sistema de CSV" //$NON-NLS-1$
			);
        }
    }

    static CsvStorer getCsvStorer() {
    	if (ServiceConfig.csvStorer == null) {
    		final String storerClassName = CFG.getProperty(KEY_STORER_CLASSNAME);
    		if (storerClassName == null) {
    			LOGGER.severe(
					"No se ha definido en la configuracion el valor del parametro '" + KEY_STORER_CLASSNAME + "'" //$NON-NLS-1$ //$NON-NLS-2$
				);
    			throw new IllegalStateException(
					"No se ha definido en la configuracion el valor del parametro '" + KEY_STORER_CLASSNAME + "'" //$NON-NLS-1$ //$NON-NLS-2$
				);
    		}
    		try {
				csvStorer = (CsvStorer) Class.forName(storerClassName).getConstructor().newInstance();
			}
    		catch (final InstantiationException    |
    			         IllegalAccessException    |
    			         IllegalArgumentException  |
    			         InvocationTargetException |
    			         NoSuchMethodException     |
    			         SecurityException         |
    			         ClassNotFoundException e) {
    			LOGGER.severe(
					"No se ha podido instanciar la clase de almacen de CSV ('" + storerClassName + "'): " + e //$NON-NLS-1$ //$NON-NLS-2$
				);
    			throw new IllegalStateException(
					"No se ha podido instanciar la clase de almacen de CSV ('" + storerClassName + "'): " + e //$NON-NLS-1$ //$NON-NLS-2$
				);
			}
    	}
    	LOGGER.info(
			"Se usara el almacenador de documentos: " + csvStorer //$NON-NLS-1$
		);
    	return ServiceConfig.csvStorer;
    }

}
