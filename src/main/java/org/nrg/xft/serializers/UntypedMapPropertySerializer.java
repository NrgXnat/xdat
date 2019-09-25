package org.nrg.xft.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.extern.slf4j.Slf4j;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

import java.io.IOException;
import java.util.Map;

@Slf4j
@SuppressWarnings("rawtypes")
public class UntypedMapPropertySerializer extends JsonSerializer<Map> {
    public UntypedMapPropertySerializer() {
        _itemSerializer = new ItemISerializer();
        _userSerializer = new UserISerializer();
    }

    @Override
    public void serialize(final Map map, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        for (final Object key : map.keySet()) {
            final String   field      = key.toString();
            final Object   value      = map.get(key);
            final Class<?> valueClass = value.getClass();
            if (ItemI.class.isAssignableFrom(valueClass)) {
                _itemSerializer.serialize((ItemI) value, generator, provider);
            } else if (UserI.class.isAssignableFrom(valueClass)) {
                _userSerializer.serialize((UserI) value, generator, provider);
            } else {
                final JsonSerializer<Object> serializer = provider.findValueSerializer(valueClass);
                if (serializer != null) {
                    serializer.serialize(value, generator, provider);
                } else if (valueClass.isPrimitive()) {
                    generator.writeStringField(field, value.toString());
                } else {
                    log.info("Skipping serialization of untyped-map property \"{}\" with value type {} because I don't know what it is and don't know if it's safe to serialize", field, valueClass.getName());
                }
            }
        }
        generator.writeEndObject();
    }

    private final ItemISerializer _itemSerializer;
    private final UserISerializer _userSerializer;
}
