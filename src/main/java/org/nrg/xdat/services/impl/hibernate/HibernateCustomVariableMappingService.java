package org.nrg.xdat.services.impl.hibernate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xdat.daos.CustomVariableDAO;
import org.nrg.xdat.daos.CustomVariableMappingDAO;
import org.nrg.xdat.entities.CustomVariable;
import org.nrg.xdat.entities.CustomVariableMapping;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.services.PermissionsServiceI;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xdat.services.CustomVariableMappingService;
import org.nrg.xdat.services.CustomVariableService;
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
public class HibernateCustomVariableMappingService extends AbstractHibernateEntityService<CustomVariableMapping, CustomVariableMappingDAO> implements CustomVariableMappingService {
    @Autowired
    public HibernateCustomVariableMappingService(final CustomVariableMappingDAO dao, final SiteConfigPreferences preferences, final UserManagementServiceI userService, final PermissionsServiceI permissionsService) {
        _dao = dao;
        _userService = userService;
        _permissionsService = permissionsService;
    }

    @Override
    @Transactional
    public boolean createMappingIfDoesntExist(String projectId, String setName){
        if(!_dao.hasMapping(projectId,setName)) {
            _dao.create(new CustomVariableMapping(projectId, setName));
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean deleteMappingIfExists(String projectId, String setName){
        if(_dao.hasMapping(projectId,setName)) {
            List<CustomVariableMapping> mappings = _dao.getMappings(projectId, setName);
            for(CustomVariableMapping mapping: mappings){
                _dao.delete(mapping);
            }
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public void deleteMappingsForSet(String setName){
        List<CustomVariableMapping> mappings = _dao.getMappingsForSet(setName);
        if(mappings!=null){
            for(CustomVariableMapping mapping: mappings){
                _dao.delete(mapping);
            }
        }
    }

    @Override
    @Transactional
    public List<String> getSetsForProject(String projectId){
        List<CustomVariableMapping> mappings = _dao.getMappingsForProject(projectId);
        List<String> sets = new ArrayList<>();
        if(mappings!=null){
            for(CustomVariableMapping mapping: mappings){
                sets.add(mapping.getSetName());
            }
        }
        return sets;
    }

    @Override
    @Transactional
    public List<String> getSetsForProjects(List<String> projectIds){
        List<CustomVariableMapping> mappings = _dao.getMappingsForProjects(projectIds);
        List<String> sets = new ArrayList<>();
        if(mappings!=null){
            for(CustomVariableMapping mapping: mappings){
                sets.add(mapping.getSetName());
            }
        }
        return sets;
    }
    @Override
    protected CustomVariableMappingDAO getDao() {
        return _dao;
    }

    private static final Log _log = LogFactory.getLog(HibernateCustomVariableMappingService.class);

    private final CustomVariableMappingDAO          _dao;
    private final UserManagementServiceI _userService;
    private PermissionsServiceI _permissionsService;
}
