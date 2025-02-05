package io.cucumber.java;

import io.cucumber.core.backend.DefaultDataTableEntryTransformerDefinition;
import io.cucumber.core.backend.Lookup;
import io.cucumber.core.runtime.Invoker;
import io.cucumber.datatable.TableCellByTypeTransformer;
import io.cucumber.datatable.TableEntryByTypeTransformer;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import static io.cucumber.java.InvalidMethodSignatureException.builder;

class JavaDefaultDataTableEntryTransformerDefinition extends AbstractGlueDefinition implements DefaultDataTableEntryTransformerDefinition {

    private final TableEntryByTypeTransformer transformer;
    private final boolean headersToProperties;

    JavaDefaultDataTableEntryTransformerDefinition(Method method, Lookup lookup) {
        super(requireValidMethod(method), lookup);
        this.headersToProperties = false;
        this.transformer = this::execute;
    }

    JavaDefaultDataTableEntryTransformerDefinition(Method method, Lookup lookup, boolean headersToProperties) {
        super(requireValidMethod(method), lookup);
        this.headersToProperties = headersToProperties;
        this.transformer = this::execute;
    }

    private static Method requireValidMethod(Method method) {
        Class<?> returnType = method.getReturnType();
        if (Void.class.equals(returnType) || void.class.equals(returnType)) {
            throw createInvalidSignatureException(method);
        }

        Type[] parameterTypes = method.getParameterTypes();
        Type[] genericParameterTypes = method.getGenericParameterTypes();

        if (parameterTypes.length < 2 || parameterTypes.length > 3) {
            throw createInvalidSignatureException(method);
        }

        Type parameterType = genericParameterTypes[0];

        if (!Object.class.equals(parameterType)) {
            if (!(parameterType instanceof ParameterizedType)) {
                throw createInvalidSignatureException(method);
            }
            ParameterizedType parameterizedType = (ParameterizedType) parameterType;
            Type rawType = parameterizedType.getRawType();
            if (!Map.class.equals(rawType)) {
                throw createInvalidSignatureException(method);
            }
            Type[] typeParameters = parameterizedType.getActualTypeArguments();
            for (Type typeParameter : typeParameters) {
                if (!String.class.equals(typeParameter)) {
                    throw createInvalidSignatureException(method);
                }
            }
        }

        if (!Type.class.equals(parameterTypes[1])) {
            throw createInvalidSignatureException(method);
        }

        if (parameterTypes.length == 3) {
            if (!(Object.class.equals(parameterTypes[2]) || TableCellByTypeTransformer.class.equals(parameterTypes[2]))) {
                throw createInvalidSignatureException(method);
            }
        }

        return method;
    }

    private static InvalidMethodSignatureException createInvalidSignatureException(Method method) {
        return builder(method)
            .addAnnotation(DefaultDataTableEntryTransformer.class)
            .addSignature("public Object defaultDataTableEntry(Map<String, String> fromValue, Type toValueType)")
            .addSignature("public Object defaultDataTableEntry(Object fromValue, Type toValueType)")
            .build();
    }

    @Override
    public TableEntryByTypeTransformer tableEntryByTypeTransformer() {
        return transformer;
    }

    @Override
    public boolean headersToProperties() {
        return headersToProperties;
    }

    private Object execute(Map<String, String> fromValue, Type toValueType, TableCellByTypeTransformer cellTransformer) throws Throwable {
        Object[] args;
        if (method.getParameterTypes().length == 3) {
            args = new Object[]{fromValue, toValueType, cellTransformer};
        } else {
            args = new Object[]{fromValue, toValueType};
        }
        return Invoker.invoke(lookup.getInstance(method.getDeclaringClass()), method, args);
    }

}
