package org.nrg.xft.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.*;

@Component
@Slf4j
public class ItemISerializer extends JsonSerializer<ItemI> {
    @Override
    public void serialize(final ItemI element, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Now serializing a BaseElement instance: {}", displayClassMethods(element.getClass()));
        }

        generator.writeStartObject();

        // TODO: Add a way to have a reasonable way of sorting properties by name in terms of importance, e.g. ID, label, project, things like that should float to the top.
        final Map<String, Object> properties = getItemPropertiesFromHierarchy(element);
        for (final String property : properties.keySet()) {
            final Object value = properties.get(property);
            writeField(generator, property, value);
        }

        generator.writeEndObject();
    }

    private void writeField(final JsonGenerator generator, final String property, final Object value) throws IOException {
        final Class<?> valueClass = value.getClass();
        if (valueClass.equals(String.class)) {
            generator.writeStringField(property, (String) value);
        } else if (NUMBERS.contains(valueClass)) {
            if (valueClass.equals(Integer.class)) {
                generator.writeNumberField(property, (Integer) value);
            } else if (valueClass.equals(Long.class)) {
                generator.writeNumberField(property, (Long) value);
            } else if (valueClass.equals(Float.class)) {
                generator.writeNumberField(property, (Float) value);
            } else if (valueClass.equals(Double.class)) {
                generator.writeNumberField(property, (Double) value);
            } else if (valueClass.equals(BigDecimal.class)) {
                generator.writeNumberField(property, (BigDecimal) value);
            }
        } else if (List.class.isAssignableFrom(valueClass)) {
            generator.writeArrayFieldStart(property);
            //noinspection rawtypes
            for (final Object item : (List) value) {
                generator.writeObject(item);
            }
            generator.writeEndArray();
        } else {
            log.debug("Serializing property \"{}\": this makes me nervous because I don't know what it is: {}", property, valueClass.getName());
            generator.writeObjectField(property, value);
        }
    }

    private Map<String, Object> getItemPropertiesFromHierarchy(final ItemI element) {
        //noinspection rawtypes
        final Hashtable           props      = element.getProps();
        final Map<String, Object> properties = new HashMap<>();
        for (final Object object : props.keySet()) {
            final Object value = props.get(object);
            if (value == null) {
                continue;
            }
            final Class<?> valueClass = value.getClass();
            final String   property   = (String) object;
            if (ItemI.class.isAssignableFrom(valueClass)) {
                final String localName = ((XFTItem) value).getXSIType().split(":")[1];
                if (StringUtils.equalsIgnoreCase(property, localName + "_" + localName)) {
                    properties.putAll(getItemPropertiesFromHierarchy((ItemI) value));
                } else if (StringUtils.endsWithIgnoreCase(property, "_" + localName)) {
                    log.debug("Filtering out metadata or history reference: {}", property);
                }
                continue;
            }
            properties.put(property, value);
        }
        return properties;
    }

    private String displayClassMethods(final Class<?> clazz) {
        final StringWriter buffer = new StringWriter();
        try (final PrintWriter writer = new PrintWriter(buffer)) {
            displayClassMethods(clazz, writer, 0);
        }
        return buffer.toString();
    }

    private void displayClassMethods(final Class<?> clazz, final PrintWriter writer, final int level) {
        if (clazz == null) {
            return;
        }
        final String indent = getIndent(level);
        writer.println(indent + "Class: " + clazz.getName());
        writer.println(indent + " Methods:");
        final Map<String, Field> fields = new HashMap<>(Maps.uniqueIndex(Iterables.filter(Arrays.asList(clazz.getDeclaredFields()), new Predicate<Field>() {
            @Override
            public boolean apply(final Field field) {
                final int modifiers = field.getModifiers();
                return Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers);
            }
        }), new Function<Field, String>() {
            @Override
            public String apply(final Field field) {
                return field.getName();
            }
        }));
        for (final String display : Iterables.transform(Iterables.filter(Arrays.asList(clazz.getDeclaredMethods()), new Predicate<Method>() {
            @Override
            public boolean apply(final Method method) {
                return isPublicGetterMethod(method);
            }
        }), new Function<Method, String>() {
            @Override
            public String apply(final Method method) {
                return displayMethod(method);
            }
        })) {
            if (!fields.isEmpty()) {
                for (final String stripped : getFieldVariants(display)) {
                    fields.remove(stripped);
                }
            }
            writer.println(indent + " * " + display);

        }
        if (!fields.isEmpty()) {
            writer.println(indent + " Fields without getters:");
            for (final String field : fields.keySet()) {
                writer.println(indent + " * " + field + " (" + fields.get(field).getType().getName());
            }
        }
        writer.println();
        displayClassMethods(clazz.getSuperclass(), writer, level + 1);
    }

    private List<String> getFieldVariants(final String display) {
        final String noPrefix      = RegExUtils.removePattern(display, "^(get|is)");
        final String uncapitalized = StringUtils.uncapitalize(noPrefix);
        return Arrays.asList(uncapitalized, "_" + uncapitalized, noPrefix, "_" + noPrefix);
    }

    private String getIndent(final int level) {
        final StringBuilder builder = new StringBuilder();
        for (int index = 0; index < level; index++) {
            builder.append("  ");
        }
        return builder.toString();
    }

    private String displayMethod(final Method method) {
        return String.format("%s %s %s(%s)", Modifier.toString(method.getModifiers()), method.getReturnType().getName(), method.getName(), displayParameters(method.getParameterTypes()));
    }

    private String displayParameters(final Class<?>[] parameterTypes) {
        return parameterTypes.length == 0 ? "" : StringUtils.join(Iterables.transform(Arrays.asList(parameterTypes), new Function<Class<?>, String>() {
            @Override
            public String apply(final Class<?> clazz) {
                return clazz.getName();
            }
        }), ", ");
    }

    private boolean isPublicGetterMethod(final Method method) {
        final int modifiers = method.getModifiers();
        return method.getParameterTypes().length == 0 && Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers) && StringUtils.startsWithAny(method.getName(), "get", "is");
    }

    private static final List<Class<? extends Number>> NUMBERS = Arrays.<Class<? extends Number>>asList(Integer.class, Long.class, Float.class, Double.class, BigDecimal.class);
}
