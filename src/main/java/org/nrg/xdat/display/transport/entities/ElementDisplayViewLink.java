package org.nrg.xdat.display.transport.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;
import org.python.apache.commons.compress.utils.Lists;

import javax.persistence.*;
import java.util.List;


/**
 * @author Tim Olsen
 *
 * ViewLinks are configured within the scope of an ElementDisplay to configure how a view can be joined to data stored in a schema.
 */
@Entity
@Table(uniqueConstraints =
    @UniqueConstraint(columnNames = { "element_display_id", "alias" }))
public class ElementDisplayViewLink extends AbstractHibernateEntity {
    private String view;

    private String alias;

    private List<ElementDisplayViewLinkMappingColumn> columns;

    private ElementDisplay elementDisplay;

    public String getView() {
        return view;
    }

    public void setView(final String view){
        this.view=view;
    }

    @JoinColumn(nullable=false)
    public String getAlias() {
        return alias;
    }

    public void setAlias(final String alias) {
        this.alias = alias;
    }

    @OneToMany(mappedBy="viewLink", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ElementDisplayViewLinkMappingColumn> getColumns() {
        return columns;
    }

    public void addColumn(final ElementDisplayViewLinkMappingColumn col){
        if(this.columns==null){
            this.columns= Lists.newArrayList();
        }

        this.columns.add(col);
        col.setViewLink(this);
    }

    public void setColumns(final List<ElementDisplayViewLinkMappingColumn> columns) {
        this.columns = columns;
    }

    @ManyToOne
    @JoinColumn(name = "element_display_id")
    @JsonIgnore
    public ElementDisplay getElementDisplay() {
        return elementDisplay;
    }

    public void setElementDisplay(final ElementDisplay elementDisplay) {
        this.elementDisplay = elementDisplay;
    }
}
