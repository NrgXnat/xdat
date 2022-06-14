package org.nrg.xdat.display.transport.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Hashtable;
import java.util.Map;

/**
 * @author Tim Olsen
 *
 * HTML Link Property used to configure html links in display fields
 */
@Entity
@Table(uniqueConstraints =
    @UniqueConstraint(columnNames = { "display_field_id", "name" }))
public class DisplayFieldHtmlLinkProperty {
    private long id;
    private String name = "";
    private String value = "";
    private DisplayField displayField=null;

    private Map<String,String> insertedValues= new Hashtable<>();


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @ManyToOne
    @JoinColumn(name = "display_field_id")
    @JsonIgnore
    public DisplayField getDisplayField() {
        return displayField;
    }

    public void setDisplayField(final DisplayField displayField) {
        this.displayField = displayField;
    }

    @ElementCollection
    @MapKeyColumn(name = "insert_key")
    @Column(name = "insert_value")
    public Map<String, String> getInsertedValues() {
        return insertedValues;
    }

    public void setInsertedValues(final Map<String, String> insertedValues) {
        this.insertedValues = insertedValues;
    }
}
