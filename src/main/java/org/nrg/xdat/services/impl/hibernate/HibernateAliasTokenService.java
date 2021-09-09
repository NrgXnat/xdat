/*
 * core: org.nrg.xdat.services.impl.hibernate.HibernateAliasTokenService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*
 * HibernateAliasTokenService
 * (C) 2016 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 */
package org.nrg.xdat.services.impl.hibernate;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xapi.exceptions.DataFormatException;
import org.nrg.xapi.exceptions.InsufficientPrivilegesException;
import org.nrg.xapi.exceptions.NotAuthenticatedException;
import org.nrg.xdat.daos.AliasTokenDAO;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.nrg.framework.orm.DatabaseHelper.convertPGIntervalToSeconds;

@Service
public class HibernateAliasTokenService extends AbstractHibernateEntityService<AliasToken, AliasTokenDAO> implements AliasTokenService {
    @Autowired
    public HibernateAliasTokenService(final AliasTokenDAO dao, final SiteConfigPreferences preferences, final UserManagementServiceI userService) {
        _dao = dao;
        _preferences = preferences;
        _userService = userService;
    }

    /**
     * Finds all active tokens for a particular user.
     *
     * @param xdatUserId The user ID from the XdatUser table.
     *
     * @return An list of the {@link AliasToken alias tokens} issued to the indicated user.
     */
    @Override
    @Transactional
    public List<AliasToken> findTokensForUser(String xdatUserId) {
        return getDao().findByXdatUserId(xdatUserId);

    }

    /**
     * Finds and deactivates all active tokens for a particular user.
     *
     * @param username The username of the user to deactivate.
     */
    @Override
    @Transactional
    public void deactivateAllTokensForUser(String username) {
        List<AliasToken> tokens = findTokensForUser(username);
        if (tokens != null) {
            for (AliasToken token : tokens) {
                token.setEnabled(false);
            }
        }
    }

    @Override
    @Transactional
    public AliasToken issueTokenForUser(final String username) throws Exception {
        return issueTokenForUser(_userService.getUser(username));
    }

    @Override
    @Transactional
    public AliasToken issueTokenForUser(final UserI xdatUser) {
        return issueTokenForUser(xdatUser, null);
    }

    @Override
    @Transactional
    public AliasToken issueTokenForUser(final UserI xdatUser, final boolean isSingleUse) {
        return issueTokenForUser(xdatUser, isSingleUse, null);
    }

    @Override
    @Transactional
    public AliasToken issueTokenForUser(final UserI xdatUser, Set<String> validIPAddresses) {
        return issueTokenForUser(xdatUser, false, validIPAddresses);
    }

    @Override
    @Transactional
    public AliasToken issueTokenForUser(final UserI xdatUser, boolean isSingleUse, Set<String> validIPAddresses) {
        AliasToken token = newEntity();
        final Calendar calendar = Calendar.getInstance();
        final long seconds = convertPGIntervalToSeconds(_preferences.getAliasTokenTimeout());
        calendar.add(Calendar.SECOND, ((Long) seconds).intValue());
        token.setEstimatedExpirationTime(calendar.getTime());
        token.setXdatUserId(xdatUser.getLogin());
        token.setSingleUse(isSingleUse);
        token.setValidIPAddresses(validIPAddresses);
        getDao().create(token);
        if (_log.isDebugEnabled()) {
            _log.debug("Created new token " + token.getAlias() + " for user: " + xdatUser.getLogin());
        }
        return token;
    }

    /**
     * Locates and returns the token indicated by the alias string. The returned token should not be considered fully
     * validated until the {@link org.nrg.xdat.entities.AliasToken#getSecret() token secret} and {@link org.nrg.xdat.entities.AliasToken#getValidIPAddresses() IP
     * addresses} have also been checked and validated against the requesting client.
     *
     * @param alias The alias for the requested token.
     *
     * @return The token matching the indicated alias if one exists; otherwise this returns null.
     */
    @Override
    @Transactional
    public AliasToken locateToken(final String alias) {
        AliasToken token = getDao().findByAlias(alias);
        if (token == null) {
            if (_log.isInfoEnabled()) {
                _log.info("Token location requested but not found for alias: " + alias);
            }
            return null;
        }

        if (_log.isDebugEnabled()) {
            _log.debug("Token located for alias: " + alias);
        }

        return token;
    }

    @Override
    @Transactional
    public String validateToken(final String alias, final String secret) {
        return validateToken(alias, secret, null);
    }

    @Override
    @Transactional
    public String validateToken(final String alias, final String secret, final String address) {
        AliasToken token = getDao().findByAlias(alias);
        if (token == null) {
            if (_log.isInfoEnabled()) {
                _log.info("Token requested but not found for alias: " + alias);
            }
            return null;
        }
        try {
            return StringUtils.equals(secret, token.getSecret()) && token.isValidIPAddress(address) ? token.getXdatUserId() : null;
        } finally {
            if (token.isSingleUse()) {
                getDao().delete(token);
            }
        }
    }

    @Override
    @Transactional
    public void invalidateToken(final String alias) {
        AliasToken token = getDao().findByAlias(alias);
        if (token == null) {
            if (_log.isInfoEnabled()) {
                _log.info("Token requested to be invalidated but not found for alias: " + alias);
            }
        } else {
            invalidateToken(token);
        }
    }

