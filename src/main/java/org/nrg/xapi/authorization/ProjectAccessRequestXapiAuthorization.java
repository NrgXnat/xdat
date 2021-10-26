package org.nrg.xapi.authorization;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.nrg.xdat.security.helpers.AccessLevel;
import org.nrg.xdat.security.helpers.Groups;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Checks whether the user has access to view a project access request. This means the user is either
 */
@Component
@Slf4j
public class ProjectAccessRequestXapiAuthorization extends AbstractXapiAuthorization {
    private static final String PARAM_PAR_ID          = "parId";
    private static final String PARAM_USER_ID         = "userId";
    private static final String QUERY_IS_PAR_FOR_USER = "SELECT EXISTS(SELECT par_id FROM xs_par_table WHERE par_id = :" + PARAM_PAR_ID + " AND user_id = :" + PARAM_USER_ID + ")";

    private final NamedParameterJdbcTemplate _template;

    @Autowired
    public ProjectAccessRequestXapiAuthorization(final NamedParameterJdbcTemplate template) {
        _template = template;
    }

    @Override
    protected boolean considerGuests() {
        return false;
    }

    @Override
    protected boolean checkImpl(final AccessLevel accessLevel, final JoinPoint joinPoint, final UserI user, final HttpServletRequest request) {
        if (Groups.hasAllDataAccess(user)) {
            return true;
        }
        final Method      method     = ((MethodSignature) joinPoint.getSignature()).getMethod();
        final Parameter[] parameters = method.getParameters();

        final Parameter parIdParameter = Arrays.stream(parameters).filter(IS_INTEGER_PARAMETER.and(parameter -> StringUtils.equals(PARAM_PAR_ID, parameter.getName()))).findFirst().orElseGet(() -> {
            final List<Parameter> intParams = Arrays.stream(parameters).filter(IS_INTEGER_PARAMETER).collect(Collectors.toList());
            if (intParams.size() == 1) {
                return intParams.get(0);
            }
            if (intParams.isEmpty()) {
                log.warn("This delegate was assigned to a method {} that doesn't have any integer parameters", method.getName());
            } else {
                log.warn("This delegate was assigned to a method {} that has multiple integer parameters but none of them are named \"parId\"", method.getName());
            }
            return null;
        });
        if (parIdParameter == null) {
            return false;
        }

        final int parId = Integer.class.cast(parIdParameter);
        log.info("Checking for access to PAR {} for user {}", parId, user.getUsername());
        return _template.queryForObject(QUERY_IS_PAR_FOR_USER, new MapSqlParameterSource(PARAM_PAR_ID, parId).addValue(PARAM_USER_ID, user.getID()), Boolean.class);
    }
}
