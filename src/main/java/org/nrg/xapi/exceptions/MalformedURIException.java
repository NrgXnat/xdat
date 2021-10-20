package org.nrg.xapi.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MalformedURIException extends DataFormatException {
    public MalformedURIException(final String uri) {
        super("Malformed URI: " + uri);
    }
}
