/*
 * core: org.nrg.xdat.services.impl.hibernate.HibernateFeatureDefinitionService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xdat.services.impl.hibernate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xapi.model.FeatureDefinitionUserGroupDto;
import org.nrg.xapi.model.FeatureUserGroupDto;
import org.nrg.xdat.daos.FeatureDefinitionDAO;
import org.nrg.xdat.entities.FeatureDefinition;
import org.nrg.xdat.security.helpers.FeatureDefinitionI;
import org.nrg.xdat.security.helpers.Features;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.security.services.RoleRepositoryServiceI.RoleDefinitionI;
import org.nrg.xdat.services.FeatureDefinitionService;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.security.UserI;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HibernateFeatureDefinitionService extends AbstractHibernateEntityService<FeatureDefinition, FeatureDefinitionDAO> implements FeatureDefinitionService {
	@Override
	@Transactional
	public FeatureDefinition findFeatureByKey(String key) {
		return this.getDao().findByKey(key);
	}

	@Override
	public List<FeatureDefinition> findAll() {
		List<FeatureDefinition>featureDefinitions = new ArrayList<>();
		for(FeatureDefinitionI feature: Features.getAllFeatures()){
			FeatureDefinition featureDefinition = new FeatureDefinition();
			featureDefinition.setName(feature.getName());
			featureDefinition.setKey(feature.getKey());
			featureDefinition.setOnByDefault(feature.isOnByDefault());
			featureDefinition.setBanned(feature.isBanned());
			featureDefinitions.add(featureDefinition);
		}
		return featureDefinitions;
	}

	@Override
	public void findAllByTags(UserI user, String[] tags) {
//		Collection<String> siteWideEnabled=Features.getEnabledFeatures();
//    	Collection<String> siteWideBanned=Features.getBannedFeatures();
//    	List<FeatureDefinitionUserGroupDto> groups = new ArrayList<>();
//    	for(String tag:tags){
//    		XnatProjectdata proj=XnatProjectdata.getProjectByIDorAlias(tag, user, false);
//    		List<FeatureUserGroupDto> group = new ArrayList<>();
//    		for(List gID:proj.getGroupIDs()){
//    			UserGroupI ug=Groups.getGroup((String)gID.get(0));
//    			group.add(FeatureUserGroupDto.builder()
//    					.id(ug.getId())
//    					.display(gID.get(1))
//    					.features(Features.getFeaturesForGroup(ug))
//    					.blocked(Features.getBlockedFeaturesForGroup(ug))
//    					.inherited_banned(Features.getBannedFeaturesForGroupType((String)gID.get(1)))
//    					.inherited_features(Features.getEnabledFeaturesForGroupType((String)gID.get(1))).build());
//    		}
//    		groups.add(FeatureDefinitionUserGroupDto.builder()
//    				.id(proj.getId())
//    				.banned(siteWideBanned)
//    				.onByDefault(siteWideEnabled)
//    				.groups(group).build());
//    	}
	}

	@Override
	public List<FeatureDefinitionUserGroupDto> findAllByType(UserI user, String type) throws SQLException, DBPoolException {
		Collection<String> siteWideEnabled=Features.getEnabledFeatures();
    	Collection<String> siteWideBanned=Features.getBannedFeatures();
    	
		List<FeatureDefinitionUserGroupDto> groups = new ArrayList<>();
		XFTTable t= XFTTable.Execute(XDAT_USER_GROUP, null, null);
        List<Object> groupTypes=t.convertColumnToArrayList("displayname");
        
        FeatureDefinitionUserGroupDto userGroup = getFeatureUserGroup(groupTypes);
        userGroup.setId(Features.SITE_WIDE);
        userGroup.setBanned(siteWideBanned);
        userGroup.setOnByDefault(siteWideEnabled);
        groups.add(userGroup);
        
        FeatureDefinitionUserGroupDto userGroupWithRole = getFeatureUserGroupWithRole(groupTypes);
        groups.add(userGroupWithRole);
        
        return groups;
	}
	
	private FeatureDefinitionUserGroupDto getFeatureUserGroupWithRole(List<Object> groupTypes) {
		FeatureDefinitionUserGroupDto userGroup = new FeatureDefinitionUserGroupDto();
		 for(RoleDefinitionI role : Roles.getRoles()){
	        	List<FeatureUserGroupDto> group = new ArrayList<>();
	        	String key="role:"+role.getKey();
	        	group.add(FeatureUserGroupDto.builder()
	        			.id(key)
	        			.display("Role: " + role.getName())
	        			.isRole(true)
	        			.features(Features.getEnabledFeaturesForGroupType(key))
	        			.blocked(Features.getBannedFeaturesForGroupType(key)).build());
	        	userGroup.setGroups(group);
	        }
		return userGroup;
	}
	
	private FeatureDefinitionUserGroupDto getFeatureUserGroup(List<Object> groupTypes) {
		 FeatureDefinitionUserGroupDto userGroup = new FeatureDefinitionUserGroupDto();
		for(Object gType:groupTypes){
       	List<FeatureUserGroupDto> group = new ArrayList<>();
       	group.add(FeatureUserGroupDto.builder()
       			.id(gType.toString())
       			.display(gType.toString())
       			.features(Features.getEnabledFeaturesForGroupType((String)gType))
       			.blocked(Features.getBannedFeaturesForGroupType((String)gType)).build());
       	userGroup.setGroups(group);
		}
		return userGroup;
	}
	
	private static final String XDAT_USER_GROUP = "SELECT DISTINCT displayname FROM xdat_usergroup WHERE tag IS NOT NULL";
}
