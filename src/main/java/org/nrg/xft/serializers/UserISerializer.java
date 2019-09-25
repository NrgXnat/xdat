package org.nrg.xft.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.extern.slf4j.Slf4j;
import org.nrg.xft.security.UserI;

import java.io.IOException;

@Slf4j
public class UserISerializer extends JsonSerializer<UserI> {
    @Override
    public void serialize(final UserI user, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
        log.debug("Serializing UserI object for username: {}", user.getUsername());
        generator.writeStartObject();
        generator.writeNumberField("id", user.getID());
        generator.writeStringField("username", user.getUsername());
        generator.writeStringField("email", user.getEmail());
        generator.writeStringField("firstname", user.getFirstname());
        generator.writeStringField("lastname", user.getLastname());
        generator.writeObjectField("lastModified", user.getLastModified());
        generator.writeObjectField("authorization", user.getAuthorization());
        generator.writeObjectField("authorities", user.getAuthorities());
        generator.writeEndObject();
    }
}
