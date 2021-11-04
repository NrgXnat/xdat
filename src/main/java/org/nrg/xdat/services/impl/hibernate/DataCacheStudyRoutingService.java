/*
 * core: org.nrg.xdat.services.impl.hibernate.DataCacheStudyRoutingService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xdat.services.impl.hibernate;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.framework.datacache.DataCacheService;
import org.nrg.xapi.exceptions.DataFormatException;
import org.nrg.xapi.exceptions.InitializationException;
import org.nrg.xapi.exceptions.InsufficientPrivilegesException;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xapi.model.dicom.StudyRoutingModel;
import org.nrg.xdat.security.SecurityManager;
import org.nrg.xdat.security.helpers.Permissions;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.security.helpers.UserHelper;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataCacheStudyRoutingService implements StudyRoutingService {
    @Autowired
    public DataCacheStudyRoutingService(final DataCacheService dataCacheService) {
        _dataCacheService = dataCacheService;
    }

    /**
     * Creates a new routing configuration for the indicated attributes.
     *
     * <b>Note:</b> Once a routing configuration has been created for a particular study instance UID, further calls to
     * any of the <b>assign()</b> methods will not change that existing routing configuration. In that case, the return
     * value will be <b>false</b>. To change an existing routing configuration, use the {@link #update(String,
     * java.util.Map)} method.
     *
     * @param studyInstanceUid The study instance UID to be assigned.
     * @param projectId        The ID of the destination project.
     * @param userId           The login name for the user who created the routing configurations.
     *
     * @return This method returns <b>true</b> when the configuration was successfully created, <b>false</b> otherwise.
     * The <b>false</b> return value usually indicates that a routing configuration for the submitted study
     * instance UID already exists.
     */
    @Override
    public boolean assign(final String studyInstanceUid, final String projectId, final String userId) {
        return assign(studyInstanceUid, projectId, null, null, userId);
    }

    /**
     * Creates a new routing configuration for the indicated attributes.
     *
     * <b>Note:</b> Once a routing configuration has been created for a particular study instance UID, further calls to
     * any of the <b>assign()</b> methods will not change that existing routing configuration. In that case, the return
     * value will be <b>false</b>. To change an existing routing configuration, use the {@link #update(String,
     * java.util.Map)} method.
     *
     * @param studyInstanceUid The study instance UID to be assigned.
     * @param projectId        The ID of the destination project.
     * @param subjectId        The ID of the destination subject.
     * @param userId           The login name for the user who created the routing configurations.
     *
     * @return This method returns <b>true</b> when the configuration was successfully created, <b>false</b> otherwise.
     * The <b>false</b> return value usually indicates that a routing configuration for the submitted study
     * instance UID already exists.
     */
    @Override
    public boolean assign(final String studyInstanceUid, final String projectId, final String subjectId, final String userId) {
        return assign(studyInstanceUid, projectId, subjectId, null, userId);
    }

    /**
     * Creates a new routing configuration for the indicated attributes.
     *
     * <b>Note:</b> Once a routing configuration has been created for a particular study instance UID, further calls to
     * any of the <b>assign()</b> methods will not change that existing routing configuration. In that case, the return
     * value will be <b>false</b>. To change an existing routing configuration, use the {@link #update(String,
     * java.util.Map)} method.
     *
     * @param studyInstanceUid The study instance UID to be assigned.
     * @param projectId        The ID of the destination project.
     * @param subjectId        The ID of the destination subject.
     * @param label            The label for the session.
     * @param userId           The login name for the user who created the routing configurations.
     *
     * @return This method returns <b>true</b> when the configuration was successfully created, <b>false</b> otherwise.
     * The <b>false</b> return value usually indicates that a routing configuration for the submitted study
     * instance UID already exists.
     */
    @Override
    public boolean assign(final String studyInstanceUid, final String projectId, final String subjectId, final String label, final String userId) {
        if (getConfigurations().containsKey(studyInstanceUid)) {
            log.warn("Attempt to assign new routing configuration for study instance UID: {} (configuration already exists!)", studyInstanceUid);
            return false;
        }
        if (StringUtils.isBlank(studyInstanceUid)) {
            throw new RuntimeException("You must specify a study instance UID to create a new routing configuration.");
        }
        if (StringUtils.isBlank(userId)) {
            throw new RuntimeException("You must specify a user ID to create a new routing configuration.");
        }
        if (StringUtils.isBlank(projectId) && StringUtils.isBlank(subjectId) && StringUtils.isBlank(label)) {
            throw new RuntimeException("You must specify at least one of a project ID, subject ID, or label to create a new routing configuration.");
        }

        final HashMap<String, String> configuration = new HashMap<>();
        final String                  now           = FORMATTER.format(new Date());

        configuration.put(USER, userId);
        configuration.put(CREATED, now);
        configuration.put(ACCESSED, now);

        if (!StringUtils.isBlank(projectId)) {
            configuration.put(PROJECT, projectId);
        }
        if (!StringUtils.isBlank(subjectId)) {
            configuration.put(SUBJECT, subjectId);
        }
        if (!StringUtils.isBlank(label)) {
            configuration.put(LABEL, label);
        }

        log.debug("Updated routing configuration for study instance UID: {}\n{}", studyInstanceUid, configuration);

        return commit(studyInstanceUid, configuration);
    }

    /**
     * Updates the configuration for the indicated study instance UID. Only the attributes specified in the submitted
     * configuration will be updated; all other existing attributes will remain the same (i.e. they will not be cleared
     * or deleted). To clear an existing attribute, submit the attribute key with a null value.
     *
     * <b>Note:</b> This method will not create a new routing configuration if one has not already been created for the
     * submitted study instance UID. In that case, the return value will be <b>false</b>. To create a new routing
     * configuration, use the {@link #assign(String, String, String)}, {@link #assign(String, String, String, String)},
     * or {@link #assign(String, String, String, String, String)} method.
     *
     * @param studyInstanceUid The study instance UID of the configuration to be updated.
     * @param configuration    The attribute keys and values to be updated.
     *
     * @return This method returns <b>true</b> when the configuration was successfully updated, <b>false</b> otherwise.
     * The <b>false</b> return value usually indicates that a routing configuration for the submitted study
     * instance UID doesn't exist to be updated.
     */
    @Override
    public boolean update(final String studyInstanceUid, final Map<String, String> configuration) {
        if (!getConfigurations().containsKey(studyInstanceUid)) {
            log.warn("Attempt to update non-existent routing configuration for study instance UID: {}", studyInstanceUid);
            return false;
        }

        HashMap<String, String> existing = getConfigurations().get(studyInstanceUid);

        for (final String key : configuration.keySet()) {
            final String value = configuration.get(key);
            if (value == null && existing.containsKey(key)) {
                log.debug("Removing key {} from configuration for study instance UID: {}", key, studyInstanceUid);
                existing.remove(key);
            } else {
                log.debug(existing.containsKey(key) ? "Updating existing key {} in configuration for study instance UID: {}" : "Creating new key {} in configuration for study instance UID: {}", key, studyInstanceUid);
                existing.put(key, value);
            }
        }

        configuration.put(ACCESSED, FORMATTER.format(new Date()));

        return commit(studyInstanceUid, existing);
    }

    /**
     * Finds the active routing configuration for the indicated study instance UID. If there is no active routing
     * configuration for that study instance UID, this method returns <b>null</b>.
     *
     * @param studyInstanceUid The study instance UID to query.
     *
     * @return The active routing configuration, if any, for the indicated study instance UID. Returns <b>null</b> if
     * there is no active routing configuration for that study.
     */
    @Override
    public Map<String, String> findStudyRouting(final String studyInstanceUid) {
        return getConfigurations().get(studyInstanceUid);
    }

    /**
     * Finds all active routing configurations for a particular project. This returns as a map of maps. The containing
     * map is keyed on the study instance UIDs for the study configurations. The contained maps include all the attributes
     * for each routing configuration. The project ID configured for all routing configurations wil be the same as the requested
     * project ID parameter.
     *
     * @param projectId The ID of the destination project.
     *
     * @return A list of the routing configurations for the indicated project.
     */
    @Override
    public Map<String, Map<String, String>> findProjectRoutings(final String projectId) {
        return findRoutingsByAttribute(PROJECT, projectId);
    }

    /**
     * Finds all active routing configurations created by a particular user. This returns as a map of maps. The
     * containing map is keyed on the study instance UIDs for the routing configurations. The contained maps include
     * all the attributes for each routing configuration. The user ID configured for all routing configurations will be
     * the same as the requested project ID parameter.
     *
     * @param userId The login name for the user who created the routing configurations.
     *
     * @return A list of the routing configurations for the indicated user.
     */
    @Override
    public Map<String, Map<String, String>> findUserRoutings(final String userId) {
        return findRoutingsByAttribute(USER, userId);
    }

    /**
     * Finds all active routing configurations for a particular value of a particular attribute. This returns as a map
     * of maps. The containing map is keyed on the study instance UIDs for the study configurations. The contained maps
     * include all the attributes for each routing configuration. The value for the requested attribute configured for all
     * routing configurations wil be the same as the requested attribute value.
     *
     * @param attribute The key for the attribute on which you wish to search.
     * @param value     The value of the attribute on which you wish to search.
     *
     * @return A list of the routing configurations for the indicated attribute key and value.
     */
    @Override
    public Map<String, Map<String, String>> findRoutingsByAttribute(final String attribute, final String value) {
        Map<String, Map<String, String>> found = new HashMap<>();
        for (final Map.Entry<String, HashMap<String, String>> entry : getConfigurations().entrySet()) {
            if (entry.getValue().containsKey(attribute) && entry.getValue().get(attribute).equals(value)) {
                found.put(entry.getKey(), entry.getValue());
            }
        }
        return found;
    }

    /**
     * Finds all active routing configurations.
     *
     * @return A list of the routing configurations currently in the system.
     */
    @Override
    public Map<String, Map<String, String>> findAllRoutings() {
        return new HashMap<>(getConfigurations());
    }

    /**
     * Closes the routing configuration for the indicated study instance UID. This should only be done when the study
     * has been fully received and archived. Any DICOM data for the study received later will not have the routing
     * configuration available anymore, which could lead to split sessions. This method returns <b>true</b> if the
     * specified study instance UID exists in the routing configuration map and is closed successfully, and <b>false</b>
     * otherwise.
     *
     * @param studyInstanceUid The UID of the routing configuration to close.
     *
     * @return <b>true</b> if the specified study instance UID exists in the routing configuration map and is closed
     * successfully, and <b>false</b> otherwise.
     */
    @Override
    public boolean close(final String studyInstanceUid) {
        if (!getConfigurations().containsKey(studyInstanceUid)) {
            return false;
        }
        getConfigurations().remove(studyInstanceUid);

        // The return value just checks for a non-zero object ID for the updated record.
        return _dataCacheService.put(SERVICE_KEY, getConfigurations()) > 0;
    }

    /**
     * Closes all routing configurations on the site. This is a pretty extreme step and can only be performed by site
     * administrators. This returns all existing routings to allow for restoring configurations later.
     *
     * @return The existing routings.
     */
    @Override
    public Map<String, Map<String, String>> closeAll() {
        final Map<String, Map<String, String>> found = new HashMap<>(getConfigurations());
        getConfigurations().clear();
        _dataCacheService.put(SERVICE_KEY, getConfigurations());
        return found;
    }

    @Override
    public List<StudyRoutingModel> findAll(final UserI user) {
        return getAllStudyRouting(user);
    }


    @Override
    public Optional<StudyRoutingModel> findByStudyInstanceUid(UserI user, String studyInstanceUid) throws InsufficientPrivilegesException {
        return Optional.ofNullable(getAllStudyRoutingByInstanceUid(user, studyInstanceUid));
    }

    @Override
    public void updateStudyRouting(UserI user, String studyInstanceUid, String projectId) throws InitializationException, DataFormatException {
        if (StringUtils.isBlank(studyInstanceUid) || StringUtils.isBlank(projectId)) {
            throw new DataFormatException("You must specify both a study instance UID and the project ID that you wish to assign.");
        }
        try {
            if (!Permissions.can(user, "xnat:mrSessionData/project", projectId, SecurityManager.EDIT)) {
                throw new InsufficientPrivilegesException("You do not have sufficient privileges to modify study routings for this project.");
            }
            final Map<String, String> routing = findStudyRouting(studyInstanceUid);
            if (routing == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Creating new study routing assignment for study instance UID {} to project {}, created by user {}", studyInstanceUid, projectId, user.getLogin());
                }
                assign(studyInstanceUid, projectId, user.getLogin());
            } else if (!StringUtils.equals(routing.get(StudyRoutingService.PROJECT), projectId)) {
                final String existing = routing.get(StudyRoutingService.PROJECT);
                if (!Permissions.can(user, "xnat:mrSessionData/project", existing, SecurityManager.EDIT)) {
                    throw new InsufficientPrivilegesException("You are trying to reassign study instance UID " + studyInstanceUid + " from project " + existing + " but do not have sufficient privileges to modify study routings for this project.");
                }
                log.debug("Updating routing assignment for study instance UID {} to project {}, updated by user {}", studyInstanceUid, projectId, user.getLogin());
                routing.put(StudyRoutingService.PROJECT, projectId);
                update(studyInstanceUid, routing);
            }
        } catch (Exception e) {
            log.error("An error occurred checking user permissions", e);
            throw new InitializationException("An error occurred checking user permissions");
        }
    }

    @Override
    public void deleteStudyRouting(UserI user, String studyInstanceUid) throws InsufficientPrivilegesException, InitializationException {
        if (StringUtils.isBlank(studyInstanceUid)) {
            if (!Roles.isSiteAdmin(user)) {
                throw new InsufficientPrivilegesException("You must be a site administrator to delete all study routings for this site.");
            }
            log.warn("Closing all study instance UID assignments, per user {}", user.getLogin());
            closeAll();
        } else {
            try {
                Map<String, String> routing = findStudyRouting(studyInstanceUid);
                if (routing == null) {
                    throw new NotFoundException("Couldn't find a routing for the study instance UID: " + studyInstanceUid);
                }
                if (!Permissions.can(user, "xnat:mrSessionData/project",
                                     routing.get(StudyRoutingService.PROJECT), SecurityManager.EDIT)) {
                    throw new InsufficientPrivilegesException("You do not have sufficient privileges to delete study routings for this project.");
                }
                log.info("Closing study instance UID {} assignment, per user {}", studyInstanceUid, user.getLogin());
                close(studyInstanceUid);
            } catch (Exception e) {
                log.error("An error occurred checking user permissions", e);
                throw new InitializationException("An error occurred checking user permissions");
            }
        }
    }

    private StudyRoutingModel getAllStudyRoutingByInstanceUid(final UserI user, final String studyInstanceUid) throws InsufficientPrivilegesException {
        final Map<String, String> routing = findStudyRouting(studyInstanceUid);
        if (routing == null || routing.size() == 0) {
            log.info("Request made for routing for study instance UID {}, but nothing was found for that value.", studyInstanceUid);
            return null;
        }
        if (!UserHelper.getUserHelperService(user).hasEditAccessToSessionDataByTag(routing.getOrDefault(StudyRoutingService.PROJECT, ""))) {
            throw new InsufficientPrivilegesException("You do not have sufficient privileges to view the routing for this study instance UID.");
        }
        log.debug("Request made for routing for study instance UID {}, found routing: {}", studyInstanceUid, routing);
        return getStudyRouting(studyInstanceUid, routing);
    }

    private List<StudyRoutingModel> getAllStudyRouting(final UserI user) {
        final Map<String, Map<String, String>> routings = findAllRoutings();
        if (routings == null || routings.size() <= 0) {
            log.debug("Request made for all system routings, found no results!");
            return Collections.emptyList();
        }
        log.debug("Request made for all system routings, found {} results", routings.size());
        return routings.entrySet().stream()
                       .filter(entry -> UserHelper.getUserHelperService(user).hasEditAccessToSessionDataByTag(entry.getValue().getOrDefault(StudyRoutingService.PROJECT, "")))
                       .map(entry -> getStudyRouting(entry.getKey(), entry.getValue()))
                       .collect(Collectors.toList());
    }

    private StudyRoutingModel getStudyRouting(String studyInstanceUid, Map<String, String> routing) {
        return StudyRoutingModel.builder()
                                .studyInstanceUid(studyInstanceUid)
                                .project(routing.getOrDefault(StudyRoutingService.PROJECT, ""))
                                .subject(routing.getOrDefault(StudyRoutingService.SUBJECT, ""))
                                .user(routing.getOrDefault(StudyRoutingService.USER, ""))
                                .created(routing.getOrDefault(StudyRoutingService.CREATED, ""))
                                .accessed(routing.getOrDefault(StudyRoutingService.ACCESSED, ""))
                                .label(routing.getOrDefault(StudyRoutingService.LABEL, ""))
                                .build();
    }

    /**
     * Commits the routing configuration for the indicated study instance UID. Returns <b>true</b> if the commit
     * completed successfully, <b>false</b> otherwise.
     *
     * @param studyInstanceUid The study instance UID of the configuration to be created or updated.
     * @param configuration    The configuration itself.
     *
     * @return <b>true</b> if the commit completed successfully, <b>false</b> otherwise.
     */
    private boolean commit(final String studyInstanceUid, final HashMap<String, String> configuration) {
        log.debug(getConfigurations().containsKey(studyInstanceUid) ? "Updated existing" : "Created new" + " routing configuration for study instance UID: " + studyInstanceUid + "\n" + configuration.toString());

        getConfigurations().put(studyInstanceUid, configuration);

        // The return value just checks for a non-zero object ID for the updated record.
        return _dataCacheService.put(SERVICE_KEY, getConfigurations()) > 0;
    }

    private HashMap<String, HashMap<String, String>> getConfigurations() throws RuntimeException {
        if (_configuration == null) {
            _configuration = _dataCacheService.get(SERVICE_KEY);
            if (_configuration == null) {
                _dataCacheService.put(SERVICE_KEY, new HashMap<>());
                _configuration = _dataCacheService.get(SERVICE_KEY);
            }
        }
        return _configuration;
    }

    private static final String           SERVICE_KEY = DataCacheStudyRoutingService.class.getName();
    private static final SimpleDateFormat FORMATTER   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss a");

    private HashMap<String, HashMap<String, String>> _configuration;

    private final DataCacheService _dataCacheService;
}
