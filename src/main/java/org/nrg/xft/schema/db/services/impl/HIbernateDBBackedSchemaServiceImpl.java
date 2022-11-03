/*
 * core: org.nrg.xdat.services.impl.hibernate.HibernateAliasTokenService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*
 * HibernateAliasTokenService
 * (C) 2016 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 */
package org.nrg.xft.schema.db.services.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.framework.generics.GenericUtils;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xdat.display.DisplayManager;
import org.nrg.xdat.display.ElementDisplay;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xdat.servlet.XDATServlet;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.generators.JavaFileGenerator;
import org.nrg.xft.generators.SQLUpdateGenerator;
import org.nrg.xft.meta.XFTMetaManager;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.schema.XFTDataModel;
import org.nrg.xft.schema.XFTElement;
import org.nrg.xft.schema.XFTManager;
import org.nrg.xft.schema.XFTSchema;
import org.nrg.xft.schema.db.entities.DBBackedSchema;
import org.nrg.xft.schema.db.services.DBBackedSchemaService;
import org.nrg.xft.schema.db.dao.DBBackedSchemaDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class HIbernateDBBackedSchemaServiceImpl extends AbstractHibernateEntityService<DBBackedSchema, DBBackedSchemaDAO> implements DBBackedSchemaService {
    @Autowired
    public HIbernateDBBackedSchemaServiceImpl(final DBBackedSchemaDAO dao) {
        _dao = dao;
    }

    /**
     * Finds the DBBackedSchemaDAO by path
     *
     * @param path The path the file was found at
     *
     * @return
     */
    @Override
    @Transactional
    public DBBackedSchema findConfigByPath(String path) {
        return getDao().findByPath(path,false);
    }



    @Override
    @Transactional
    public DBBackedSchema createConfig(String path, String name, String content){
        DBBackedSchema config = new DBBackedSchema();
        config.setContent(content);
        config.setPath(path);
        config.setName(name);

        this.getDao().create(config);

        try {
            XFTDataModel model = XFTManager.GetInstance().addDBSchema(config);

        } catch (IOException e) {
            _log.error("",e);
        } catch (XFTInitException e) {
            _log.error("",e);
        } catch (ElementNotFoundException e) {
            _log.error("",e);
        }

        return getDao().findByPath(path,false);
    }

    @Override
    @Transactional
    public DBBackedSchema registerNewElement(String prefix, String complexType, String prettyPrintName, SchemaElement extended) {
        final String proposedType = String.format("%1$s:%2$s",prefix,complexType);
        final String proposedName = String.format("%1$s:%2$s",prefix,prettyPrintName);

        try {
            if(SchemaElement.GetElement(proposedType)!=null){
                //duplicate
                return null;
            }
        } catch (Exception e) {
            //safe
        }

        try {
            if(SchemaElement.GetElement(proposedName)!=null){
                //duplicate
                return null;
            }
        } catch (Exception e) {
            //safe
        }

        final String xsd = String.format(schemaTemplate,prefix,complexType,prettyPrintName,extended);

        final DBBackedSchema schemaConfig = this.createConfig("/custom/".concat(complexType),proposedType,xsd);

        //configure displaydoc
//        try {
//            GenericWrapperElement se = GenericWrapperElement.GetElement(proposedType);
//            final StringBuilder displayXML = (new JavaFileGenerator()).generateDisplayXml(se);
//            ElementDisplay ed = DisplayManager.GetInstance().registerDisplay(displayXML);
//        } catch (XFTInitException e) {
//            _log.error("",e);
//        } catch (ElementNotFoundException e) {
//            _log.error("",e);
//        }


        XDATServlet.load();
//
//        //register datatype
//        try {
//            final ElementSecurity elementSecurity = ElementSecurity.newElementSecurity(proposedType,prettyPrintName);
//            elementSecurity.initExistingPermissions();
//            ElementSecurity.refresh();
//        } catch (Exception e) {
//            _log.error("",e);
//        }

//
//        //update history tables, etc
//        try {
//            XFTManager.GetInstance().manageAddins(prefix);
//        } catch (Exception e) {
//            _log.error("",e);
//        }
//

//
//        try {
//            updateDB();
//        } catch (Exception e) {
//            _log.error("",e);
//        }

        return schemaConfig;
    }

    public static final Map<String,List<String>> elementsPerConfig = Maps.newHashMap();

    @Override
    public List<String> getElementNames(DBBackedSchema config) {
        if(!elementsPerConfig.containsKey(config.getName())){
            elementsPerConfig.put(config.getName(), Lists.newArrayList());

            for (final DBBackedSchema schema: XFTManager.getDBBackedSchemaService().getAll()) {
                final String name = schema.getName();

                //iterate active schema to identify elements for this schema
                for (final XFTSchema s : XFTManager.GetSchemas()) {
                    if (StringUtils.equals(name, s.getDataModel().getFileName())) {
                        for (final Object o : s.getSortedElements()) {
                            final XFTElement e = (XFTElement) o;
                            final String xsiType= e.getType().getFullForeignType();
                            elementsPerConfig.get(config.getName()).add(xsiType);
                        }
                    }
                }
            }
        }

        return elementsPerConfig.get(config.getName());
    }

    private void updateDB() throws Exception {
        final List[]       createSql = SQLUpdateGenerator.GetSQLCreate();
        final List<String> sql       = GenericUtils.convertToTypedList(createSql[0], String.class);
        if (!sql.isEmpty()) {
            final XDATServlet.DatabaseUpdater databaseUpdater = new XDATServlet.DatabaseUpdater(null, XDATServlet.getGeneratedSqlLogPath(), "-- Generated SQL for updating XNAT database schema");//user_count==0 means users need to be created.
            databaseUpdater.addStatements(sql);
            databaseUpdater.run();// use run to prevent a second thread.
        }
    }

    @Override
    protected DBBackedSchemaDAO getDao() {
        return _dao;
    }

    private static final Log _log = LogFactory.getLog(HIbernateDBBackedSchemaServiceImpl.class);

    private final DBBackedSchemaDAO _dao;

    private final static String schemaTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema targetNamespace=\"http://nrg.wustl.edu/%1$s\" xmlns:%1$s=\"http://nrg.wustl.edu/%1$s\" xmlns:xnat=\"http://nrg.wustl.edu/xnat\" xmlns:xdat=\"http://nrg.wustl.edu/xdat\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\n" +
            "\t<xs:import namespace=\"http://nrg.wustl.edu/xdat\" schemaLocation=\"../xdat/xdat.xsd\"/>\n" +
            "\t<xs:import namespace=\"http://nrg.wustl.edu/xnat\" schemaLocation=\"../xnat/xnat.xsd\"/>\n" +
            "\t<xs:element name=\"%3$s\" type=\"%1$s:%2$s\"/>" +
            "\t<xs:complexType name=\"%2$s\">\n" +
            "\t\t<xs:complexContent>\n" +
            "\t\t\t<xs:extension base=\"%4$s\">\n" +
            "\t\t\t\t<xs:sequence>\n" +
            "\t\t\t\t\t<xs:element name=\"data\" type=\"xdat:jsonb\"  minOccurs=\"0\"/>\n" +
            "\t\t\t\t</xs:sequence>\n" +
            "\t\t\t</xs:extension>\n" +
            "\t\t</xs:complexContent>\n" +
            "\t</xs:complexType>\n" +
            "</xs:schema>";
}
