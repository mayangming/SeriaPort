package com.machine.serialport;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class TestParseGenerateType {

    @Test
    public void main() {
        testParseResBody1();
        testParseResBody2();
    }

    private static void testParseResBody1() {
        String json = "{\"body\":{\"name\":\"areful\"}}";
        ResponseType<ResBody1> r = new Parser<ResBody1>() {
        }.parse(json);

        assert r != null;
        ResBody1 resBody1 = r.getBody();
        System.out.println(resBody1.name);
    }

    private static void testParseResBody2() {
        String json = "{\"body\":{\"code\":1997}}";
        ResponseType<ResBody2> r = new Parser<ResBody2>() {
        }.parse(json);

        assert r != null;
        ResBody2 resBody2 = r.getBody();
        System.out.println(resBody2.code);
    }

    public static class ResBody1 {
        public String name;
    }

    public static class ResBody2 {
        public int code;
    }

    public static class ResponseType<T> {
        private T body;

        public T getBody() {
            return body;
        }
    }
}