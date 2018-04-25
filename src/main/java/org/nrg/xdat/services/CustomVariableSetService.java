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
import org.nrg.xdat.entities.CustomVariable;
import org.nrg.xdat.entities.CustomVariableSet;
import org.nrg.xft.security.UserI;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CustomVariableSetService extends BaseHibernateService<CustomVariableSet> {
    List<CustomVariableSet> getAllForType(String xsiType);
    List<CustomVariableSet> getForTypeFromSetOfSets(String xsiType, List<String> setNames);
    CustomVariableSet getSet(String setName);
    List<CustomVariableSet> getSet(List<String> setNames);
    CustomVariableSet getUserViewableSet(UserI user, String setName);
    CustomVariableSet getUserEditableSet(UserI user, String setName);
    boolean doesVariableNameExistForTypeAndProject(String variableName, String xsiType, String projectId);
    boolean doesVariableNameExistForTypeAndSetsProjects(String variableName, String xsiType, CustomVariableSet set);
    boolean doAnyOfTheseVariablesExistForTypeAndProject(List<CustomVariable> variables, String xsiType, String projectId);
}
