package org.nrg.xdat.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Entity;

/**
 * Created by mike on 3/21/18.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class CustomVariableMapping extends AbstractHibernateEntity {
    private static final long serialVersionUID = 1L;

    public CustomVariableMapping() {
    }

    public CustomVariableMapping(String _projectId, String _setName) {
        this._projectId = _projectId;
        this._setName = _setName;
    }

    public String getProjectId() {
        return _projectId;
    }

    public void setProjectId(String _projectId) {
        this._projectId = _projectId;
    }

    public String getSetName() {
        return _setName;
    }

    public void setSetName(String _setName) {
        this._setName = _setName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CustomVariableMapping that = (CustomVariableMapping) o;

        if (_projectId != null ? !_projectId.equals(that._projectId) : that._projectId != null) return false;
        return _setName != null ? _setName.equals(that._setName) : that._setName == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_projectId != null ? _projectId.hashCode() : 0);
        result = 31 * result + (_setName != null ? _setName.hashCode() : 0);
        return result;
    }

    private String                       _projectId;
    private String                       _setName;
}
