package org.nrg.xapi.model.features;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class FeatureDefinitionModel implements Serializable {
    private static final long serialVersionUID = -1357797427762108637L;

    private String  key;
    private String  name;
    private String  description;
    private boolean enabled;
    private boolean banned;
}
