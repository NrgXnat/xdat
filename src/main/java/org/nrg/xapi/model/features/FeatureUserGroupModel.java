package org.nrg.xapi.model.features;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collection;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureUserGroupModel implements Serializable {
    private static final long serialVersionUID = -9066669529617137398L;

    private String             id;
    private Object             display;
    private boolean            isRole;
    private Collection<String> features;
    private Collection<String> blocked;
    private Collection<String> inheritedFeatures;
    private Collection<String> inheritedBanned;
}
