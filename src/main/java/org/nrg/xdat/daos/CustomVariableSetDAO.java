package org.nrg.xdat.daos;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xdat.entities.CustomVariable;
import org.nrg.xdat.entities.CustomVariableSet;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xft.security.UserI;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mike on 3/21/18.
 */
@Repository
public class CustomVariableSetDAO extends AbstractHibernateDAO<CustomVariableSet> {

    public CustomVariableSet getSet(String setName){
        Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("name", setName));
        List<CustomVariableSet> results = criteria.list();
        if (results.size() == 0) {
            return null;
        }
        return results.get(0);
    }

    public List<CustomVariableSet> findSetsAvailableToProjects(List<String> projectIds) {
        if(projectIds==null || projectIds.size()==0){
            return new ArrayList<CustomVariableSet>();
        }
        Criteria criteria = getCriteriaForType();
        Criterion isPublicCustomVariableSet = Restrictions.eq("projectSpecific", false);
        Criterion isOwnedByOneOfTheSpecifiedProjects= Restrictions.in("owningProjectId", projectIds);
        criteria.add(Restrictions.or(isPublicCustomVariableSet, isOwnedByOneOfTheSpecifiedProjects));
        return criteria.list();
    }

    public List<CustomVariableSet> findSetsForTypeFromSetOfSets(String xsiType, List<String> setNames) {
        if(setNames==null || setNames.size()==0){
            return new ArrayList<CustomVariableSet>();
        }
        Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("datatype", xsiType));
        criteria.add(Restrictions.in("name", setNames));
        return criteria.list();
    }

    public List<CustomVariableSet> getSets(List<String> setNames) {
        Criteria criteria = getCriteriaForType();
        if(setNames!=null && setNames.size()>0) {
            Criterion isOwnedByOneOfTheSpecifiedProjects = Restrictions.in("name", setNames);
        }
        else{
            return new ArrayList<CustomVariableSet>();
        }
        return criteria.list();
    }

    public List<CustomVariableSet> findSetsForType(String xsiType) {
        Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("datatype", xsiType));
        return criteria.list();
    }

    public List<CustomVariableSet> findSetsOwnedByProject(String projectId) {
        Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("owningProjectId", projectId));
        return criteria.list();
    }

//    public List<CustomVariableSet> findSetsAvailableToProjectsForType(List<String> projectIds, String xsiType) {
//        Criteria criteria = getCriteriaForType();
//        Criterion isPublicCustomVariableSet = Restrictions.eq("projectSpecific", false);
//        Criterion isOwnedByOneOfTheSpecifiedProjects= Restrictions.in("owningProjectId", projectIds);
//
//        Criterion hasAccess = Restrictions.or(isPublicCustomVariableSet, isOwnedByOneOfTheSpecifiedProjects);
//        Criterion isOfType = Restrictions.eq("datatype", xsiType);
//
//        criteria.add(Restrictions.and(hasAccess, isOfType));
//        return criteria.list();
//    }

    @Inject
    private SessionFactory _sessionFactory;
}
