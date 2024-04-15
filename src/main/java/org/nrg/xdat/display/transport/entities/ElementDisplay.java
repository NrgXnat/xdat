package org.nrg.xdat.display.transport.entities;

import org.apache.commons.lang.StringUtils;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;
import org.python.apache.commons.compress.utils.Lists;

import javax.persistence.*;
import java.util.List;

/**
 * @author Tim Olsen
 *
 * Element Displays, modeled after the display.xml files, enable display configurations to be stored in the database.
 *
 * Element displays which correspond to xml files will be overlayed on top of the xml version during server startup.
 */
@Entity
public class ElementDisplay extends AbstractHibernateEntity  {
    private String elementName;
    private String valueField;
    private String displayField;
    private String displayLabel;
    private String briefDescription;
    private String fullDescription;
    private String lightColor;
    private String darkColor;
    private Integer sortOrder;

    private List<DisplayField> displayFields;
    private List<ElementDisplayArc> arcs;
    private List<DisplayVersion> displayVersions;
    private List<ElementDisplayViewLink> viewLinks;

    @Column(unique = true, nullable = false)
    public String getElementName() {
        return elementName;
    }

    public void setElementName(final String elementName) {
        this.elementName = elementName;
    }

    public String getValueField() {
        return valueField;
    }

    public void setValueField(final String valueField) {
        this.valueField = valueField;
    }

    public String getDisplayField() {
        return displayField;
    }

    public void setDisplayField(final String displayField) {
        this.displayField = displayField;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public void setDisplayLabel(final String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getBriefDescription() {
        return briefDescription;
    }

    public void setBriefDescription(final String briefDescription) {
        this.briefDescription = briefDescription;
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public void setFullDescription(final String fullDescription) {
        this.fullDescription = fullDescription;
    }

    public String getLightColor() {
        return lightColor;
    }

    public void setLightColor(final String lightColor) {
        this.lightColor = lightColor;
    }

    public String getDarkColor() {
        return darkColor;
    }

    public void setDarkColor(final String darkColor) {
        this.darkColor = darkColor;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(final Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    @OneToMany(mappedBy="elementDisplay", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<DisplayField> getDisplayFields() {
        if(displayFields==null){
            displayFields= Lists.newArrayList();
        }
        return displayFields;
    }

    public void setDisplayFields(final List<DisplayField> displayFields) {
        this.displayFields = displayFields;
    }

    @OneToMany(mappedBy="elementDisplay", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ElementDisplayArc> getArcs() {
        if(arcs==null){
            arcs=Lists.newArrayList();
        }

        return arcs;
    }

    public void setArcs(final List<ElementDisplayArc> arcs) {
        this.arcs = arcs;
    }

    public void addArc(final ElementDisplayArc arc) {
        arc.setElementDisplay(this);
        this.getArcs().add(arc);
    }

    public ElementDisplayArc getArcByName(final String name){
        for(final ElementDisplayArc arc : getArcs()){
            if(StringUtils.equals(arc.getName(),name)){
                return arc;
            }
        }

        return null;
    }

    @OneToMany(mappedBy="elementDisplay", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<DisplayVersion> getDisplayVersions() {
        if(displayVersions==null){
            displayVersions=Lists.newArrayList();
        }
        return displayVersions;
    }

    public void setDisplayVersions(final List<DisplayVersion> displayVersions) {
        this.displayVersions = displayVersions;
    }

    @OneToMany(mappedBy="elementDisplay", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ElementDisplayViewLink> getViewLinks() {
        if(viewLinks==null){
            viewLinks=Lists.newArrayList();
        }
        return viewLinks;
    }

    @Transient
    public ElementDisplayViewLink getViewLinkByAlias(final String alias){
        for(final ElementDisplayViewLink vl: getViewLinks()){
            if(StringUtils.equals(vl.getAlias(),alias)){
                return vl;
            }
        }

        return null;
    }

    public void setViewLinks(final List<ElementDisplayViewLink> viewLinks) {
        this.viewLinks = viewLinks;
    }

    public void addViewLink(final ElementDisplayViewLink viewLink){
        viewLink.setElementDisplay(this);
        this.viewLinks.add(viewLink);
    }

    @Transient
    public void addDisplayField(final DisplayField field){
        if(displayFields==null){
            displayFields=Lists.newArrayList();
        }

        field.setElementDisplay(this);
        displayFields.add(field);
    }

    @Transient
    public DisplayField getDisplayField(final String fieldId){
        for(final DisplayField df : this.getDisplayFields()){
            if (StringUtils.equals(df.getFieldId(), fieldId)) {
                return df;
            }
        }

        return null;
    }

    @Transient
    public void addDisplayVersion(final DisplayVersion version){
        if(displayVersions==null){
            displayVersions=Lists.newArrayList();
        }

        version.setElementDisplay(this);
        displayVersions.add(version);
    }

    @Transient
    public DisplayVersion getDisplayVersion(final String versionId){
        for(final DisplayVersion dv : this.getDisplayVersions()){
            if (StringUtils.equals(dv.getVersionName(), versionId)) {
                return dv;
            }
        }

        return null;
    }
}
