package org.nrg.xapi.model.features;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureDefinitionUserGroupModel implements Serializable {
    private static final long serialVersionUID = 4885945211510671990L;

    private String                      id;
    private Collection<String>          banned;
    private Collection<String>          onByDefault;
    private List<FeatureUserGroupModel> groups;
}
