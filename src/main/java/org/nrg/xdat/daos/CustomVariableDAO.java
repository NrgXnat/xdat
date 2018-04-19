package org.nrg.xdat.daos;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xdat.entities.CustomVariable;
import org.nrg.xdat.entities.CustomVariableSet;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by mike on 3/21/18.
 */
@Repository
public class CustomVariableDAO extends AbstractHibernateDAO<CustomVariable> {
    public CustomVariable getVariable(long setId, String variableName){
        Criteria variableCriteria = getSession().createCriteria(CustomVariable.class);
        variableCriteria.add(Restrictions.eq("name", variableName));
        variableCriteria.add(Restrictions.eq("variables", setId));
        List<CustomVariable> results = variableCriteria.list();
        if (results.size() == 0) {
            return null;
        }
        return results.get(0);
    }
}
