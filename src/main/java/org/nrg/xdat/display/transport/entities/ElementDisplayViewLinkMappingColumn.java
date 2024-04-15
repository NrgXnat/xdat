package org.nrg.xdat.display.transport.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;


/**
 * @author Tim Olsen
 *
 * ViewLink Mapping columns are used to configure how a schema defined type joins to a view.
 */
@Entity
@Table(uniqueConstraints =
    @UniqueConstraint(columnNames = { "view_link_id", "mapsTo" }))
public class ElementDisplayViewLinkMappingColumn {
    private long id;

    private String rootElement = "";
    private String fieldElementXMLPath = "";

    private String mapsTo = "";

    private ElementDisplayViewLink viewLink;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public String getRootElement() {
        return rootElement;
    }

    public void setRootElement(final String rootElement) {
        this.rootElement = rootElement;
    }

    public String getFieldElementXMLPath() {
        return fieldElementXMLPath;
    }

    public void setFieldElementXMLPath(final String fieldElementXMLPath) {
        this.fieldElementXMLPath = fieldElementXMLPath;
    }

    public String getMapsTo() {
        return mapsTo;
    }

    public void setMapsTo(final String mapsTo) {
        this.mapsTo = mapsTo;
    }

    @ManyToOne
    @JoinColumn(name = "view_link_id")
    @JsonIgnore
    public ElementDisplayViewLink getViewLink() {
        return viewLink;
    }

    public void setViewLink(final ElementDisplayViewLink viewLink) {
        this.viewLink = viewLink;
    }
}


