package org.nrg.xft.serializers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.nrg.framework.annotations.XnatMixIn;
import org.nrg.xft.ItemI;
import org.nrg.xft.ItemWrapper;
import org.nrg.xft.XFTItem;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.security.UserI;

import java.util.ArrayList;
import java.util.Hashtable;

@JsonSerialize(using = ItemISerializer.class)
@JsonIgnoreProperties(ignoreUnknown = true, allowGetters = true)
@XnatMixIn(value = ItemWrapperMixin.class, targets = ItemWrapper.class)
@SuppressWarnings("rawtypes")
public abstract class ItemWrapperMixin {
    @JsonSerialize(using = ItemISerializer.class)
    public abstract ItemI getParent();

    @JsonIgnore
    public abstract XFTItem getItem();

    @JsonIgnore
    public abstract ArrayList getChildItems() throws XFTInitException, ElementNotFoundException, FieldNotFoundException;

    @JsonIgnore
    public abstract XFTItem getCurrentDBVersion();

    @JsonSerialize(using = UntypedMapPropertySerializer.class)
    public abstract Hashtable getProps();

    @JsonSerialize(using = UserISerializer.class)
    public abstract UserI getUser();

    @JsonIgnore
    public abstract String getDBName();

    @JsonSerialize(using = UserISerializer.class)
    public abstract UserI getInsertUser();
}
