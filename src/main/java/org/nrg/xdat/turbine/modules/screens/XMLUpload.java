/*
 * core: org.nrg.xdat.turbine.modules.screens.XMLUpload
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xdat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.security.helpers.Features;
import org.nrg.xft.security.UserI;

@SuppressWarnings("unused")
public class XMLUpload extends SecureScreen {
    public static final String MESSAGE_NO_GUEST_PERMISSIONS = "Guest users are not permitted to perform any write operations";
    public static final String MESSAGE_FEATURE_BLOCKED = "XML Upload disabled.";

    @Override
    protected void doBuildTemplate(final RunData data, final Context context) throws Exception {
        final UserI user = getUser();
        if (user.isGuest()) {
            data.setScreenTemplate("Error.vm");
            data.setMessage("Permissions Exception.<BR><BR>" + MESSAGE_NO_GUEST_PERMISSIONS);
        }

        if(!Features.checkFeatureForAnyTag(user,Features.XML_UPLOAD)){
            data.setScreenTemplate("Error.vm");
            data.setMessage("Permissions Exception.<BR><BR>" + MESSAGE_FEATURE_BLOCKED);
            return;
        }

    }
}
