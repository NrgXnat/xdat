package org.nrg.xdat.services.impl.hibernate;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xdat.daos.CustomVariableDAO;
import org.nrg.xdat.daos.CustomVariableMappingDAO;
import org.nrg.xdat.daos.CustomVariableSetDAO;
import org.nrg.xdat.entities.CustomVariable;
import org.nrg.xdat.entities.CustomVariableMapping;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mike on 3/21/18.
 */
@Service
public class HibernateCustomVariableSetService extends AbstractHibernateEntityService<CustomVariableSet, CustomVariableSetDAO> implements CustomVariableSetService {
    @Autowired
    public HibernateCustomVariableSetService(final CustomVariableSetDAO dao, CustomVariableDAO varDao, CustomVariableMappingDAO mappingDao, final SiteConfigPreferences preferences, final UserManagementServiceI userService, final PermissionsServiceI permissionsService) {
        _varDao = varDao;
        _dao = dao;
        _mappingDao = mappingDao;
        _userService = userService;
        _permissionsService = permissionsService;
    }

    @Override
    @Transactional
    public List<CustomVariableSet> getAllForType(String xsiType){
        return _dao.findSetsForType(xsiType);
    }

    @Override
    @Transactional
    public List<CustomVariableSet> getForTypeFromSetOfSets(String xsiType, List<String>setNames){
        return _dao.findSetsForTypeFromSetOfSets(xsiType, setNames);
    }

    @Override
    @Transactional
    public CustomVariableSet getSet(String setName){
        CustomVariableSet set = _dao.getSet(setName);
        return set;
    }

    @Override
    @Transactional
    public List<CustomVariableSet> getSet(List<String> setNames){
        List<CustomVariableSet> set = _dao.getSets(setNames);
        return set;
    }

    @Override
    @Transactional
    public CustomVariableSet getUserViewableSet(UserI user, String setName){
        List<CustomVariableMapping> mappings = _mappingDao.getMappingsForSet(setName);
        CustomVariableSet set = _dao.getSet(setName);
        if(!set.isProjectSpecific()){
            return set;
        }
        else {
            List<String> usersProjects = _permissionsService.getUserReadableProjects(user);
            for (CustomVariableMapping mapping : mappings) {
                if (usersProjects.contains(mapping.getProjectId())) {
                    return set;
                }
            }
        }
        return null;
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

    @Override
    @Transactional
    public boolean doesVariableNameExistForTypeAndProject(String variableName, String xsiType, String projectId){
        List<CustomVariableSet> sets = _dao.findSetsForType(xsiType);
        for(CustomVariableSet set: sets) {
            if(_mappingDao.hasMapping(projectId,set.getName())){
                List<CustomVariable> variables = set.getVariables();
                for(CustomVariable variable: variables){
                    if(StringUtils.equalsIgnoreCase(variable.getName(),variableName)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    @Transactional
    public boolean doesVariableNameExistForTypeAndSetsProjects(String variableName, String xsiType, CustomVariableSet setToCheck){
        List<CustomVariableSet> sets = _dao.findSetsForType(xsiType);
        List<CustomVariableMapping> mappings = _mappingDao.getMappingsForSet(setToCheck.getName());
        for(CustomVariableMapping mapping: mappings){
            for(CustomVariableSet set: sets) {
                if(_mappingDao.hasMapping(mapping.getProjectId(),set.getName())){
                    List<CustomVariable> variables = set.getVariables();
                    for(CustomVariable variable: variables){
                        if(StringUtils.equalsIgnoreCase(variable.getName(),variableName)){
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    @Transactional
    public boolean doAnyOfTheseVariablesExistForTypeAndProject(List<CustomVariable> variablesToLookFor, String xsiType, String projectId){
        List<String> variableNamesToLookFor = new ArrayList<>();
        for(CustomVariable var : variablesToLookFor){
            variableNamesToLookFor.add(var.getName());
        }
        List<CustomVariableSet> sets = _dao.findSetsForType(xsiType);
        for(CustomVariableSet set: sets) {
            if(_mappingDao.hasMapping(projectId,set.getName())){
                List<String> variableNamesToLookThrough = new ArrayList<>();
                for(CustomVariable checkVar : variablesToLookFor){
                    for(CustomVariable existingVar : set.getVariables()) {
                        if (StringUtils.equalsIgnoreCase(checkVar.getName(), existingVar.getName())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected CustomVariableSetDAO getDao() {
        return _dao;
    }

    private static final Log _log = LogFactory.getLog(HibernateCustomVariableSetService.class);

    private final CustomVariableSetDAO          _dao;
    private final CustomVariableDAO _varDao;
    private final CustomVariableMappingDAO _mappingDao;
    private final UserManagementServiceI _userService;
    private PermissionsServiceI _permissionsService;
}
