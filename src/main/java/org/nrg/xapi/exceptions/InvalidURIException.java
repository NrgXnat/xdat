package org.nrg.xapi.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class InvalidURIException extends DataFormatException {
    public InvalidURIException(final String uri) {
        super(HttpStatus.NOT_FOUND, "Invalid URI: " + uri);
    }
}
