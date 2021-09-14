package org.nrg.xapi.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureDefinitionUserGroupDto implements Serializable{
	private static final long serialVersionUID = 4885945211510671990L;
	private String id;
	private Collection<String> banned;
	private Collection<String> onByDefault;
	private List<FeatureUserGroupDto>groups;
}
