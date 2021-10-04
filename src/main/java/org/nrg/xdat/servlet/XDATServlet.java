/*
 * core: org.nrg.xdat.servlet.XDATServlet
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xdat.servlet;

import lombok.extern.slf4j.Slf4j;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.velocity.loaders.CustomClasspathResourceLoader;
import org.nrg.xft.db.DatabaseUpdater;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.nio.file.Paths;
import java.util.logging.Handler;
import java.util.logging.LogManager;

/**
 * @author Tim
 */
@Slf4j
public class XDATServlet extends HttpServlet {
    public static String WEBAPP_ROOT;

    public static Boolean shouldUpdateViews() {
        return _shouldUpdateViews;
    }

    public static boolean isDatabasePopulateOrUpdateCompleted() {
        return _isDatabasePopulateOrUpdateCompleted;
    }

    public void init(ServletConfig config) throws ServletException {
        replaceLogging();

        super.init(config);

        try {
            //stores some paths for convenient access later.
            XDATServlet.WEBAPP_ROOT = getWebAppPath();

            // Migration: Fix this to use NIO Paths.
            XDAT.init(getWebAppPath("WEB-INF", "conf"), true);

            //store some  more convenience paths
            XDAT.setScreenTemplatesFolder(getWebAppPath("templates", "screens"));
            for (final String path : CustomClasspathResourceLoader.TEMPLATE_PATHS) {
                XDAT.addScreenTemplatesFolder(path, new File(getWebAppPath(path, "screens")));
            }

            final DatabaseUpdater updater = new DatabaseUpdater(XDAT.getJdbcTemplate(), getWebAppPath("resources", "default-sql"));
            updater.populateOrUpdateDatabase();
            _shouldUpdateViews = updater.shouldUpdateViews();
            _isDatabasePopulateOrUpdateCompleted = updater.isDatabasePopulateOrUpdateCompleted();
        } catch (Exception e) {
            log.error("An error occurred trying to initialize the XDAT servlet", e);
        }
    }

    private String getWebAppPath(final String... paths) {
        return Paths.get(getServletContext().getRealPath(""), paths).toString();
    }

    private void replaceLogging() {
        // remove the java.util.logging handlers so that nothing is logged to stdout/stderr
        java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
        for (Handler h : rootLogger.getHandlers()) {
            rootLogger.removeHandler(h);
        }

        try {
            // use the sl4j bridge to redirect restlet logging to log4j
            SLF4JBridgeHandler.install();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static Boolean _shouldUpdateViews;
    private static Boolean _isDatabasePopulateOrUpdateCompleted = false;
}
