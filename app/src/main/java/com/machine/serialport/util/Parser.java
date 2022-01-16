package com.machine.serialport.util;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.machine.serialport.model.HttpBaseResponseMode;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class Parser<T> {
    private static final Gson gson = new GsonBuilder().create();
    private final Class<?> clazz = HttpBaseResponseMode.class;

    public HttpBaseResponseMode<T> parse(String json) {
        try {
            ParameterizedType type = (ParameterizedType) this.getClass().getGenericSuperclass();
            Type objectType = buildType(clazz, type.getActualTypeArguments());
            return gson.fromJson(json, objectType);
        } catch (Exception ignored) {
        }
        return null;
    }

    private static ParameterizedType buildType(final Class<?> raw, final Type... args) {
        return new ParameterizedType() {
            public Type getRawType() {
                return raw;
            }

            public Type[] getActualTypeArguments() {
                return args;
            }

            public Type getOwnerType() {
                return null;
            }
        };
    }
}