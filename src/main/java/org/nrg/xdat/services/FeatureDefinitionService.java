/*
 * core: org.nrg.xdat.services.FeatureDefinitionService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xdat.services;

import java.sql.SQLException;
import java.util.List;

import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xapi.model.FeatureDefinitionUserGroupDto;
import org.nrg.xdat.entities.FeatureDefinition;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.security.UserI;

public interface FeatureDefinitionService extends BaseHibernateService<FeatureDefinition> {
    /**
     * Finds the specified role definition
     *
     * @param key The role key
     * @return The {@link FeatureDefinition feature definition} for the specified key.
     */
    FeatureDefinition findFeatureByKey(String key);
    
    List<FeatureDefinition> findAll();
    
    void findAllByTags(UserI user, String [] tags);
    
    List<FeatureDefinitionUserGroupDto> findAllByType(UserI user, String type) throws SQLException, DBPoolException ;
    
}
