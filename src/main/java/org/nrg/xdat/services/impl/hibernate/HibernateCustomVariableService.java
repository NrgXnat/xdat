package org.nrg.xdat.services.impl.hibernate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xdat.daos.CustomVariableDAO;
import org.nrg.xdat.daos.CustomVariableSetDAO;
import org.nrg.xdat.entities.CustomVariable;
import org.nrg.xdat.entities.CustomVariableSet;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.security.services.PermissionsServiceI;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xdat.services.CustomVariableService;
import org.nrg.xdat.services.CustomVariableSetService;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by mike on 3/21/18.
 */
@Service
public class HibernateCustomVariableService extends AbstractHibernateEntityService<CustomVariable, CustomVariableDAO> implements CustomVariableService {
    @Autowired
    public HibernateCustomVariableService(final CustomVariableDAO dao, final SiteConfigPreferences preferences, final UserManagementServiceI userService, final PermissionsServiceI permissionsService) {
        _dao = dao;
        _userService = userService;
        _permissionsService = permissionsService;
    }

    @Override
    @Transactional
    public CustomVariable getVariableFromSet(long setId, String variableName){
        CustomVariable variable = _dao.getVariable(setId, variableName);
        return variable;
    }

    @Override
    protected CustomVariableDAO getDao() {
        return _dao;
    }

    private static final Log _log = LogFactory.getLog(HibernateCustomVariableService.class);

    private final CustomVariableDAO          _dao;
    private final UserManagementServiceI _userService;
    private PermissionsServiceI _permissionsService;
}
