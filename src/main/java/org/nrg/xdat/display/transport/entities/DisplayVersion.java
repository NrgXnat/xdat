package org.nrg.xdat.display.transport.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author Tim Olsen
 *
 * Display Versions are configurable lists of columns to be displayed via XNAT's search engines.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "element_display_id", "versionName" }))
public class DisplayVersion extends AbstractHibernateEntity {
    private String versionName = "";
    private String defaultOrderBy = "";
    private String defaultSortOrder = "";
    private String briefDescription = "";
    private String darkColor = "";
    private String lightColor = "";
    private boolean allowDiffs = true;

    private Integer htmlCellWidth = null;
    private Integer htmlCellHeight = null;
    private String htmlCellValign = null;
    private String htmlCellAlign = null;
    private String htmlCellServerLink = null;

    public ElementDisplay elementDisplay;

    private List<DisplayFieldRef> fields=null;


    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(final String versionName) {
        this.versionName = versionName;
    }

    public String getDefaultOrderBy() {
        return defaultOrderBy;
    }

    public void setDefaultOrderBy(final String defaultOrderBy) {
        this.defaultOrderBy = defaultOrderBy;
    }

    public String getDefaultSortOrder() {
        return defaultSortOrder;
    }

    public void setDefaultSortOrder(final String defaultSortOrder) {
        this.defaultSortOrder = defaultSortOrder;
    }

    public String getBriefDescription() {
        return briefDescription;
    }

    public void setBriefDescription(final String briefDescription) {
        this.briefDescription = briefDescription;
    }

    public String getDarkColor() {
        return darkColor;
    }

    public void setDarkColor(final String darkColor) {
        this.darkColor = darkColor;
    }

    public String getLightColor() {
        return lightColor;
    }

    public void setLightColor(final String lightColor) {
        this.lightColor = lightColor;
    }

    public boolean isAllowDiffs() {
        return allowDiffs;
    }

    public void setAllowDiffs(final boolean allowDiffs) {
        this.allowDiffs = allowDiffs;
    }

    public Integer getHtmlCellWidth() {
        return htmlCellWidth;
    }

    public void setHtmlCellWidth(final Integer htmlCellWidth) {
        this.htmlCellWidth = htmlCellWidth;
    }

    public Integer getHtmlCellHeight() {
        return htmlCellHeight;
    }

    public void setHtmlCellHeight(final Integer htmlCellHeight) {
        this.htmlCellHeight = htmlCellHeight;
    }

    public String getHtmlCellValign() {
        return htmlCellValign;
    }

    public void setHtmlCellValign(final String htmlCellValign) {
        this.htmlCellValign = htmlCellValign;
    }

    public String getHtmlCellAlign() {
        return htmlCellAlign;
    }

    public void setHtmlCellAlign(final String htmlCellAlign) {
        this.htmlCellAlign = htmlCellAlign;
    }

    public String getHtmlCellServerLink() {
        return htmlCellServerLink;
    }

    public void setHtmlCellServerLink(final String htmlCellServerLink) {
        this.htmlCellServerLink = htmlCellServerLink;
    }

    @OneToMany(mappedBy="displayVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<DisplayFieldRef> getFields() {
        if(fields==null){
            fields=Lists.newArrayList();
        }

        fields.sort(new Comparator<DisplayFieldRef>() {
            @Override
            public int compare(final DisplayFieldRef o1, final DisplayFieldRef o2) {
                final int v1 = (o1.getSortOrder()==null)?0:o1.getSortOrder();
                final int v2 = (o2.getSortOrder()==null)?0:o2.getSortOrder();
                return NumberUtils.compare(v1,v2);
            }
        });

        return fields;
    }

    @Transient
    public DisplayFieldRef getFieldById(final String id, final String element, final String v){
        for(final DisplayFieldRef df: getFields()){
            if(v==null) {
                if (StringUtils.equals(id, df.getFieldId()) && (StringUtils.equals(element, df.getElementName()))) {
                    return df;
                }
            }else{
                if (StringUtils.equals(id, df.getFieldId()) && (StringUtils.equals(element, df.getElementName())) && StringUtils.equals(df.getValue(),v)) {
                    return df;
                }
            }
        }

        return null;
    }

    public void setFields(final List<DisplayFieldRef> fields) {
        this.fields = fields;
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


    /**
     * Copy's the key fields from another DisplayVersion into this object.
     * @param dv
     */
    @Transient
    public void copyFrom(final DisplayVersion dv){
        this.setVersionName(dv.getVersionName());
        this.setDarkColor(dv.getDarkColor());
        this.setLightColor(dv.getLightColor());
        this.setBriefDescription(dv.getBriefDescription());
        this.setHtmlCellAlign(dv.getHtmlCellAlign());
        this.setHtmlCellHeight(dv.getHtmlCellHeight());
        this.setHtmlCellValign(dv.getHtmlCellValign());
        this.setHtmlCellWidth(dv.getHtmlCellWidth());
        this.setHtmlCellServerLink(dv.getHtmlCellServerLink());
        this.setAllowDiffs(dv.isAllowDiffs());
        this.setDefaultOrderBy(dv.getDefaultOrderBy());
        this.setDefaultSortOrder(dv.getDefaultSortOrder());

        final List<DisplayFieldRef> stillThere= Lists.newArrayList();
        int i=0;
        for(final DisplayFieldRef dfr: dv.getFields()){
            DisplayFieldRef storedDFR = this.getFieldById(dfr.getFieldId(),dfr.getElementName(),dfr.getValue());
            if(storedDFR!=null){
                storedDFR.setSortOrder(dfr.getSortOrder());
                storedDFR.setHeader(dfr.getHeader());
            }else{
                storedDFR = new DisplayFieldRef();
                storedDFR.setElementName(dfr.getElementName());
                storedDFR.setHeader(dfr.getHeader());
                storedDFR.setFieldId(dfr.getFieldId());
                storedDFR.setVisible(dfr.getVisible());
                storedDFR.setSortOrder(dfr.getSortOrder());
                storedDFR.setValue(dfr.getValue());
                this.addField(storedDFR);
            }
            stillThere.add(storedDFR);
        }

        //remove absent fields
        final List<DisplayFieldRef> toRemove = Lists.newArrayList();
        for(final DisplayFieldRef dfr: this.getFields()){
            if(!stillThere.contains(dfr)){
                toRemove.add(dfr);
            }
        }
        for(final DisplayFieldRef dfr : toRemove){
            this.getFields().remove(dfr);
        }
    }

    private void addField(final DisplayFieldRef storedDFR) {
        storedDFR.setDisplayVersion(this);

        this.getFields().add(storedDFR);
    }
}
