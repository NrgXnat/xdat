package org.nrg.xdat.daos;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xdat.entities.CustomVariableMapping;
import org.nrg.xdat.entities.CustomVariableSet;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by mike on 3/21/18.
 */
@Repository
public class CustomVariableMappingDAO extends AbstractHibernateDAO<CustomVariableMapping> {

    public boolean hasMapping(String projectId, String setName) {
        List mappings = getMappings(projectId,setName);
        if(mappings!=null &&mappings.size()>0){
            return true;
        }
        else{
            return false;
        }
    }

    public List<CustomVariableMapping> getMappingsForProjects(List<String> projectIds) {
        Criteria criteria = getCriteriaForType();
        if(projectIds!=null && projectIds.size()>0) {
            criteria.add(Restrictions.in("projectId", projectIds));
        }
        return criteria.list();
    }

    public List<CustomVariableMapping> getMappings(String projectId, String setName) {
        Criteria criteria = getCriteriaForType();
        if(projectId!=null) {
            criteria.add(Restrictions.eq("projectId", projectId));
        }
        if(setName!=null) {
            criteria.add(Restrictions.eq("setName", setName));
        }
        return criteria.list();
    }

    public List<CustomVariableMapping> getMappingsForProject(String projectId) {
        return getMappings(projectId, null);
    }

    public List<CustomVariableMapping> getMappingsForSet(String setName) {
        return getMappings(null, setName);
    }
}
