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

import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;
import org.nrg.framework.utilities.Patterns;
import org.nrg.xdat.preferences.SiteConfigPreferences;

import javax.persistence.*;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class CustomVariableSet extends AbstractHibernateEntity {
    private static final long serialVersionUID = 1L;

    public CustomVariableSet() {

    }

    public CustomVariableSet(String _name, String _datatype, String _description, boolean _projectSpecific, List<CustomVariable> _variables, String _owningProjectId) {
        this._name = _name;
        this._datatype = _datatype;
        this._description = _description;
        this._projectSpecific = _projectSpecific;
        this._variables = _variables;
        this._owningProjectId = _owningProjectId;
    }

    public String getName() {
        return _name;
    }

    public void setName(String _name) {
        this._name = _name;
    }

    public String getDatatype() {
        return _datatype;
    }

    public void setDatatype(String _datatype) {
        this._datatype = _datatype;
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String _description) {
        this._description = _description;
    }

    public boolean isProjectSpecific() {
        return _projectSpecific;
    }

    public void setProjectSpecific(boolean _projectSpecific) {
        this._projectSpecific = _projectSpecific;
    }

    @OneToMany(mappedBy = "_parentSet", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    public List<CustomVariable> getVariables() {
        return _variables;
    }

    public void setVariables(List<CustomVariable> _variables) {
        this._variables = _variables;
    }

    public String getOwningProjectId() {
        return _owningProjectId;
    }

    public void setOwningProjectId(String _owningProjectId) {
        this._owningProjectId = _owningProjectId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CustomVariableSet that = (CustomVariableSet) o;

        if (_projectSpecific != that._projectSpecific) return false;
        if (_name != null ? !_name.equals(that._name) : that._name != null) return false;
        if (_datatype != null ? !_datatype.equals(that._datatype) : that._datatype != null) return false;
        if (_description != null ? !_description.equals(that._description) : that._description != null) return false;
        if (_variables != null ? !_variables.equals(that._variables) : that._variables != null) return false;
        return _owningProjectId != null ? _owningProjectId.equals(that._owningProjectId) : that._owningProjectId == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_name != null ? _name.hashCode() : 0);
        result = 31 * result + (_datatype != null ? _datatype.hashCode() : 0);
        result = 31 * result + (_description != null ? _description.hashCode() : 0);
        result = 31 * result + (_projectSpecific ? 1 : 0);
        result = 31 * result + (_variables != null ? _variables.hashCode() : 0);
        result = 31 * result + (_owningProjectId != null ? _owningProjectId.hashCode() : 0);
        return result;
    }

    private static final Log _log = LogFactory.getLog(CustomVariableSet.class);

    private String                       _name;
    private String                       _datatype;
    private String                       _description;
    private boolean                      _projectSpecific;
    private List<CustomVariable>         _variables;
    private String                       _owningProjectId;
}
