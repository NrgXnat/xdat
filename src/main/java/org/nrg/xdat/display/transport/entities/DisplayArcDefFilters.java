package org.nrg.xdat.display.transport.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

/**
 * @author Tim Olsen
 *
 * Filters for application to ArcDefinition objects.
 */
@Entity
@Table(uniqueConstraints =
    @UniqueConstraint(columnNames = { "arc_def_id", "filterType","filterKey" }))
public class DisplayArcDefFilters {
    private long id;
    private String filterType;
    private String filterKey;

    private DisplayArcDefinition arcDef;

    /**
     * @return
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(final long id) {
        this.id = id;
    }

    /**
     * @return
     */
    public String getFilterType() {
        return filterType;
    }

    /**
     * @param filterType
     */
    public void setFilterType(final String filterType) {
        this.filterType = filterType;
    }

    /**
     * @return
     */
    public String getFilterKey() {
        return filterKey;
    }

    /**
     * @param filterKey
     */
    public void setFilterKey(final String filterKey) {
        this.filterKey = filterKey;
    }

    /**
     * @return
     */
    @ManyToOne
    @JoinColumn(name = "arc_def_id")
    @JsonIgnore
    public DisplayArcDefinition getArcDef() {
        return arcDef;
    }

    /**
     * @param arcDef
     */
    public void setArcDef(final DisplayArcDefinition arcDef) {
        this.arcDef = arcDef;
    }
}
