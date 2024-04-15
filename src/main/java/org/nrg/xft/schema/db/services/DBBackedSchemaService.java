package org.nrg.xft.schema.db.services;

import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xft.schema.db.entities.DBBackedSchema;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DBBackedSchemaService extends BaseHibernateService<DBBackedSchema> {
    @Transactional
    DBBackedSchema findConfigByPath(String path);

    @Transactional
    DBBackedSchema createConfig(String path, String name, String content);

    @Transactional
    DBBackedSchema registerNewElement(final String prefix, final String complexType, final String prettyPrintName, final SchemaElement extended);

    List<String> getElementNames(DBBackedSchema config);
}
