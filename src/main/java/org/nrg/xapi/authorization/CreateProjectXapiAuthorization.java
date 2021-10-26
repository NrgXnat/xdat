package org.nrg.xapi.authorization;

import org.aspectj.lang.JoinPoint;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.AccessLevel;
import org.nrg.xdat.security.helpers.Groups;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * Checks whether the user is a site administrator.
 */
@Component
public class CreateProjectXapiAuthorization extends AbstractXapiAuthorization {
    private final SiteConfigPreferences _preferences;

    @Autowired
    public CreateProjectXapiAuthorization(final SiteConfigPreferences preferences) {
        _preferences = preferences;
    }

    @Override
    protected boolean considerGuests() {
        return false;
    }

    @Override
    protected boolean checkImpl(final AccessLevel accessLevel, final JoinPoint joinPoint, final UserI user, final HttpServletRequest request) {
        return _preferences.getUiAllowNonAdminProjectCreation() || Groups.hasAllDataAdmin(user);
    }
}
