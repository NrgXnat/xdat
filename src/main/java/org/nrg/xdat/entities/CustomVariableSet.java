/*
 * core: org.nrg.xdat.entities.AliasToken
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*
 * AliasToken
 * (C) 2016 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 */
package org.nrg.xdat.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.*;
import java.util.List;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class CustomVariableSet extends AbstractHibernateEntity {
    private static final long serialVersionUID = 1L;

    public CustomVariableSet() {

    }

    public CustomVariableSet(String _name, String _datatype, String _description, boolean _projectSpecific, List<CustomVariable> _variables, String _owningProjectId) {
        this.name = _name;
        this.datatype = _datatype;
        this.description = _description;
        this.projectSpecific = _projectSpecific;
        this.variables = _variables;
        this.owningProjectId = _owningProjectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String _name) {
        this.name = _name;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String _datatype) {
        this.datatype = _datatype;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String _description) {
        this.description = _description;
    }

    public boolean isProjectSpecific() {
        return projectSpecific;
    }

    public void setProjectSpecific(boolean _projectSpecific) {
        this.projectSpecific = _projectSpecific;
    }

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(referencedColumnName = "id")
    public List<CustomVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<CustomVariable> _variables) {
        this.variables = _variables;
    }

    public String getOwningProjectId() {
        return owningProjectId;
    }

    public void setOwningProjectId(String _owningProjectId) {
        this.owningProjectId = _owningProjectId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CustomVariableSet that = (CustomVariableSet) o;

        if (projectSpecific != that.projectSpecific) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (datatype != null ? !datatype.equals(that.datatype) : that.datatype != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (variables != null ? !variables.equals(that.variables) : that.variables != null) return false;
        return owningProjectId != null ? owningProjectId.equals(that.owningProjectId) : that.owningProjectId == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (datatype != null ? datatype.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (projectSpecific ? 1 : 0);
        result = 31 * result + (variables != null ? variables.hashCode() : 0);
        result = 31 * result + (owningProjectId != null ? owningProjectId.hashCode() : 0);
        return result;
    }

    private static final Log log = LogFactory.getLog(CustomVariableSet.class);

    private String                       name;
    private String                       datatype;
    private String                       description;
    private boolean                      projectSpecific;
    private List<CustomVariable>         variables;
    private String                       owningProjectId;
}
