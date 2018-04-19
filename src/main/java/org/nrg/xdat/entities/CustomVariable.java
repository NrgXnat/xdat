package org.nrg.xdat.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
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

    public CustomVariable(String _name, String _type, String _required, List<String> _possibleValues) {
        this.name = _name;
        this.type = _type;
        this.required = _required;
        this.possibleValues = _possibleValues;
    }

    public String getName() {
        return name;
    }

    public void setName(String _name) {
        this.name = _name;
    }

    public String getType() {
        return type;
    }

    public void setType(String _type) {
        this.type = _type;
    }

    public String getRequired() {
        return required;
    }

    public void setRequired(String _required) {
        this.required = _required;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    @CollectionTable(name="PossibleValues", joinColumns=@JoinColumn(name="custom_variable_id"))
    @Column(name="possible_values")
    public List<String> getPossibleValues() {
        return possibleValues;
    }

    public void setPossibleValues(List<String> _possibleValues) {
        this.possibleValues = _possibleValues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CustomVariable that = (CustomVariable) o;

        if (this.getId()!=that.getId()) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (required != null ? !required.equals(that.required) : that.required != null) return false;
        return possibleValues != null ? possibleValues.equals(that.possibleValues) : that.possibleValues == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (Long.valueOf(this.getId()).hashCode());
//        result = 31 * result + (name != null ? name.hashCode() : 0);
//        result = 31 * result + (type != null ? type.hashCode() : 0);
//        result = 31 * result + (required != null ? required.hashCode() : 0);
//        result = 31 * result + (possibleValues != null ? possibleValues.hashCode() : 0);
        return result;
    }

    private String                       name;
    private String                       type;
    private String required;
    private List<String> possibleValues;

}
