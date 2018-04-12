package org.nrg.xdat.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.*;
import java.util.List;

/**
 * Created by mike on 3/21/18.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class CustomVariable extends AbstractHibernateEntity {
    private static final long serialVersionUID = 1L;

    public CustomVariable() {
    }

    public CustomVariable(String _name, String _type, String _required, List<String> _possibleValues, CustomVariableSet _parentSet) {
        this._name = _name;
        this._type = _type;
        this._required = _required;
        this._possibleValues = _possibleValues;
        this._parentSet = _parentSet;
    }

    public String getName() {
        return _name;
    }

    public void setName(String _name) {
        this._name = _name;
    }

    public String getType() {
        return _type;
    }

    public void setType(String _type) {
        this._type = _type;
    }

    public String getRequired() {
        return _required;
    }

    public void setRequired(String _required) {
        this._required = _required;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable
    public List<String> getPossibleValues() {
        return _possibleValues;
    }

    public void setPossibleValues(List<String> _possibleValues) {
        this._possibleValues = _possibleValues;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_variable_set_id")
    public CustomVariableSet getParentSet() {
        return _parentSet;
    }

    public void setParentSet(CustomVariableSet parentSet) {
        this._parentSet = parentSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CustomVariable that = (CustomVariable) o;

        if (_name != null ? !_name.equals(that._name) : that._name != null) return false;
        if (_type != null ? !_type.equals(that._type) : that._type != null) return false;
        if (_required != null ? !_required.equals(that._required) : that._required != null) return false;
        return _possibleValues != null ? _possibleValues.equals(that._possibleValues) : that._possibleValues == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_name != null ? _name.hashCode() : 0);
        result = 31 * result + (_type != null ? _type.hashCode() : 0);
        result = 31 * result + (_required != null ? _required.hashCode() : 0);
        result = 31 * result + (_possibleValues != null ? _possibleValues.hashCode() : 0);
        return result;
    }

    private String                       _name;
    private String                       _type;
    private String                       _required;
    private List<String>                 _possibleValues;
    private CustomVariableSet            _parentSet;
}
