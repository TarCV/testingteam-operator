/*
 * Copyright 2019 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.ondevice;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class AnnontationReadingFilter extends Filter {
    private static final int MSG_LENGTH_LIMIT_WITHOUT_PREFIX = 4000 - 16;
    private final AtomicInteger index = new AtomicInteger();
    private final String objectId = String.format("%08x", System.identityHashCode(this));

    @Override
    public boolean shouldRun(Description description) {
        if (!description.isTest()) {
            return true;
        }

        JSONObject info = new JSONObject();
        JSONArray annotationsInfo = new JSONArray();

        try {
            info.put("testClass", description.getClassName());
            info.put("testMethod", description.getMethodName());

            // Method annotations override class ones, so they should be added last
            Class<?> testClass = description.getTestClass();
            appendSuperclassAnnotationsRoot(testClass, annotationsInfo);
            appendAnnotationInfos(Arrays.asList(testClass.getDeclaredAnnotations()),
                    annotationsInfo);
            appendAnnotationInfos(description.getAnnotations(), annotationsInfo);

            info.put("annotations", annotationsInfo);

            String message = info.toString() + ",";
            int messageLength = message.length();
            for (int i = 0; i < messageLength; i += MSG_LENGTH_LIMIT_WITHOUT_PREFIX) {
                String linePrefix = objectId + "-" + String.format("%08x", index.getAndIncrement()) + ":";
                int endIndex = Math.min(i + MSG_LENGTH_LIMIT_WITHOUT_PREFIX, messageLength);
                Log.i("Tongs.TestInfo", linePrefix + message.substring(i, endIndex));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    private void appendSuperclassAnnotationsRoot(Class<?> testClass, JSONArray annotationsInfo) throws JSONException {
        for (Class<?> iface : testClass.getInterfaces()) {
            appendSuperclassAnnotationsFull(iface, annotationsInfo);
        }

        Class<?> superclass = testClass.getSuperclass();
        if (superclass != Object.class && superclass != testClass && superclass != null) {
            appendSuperclassAnnotationsFull(superclass, annotationsInfo);
        }
    }

    private void appendSuperclassAnnotationsFull(Class<?> testClass, JSONArray annotationsInfo) throws JSONException {
        appendSuperclassAnnotationsRoot(testClass, annotationsInfo);

        ArrayList<Annotation> eligibleAnnotations = new ArrayList<>();
        for (Annotation annotation : testClass.getDeclaredAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(Inherited.class)) {
                eligibleAnnotations.add(annotation);
            }
        }

        appendAnnotationInfos(eligibleAnnotations, annotationsInfo);
    }

    private void appendAnnotationInfos(Iterable<Annotation> annotations, JSONArray annotationsInfo) throws JSONException {
        for (Annotation annotation: annotations) {
            JSONObject currentAnnotationInfo = new JSONObject();
            Class<? extends Annotation> annotationType = annotation.annotationType();

            currentAnnotationInfo.put("annotationType", annotationType.getCanonicalName());

            for (Method method : annotationType.getMethods()) {
                if (!isAnnotationParameter(method)) {
                    continue;
                }

                String name = method.getName();
                Object value = getAnnotationParameterValue(annotation, method);
                currentAnnotationInfo.put(name, JSONObject.wrap(value));
            }

            annotationsInfo.put(currentAnnotationInfo);
        }
    }

    private Object getAnnotationParameterValue(Annotation annotation, Method method) {
        try {
            return method.invoke(annotation);
        } catch (IllegalAccessException e) {
            throw createAnnotationParameterValueException(method, e);
        } catch (InvocationTargetException e) {
            throw createAnnotationParameterValueException(method, e);
        }
    }

    private static RuntimeException createAnnotationParameterValueException(Method method, Exception e) {
        return new RuntimeException(String.format("Failed to get value of '%s' annotation parameter", method.getName()), e);
    }

    private boolean isAnnotationParameter(Method method) {
        if (method.getParameterTypes().length > 0) {
            return false;
        }
        switch (method.getName()) {
            case "annotationType":
            case "hashCode":
            case "toString":
                return false;
        }

        return true;
    }

    @Override
    public String describe() {
        return "reading annotations";
    }
}
