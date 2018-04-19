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

import java.util.List;

public interface CustomVariableSetService extends BaseHibernateService<CustomVariableSet> {
    List<CustomVariableSet> getUserViewableSets(UserI user);
    List<CustomVariableSet> getAllForType(String xsiType);
    List<CustomVariableSet> getUserViewableSetsForType(UserI user, String xsiType);
    CustomVariableSet getSet(String setName);
    CustomVariableSet getUserViewableSet(UserI user, String setName);
    CustomVariableSet getUserEditableSet(UserI user, String setName);
}
