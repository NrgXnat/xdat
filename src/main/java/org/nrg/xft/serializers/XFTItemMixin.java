package org.nrg.xft.serializers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.nrg.framework.annotations.XnatMixIn;
import org.nrg.xdat.security.SecurityValues;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.collections.ItemCollection;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.w3c.dom.Document;

import java.util.*;

@JsonSerialize(using = ItemISerializer.class)
@XnatMixIn(value = XFTItemMixin.class, targets = XFTItem.class)
@JsonIgnoreProperties(ignoreUnknown = true, allowGetters = true)
@SuppressWarnings({"rawtypes", "unused"})
public abstract class XFTItemMixin {
    // First all the properties with any kind of special handling OTHER than being ignored.
    @JsonProperty("primaryKey")
    public abstract String getPKString();

    @JsonProperty("id")
    public abstract String getIDValue();

    @JsonSerialize(using = UntypedMapPropertySerializer.class)
    public abstract Hashtable getProps();

    @JsonSerialize(using = ItemISerializer.class)
    public abstract ItemI getParent();

    @JsonSerialize(using = UserISerializer.class)
    public abstract UserI getActivationUser();

    @JsonSerialize(using = UserISerializer.class)
    public abstract UserI getInsertUser();

    @JsonSerialize(using = UserISerializer.class)
    public abstract UserI getUser();

    // Now all the properties to be ignored.
    @JsonIgnore
    public abstract ArrayList getHistoryItems();

    @JsonIgnore
    public abstract ArrayList getPkNames();

    @JsonIgnore
    public abstract ArrayList getPossibleFieldNames();

    @JsonIgnore
    public abstract ArrayList getRefNames();

    @JsonIgnore
    public abstract ArrayList<XFTItem> getChildItems();

    @JsonIgnore
    public abstract CriteriaCollection getUniqueSearch();

    @JsonIgnore
    public abstract GenericWrapperElement getGenericSchemaElement();

    @JsonIgnore
    public abstract Hashtable getFieldIds();

    @JsonIgnore
    public abstract Hashtable getIDREFs();

    @JsonIgnore
    public abstract Hashtable getXmlFieldNames();

    @JsonIgnore
    public abstract Integer getMetaDataId();

    @JsonIgnore
    public abstract ItemCollection getHistory();

    @JsonIgnore
    public abstract ItemI getMeta();

    @JsonIgnore
    public abstract Map getPkValues();

    @JsonIgnore
    public abstract Map getPkValuesWTypes();

    @JsonIgnore
    public abstract Object getPK();

    @JsonIgnore
    public abstract SecurityValues getSecurityValues();

    @JsonIgnore
    public abstract String getDBName();

    @JsonIgnore
    public abstract String getPKValueString();

    @JsonIgnore
    public abstract XFTItem getCurrentDBVersion();

    @JsonIgnore
    public abstract XFTItem getExtensionItem();

    @JsonIgnore
    public abstract XFTItem getFirstHistory();

    @JsonIgnore
    public abstract XFTItem getItem();

    @JsonIgnore
    public abstract int getPropertyCount();

    // Lastly all the properties we allow to be handled by default. These can be removed as long as they work properly.
    public abstract boolean isActive();

    public abstract boolean isHistory();

    public abstract boolean isLoading();

    public abstract boolean isLocked();

    public abstract boolean isModified();

    public abstract boolean isPauseDBAccess();

    public abstract boolean isPreLoaded();

    public abstract boolean isQuarantine();

    public abstract boolean isShareable();

    public abstract boolean isValid();

    public abstract boolean isVerifyXMLPaths();

    public abstract Long getXFTVersion();

    public abstract String getXSIType();

    public abstract String getExtensionElement();

    public abstract String getFileName();

    public abstract String getProperName();

    public abstract String getStatus();

    public abstract String getUniqueFileName();

    public abstract Date getActivationDate();

    public abstract Date getInsertDate();

    public abstract Date getLastModified();

    public abstract Date getRowLastModified();

    public abstract List<String> getAllXMLFieldNames();

    public abstract List<String> getSingleXMLFieldNames();

    // TODO: I think these will all be ignored by default, but they're just in case for now...
    public abstract ValidationResults validate();

    public abstract Document toJoinedXML();

    public abstract Document toXML();

    public abstract Document toXML_Item();

    public abstract boolean canBeRootWithBase();

    public abstract boolean hasExtendedField();

    public abstract boolean hasHistory();

    public abstract boolean hasIDField();

    public abstract boolean hasPK();

    public abstract boolean hasProperties();

    public abstract boolean hasUniques();

    public abstract boolean needsActivation();

    public abstract Object clone();

    public abstract Object cloneFormat();

    public abstract XFTItem copy();

    public abstract ItemI populateRefItems();

    public abstract String output();

    public abstract String toHTML();

    public abstract String toString();

    public abstract String toXML_HTML();

    public abstract String toXML_String();
}
