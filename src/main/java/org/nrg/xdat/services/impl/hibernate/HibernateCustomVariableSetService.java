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
public class HibernateCustomVariableSetService extends AbstractHibernateEntityService<CustomVariableSet, CustomVariableSetDAO> implements CustomVariableSetService {
    @Autowired
    public HibernateCustomVariableSetService(final CustomVariableSetDAO dao, CustomVariableDAO varDao, final SiteConfigPreferences preferences, final UserManagementServiceI userService, final PermissionsServiceI permissionsService) {
        _varDao = varDao;
        _dao = dao;
        _userService = userService;
        _permissionsService = permissionsService;
    }

    @Override
    @Transactional
    public List<CustomVariableSet> getUserViewableSets(UserI user){
        if(Roles.isSiteAdmin(user)){
            return _dao.findAll();
        }
        else {
            return _dao.findSetsAvailableToProjects(_permissionsService.getUserReadableProjects(user));
        }
    }

    @Override
    @Transactional
    public List<CustomVariableSet> getAllForType(String xsiType){
        return _dao.findSetsForType(xsiType);
    }

    @Override
    @Transactional
    public List<CustomVariableSet> getUserViewableSetsForType(UserI user, String xsiType){
        return _dao.findSetsAvailableToProjectsForType(_permissionsService.getUserReadableProjects(user), xsiType);
    }

//
//    @Override
//    @Transactional
//    public void saveSet(CustomVariableSet newCustomVariableSet){
//        _dao.saveSet(newCustomVariableSet);
//        return;
//    }


    @Override
    @Transactional
    public CustomVariableSet getSet(String setName){
        CustomVariableSet set = _dao.getSet(setName);
        return set;
    }

    @Override
    @Transactional
    public CustomVariableSet getUserViewableSet(UserI user, String setName){
        CustomVariableSet set = _dao.getSet(setName);
        if(!set.isProjectSpecific() || _permissionsService.getUserReadableProjects(user).contains(set.getOwningProjectId())){
            return set;
        }
        else{
            return null;
        }
    }

    @Override
    @Transactional
    public CustomVariableSet getUserEditableSet(UserI user, String setName){
        CustomVariableSet set = _dao.getSet(setName);
        if(_permissionsService.getUserEditableProjects(user).contains(set.getOwningProjectId())){
            return set;
        }
        else{
            return null;
        }
    }

//    @Override
//    @Transactional
//    public CustomVariable createVariable(CustomVariable variable){
//        CustomVariableSet parent = variable.getParentSet();
//        List<CustomVariable> parentVariables = parent.getVariables();
//        parentVariables.add(variable);
//        parent.setVariables(parentVariables);
//        return variable;
//    }


    @Override
    protected CustomVariableSetDAO getDao() {
        return _dao;
    }

    private static final Log _log = LogFactory.getLog(HibernateCustomVariableSetService.class);

    private final CustomVariableSetDAO          _dao;
    private final CustomVariableDAO _varDao;
    private final UserManagementServiceI _userService;
    private PermissionsServiceI _permissionsService;
}
