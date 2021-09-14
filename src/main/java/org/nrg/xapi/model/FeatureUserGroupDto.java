package org.nrg.xapi.model;

import java.io.Serializable;
import java.util.Collection;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeatureUserGroupDto implements Serializable{
	private static final long serialVersionUID = -9066669529617137398L;
	private String id;
	private Object display;
	private boolean isRole;
	private Collection<String> features;
	private Collection<String> blocked;
	private Collection<String> inherited_features;
	private Collection<String> inherited_banned;
}
