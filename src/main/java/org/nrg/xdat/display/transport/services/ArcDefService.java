package org.nrg.xdat.display.transport.services;

import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xdat.display.ArcDefinition;
import org.nrg.xdat.display.transport.entities.DisplayArcDefinitionDB;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Tim Olsen
 *
 */

public interface ArcDefService  extends BaseHibernateService<DisplayArcDefinitionDB> {
    /**
     * Find stored ArcDefinition by its name
     * @param name
     * @return
     */
    @Transactional
    ArcDefinition findArcDefByName(final String name);

    /**
     * Get all enabled Arc Definitions
     * @return
     */
    @Transactional
    List<ArcDefinition> findAllArcDefs();

    /**
     * Get ArcDefinition by its name
     * @param name
     */
    @Transactional
    void deleteByName(final String name);


    /**
     * Create specified ArcDefinition
     * @param tempAD
     * @return
     */
    @Transactional
    DisplayArcDefinitionDB createArcDef(final ArcDefinition tempAD);
}