    @Override
    @Transactional
    public void invalidateToken(final AliasToken token) {
        if (token != null) {
            if (_log.isDebugEnabled()) {
                _log.debug("Invalidating token: " + token.getAlias());
            }
            getDao().delete(token);
        }
    }

    @Override
    @Transactional
    public void invalidateExpiredTokens(String interval) {
        final List<AliasToken> tokensToExpire = _dao.findByExpired(interval);
        if (tokensToExpire != null) {
            for (final AliasToken token : tokensToExpire) {
                if (token != null) {
                    invalidateToken(token);
                }
            }
        }
    }
    
    @Override
	public Object findAliasTokenByOperationOrUserNameOrTokenOrSecret(UserI user, String operation, String tokenId, String secret, String requestedUserName) throws DataFormatException, InsufficientPrivilegesException, NotFoundException, NotAuthenticatedException {
    	final boolean hasUsername = StringUtils.isNotBlank(requestedUserName);
		if (hasUsername && !StringUtils.equals(requestedUserName, user.getUsername()) && !Roles.isSiteAdmin(user)) {
			throw new InsufficientPrivilegesException("Only admins can work with alias tokens for other users.");
		}
		if (StringUtils.isBlank(operation)) {
			throw new DataFormatException("Operation value wasn't found");
		}
    	String token = getToken(tokenId);
    	switch (operation) {
    	case OP_ISSUE:
			Object issueResult = getIssueOperatioAliasToken(operation, hasUsername, user, requestedUserName);
			if (Objects.isNull(issueResult)) {
				throw new NotFoundException("Alias token issue opertion wasn't found");
			}
			return issueResult;
    	case OP_SHOW:
    		Object showResults =  getShowOperationAliasToken(operation, hasUsername, user, requestedUserName);
			if (Objects.isNull(showResults)) {
				throw new NotFoundException("Alias token show operation wasn't found");
			}
			return showResults;
		case OP_VALIDATE:
			return  getValidateOperationAliasToken(token, secret);
		case OP_INVALIDATE:
			return  getInValidateOperationAliasToken(token);
		default:
			throw new DataFormatException("Unknown operation: " + operation);
    	}
	}
    
    private AliasToken getIssueOperatioAliasToken(String operation, boolean hasUsername, UserI user, String requestedUserName) throws DataFormatException {
		if (operation.equals(OP_ISSUE)) {
			try {
				return hasUsername ? issueTokenForUser(requestedUserName) : issueTokenForUser(user);
			} catch (Exception e) {
				throw new DataFormatException("An error occurred retrieving the user: " + requestedUserName);
			}
		}
		return null;
	}
	
	private List<AliasToken> getShowOperationAliasToken(String operation, boolean hasUsername, UserI user, String requestedUserName) throws DataFormatException {
		if (operation.equals(OP_SHOW)) {
			final String username = hasUsername ? requestedUserName : user.getUsername();
			if (StringUtils.isBlank(username)) {
				throw new DataFormatException("No user found: " + requestedUserName);
			}
			return findTokensForUser(username);
		}
		return null;

	}
	
	private HashMap<String, String> getValidateOperationAliasToken(String token , String secret) throws NotAuthenticatedException{
		if (StringUtils.isBlank(token) || StringUtils.isBlank(secret)) {
			throw new NotAuthenticatedException("You must specify both token and secret to validate a token.");
        }
		final HashMap<String, String> results = new HashMap<>();
        results.put("valid", validateToken(token, secret));
        return results;
	}
	
	private HashMap<String, String> getInValidateOperationAliasToken(String token){
		final HashMap<String, String> results = new HashMap<>();
		invalidateToken(token);
		results.put("result","OK");
		return results;
	}

	private String getToken(String tokenId) throws DataFormatException {
		String token = null;
		final AliasToken aliseToken;
        if (StringUtils.isBlank(tokenId)) {
        	token = null;
            aliseToken = null;
        } else if (AliasToken.isAliasFormat(tokenId)) {
        	aliseToken = locateToken(tokenId);
            if (aliseToken == null) {
            	throw new DataFormatException("Can't find an alias token with the alias " + tokenId);
            }
            token = tokenId;
        } else if (NumberUtils.isCreatable(tokenId)) {
            try {
            	aliseToken = get(NumberUtils.toLong(tokenId));
            	token = aliseToken.getAlias();
            } catch (NotFoundException e) {
            	throw new DataFormatException("Can't find an alias token with the ID " + tokenId);
            }
        } else {
        	throw new DataFormatException( "Can't find an alias token with the ID " + tokenId);
        }
		return token;
	}

    /**
     * Gets the {@link AliasTokenDAO alias token DAO} instance for this service.
     *
     * @return The {@link AliasTokenDAO alias token DAO} instance for this service.
     *
     * @see AbstractHibernateEntityService#getDao()
     */
    @Override
    protected AliasTokenDAO getDao() {
        return _dao;
    }

    private static final Log _log = LogFactory.getLog(HibernateAliasTokenService.class);

    private final AliasTokenDAO          _dao;
    private final SiteConfigPreferences  _preferences;
    private final UserManagementServiceI _userService;
    private static final String OP_SHOW = "show";
	private static final String OP_ISSUE = "issue";
	private static final String OP_VALIDATE = "validate";
	private static final String OP_INVALIDATE = "invalidate";
}
