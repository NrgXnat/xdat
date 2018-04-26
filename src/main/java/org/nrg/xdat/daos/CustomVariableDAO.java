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
}
