/*
 * core: org.nrg.xdat.services.AliasTokenService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*
 * AliasTokenService
 * (C) 2016 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 */
package org.nrg.xdat.services;

import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xdat.entities.CustomVariableMapping;
import org.nrg.xft.security.UserI;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CustomVariableMappingService extends BaseHibernateService<CustomVariableMapping> {
    boolean createMappingIfDoesntExist(String projectId, String setName);
    boolean deleteMappingIfExists(String projectId, String setName);
    void deleteMappingsForSet(String setName);
    List<String> getSetsForProject(String projectId);
    List<String> getSetsForProjects(List<String> projectIds);
}
