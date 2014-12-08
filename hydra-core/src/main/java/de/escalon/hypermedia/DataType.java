/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia;

import java.util.Collection;

/**
 * Distinguishes data types for JSON serialization.
 * Created by dschulten on 22.10.2014.
 */
public class DataType {

    /**
     * Determines if the given class holds only one data item.
     *
     * @param clazz to check
     * @return true if class is scalar
     */
    public static boolean isScalar(Class<?> clazz) {
        boolean ret;
        if (clazz.isPrimitive()
                || Number.class.isAssignableFrom(clazz)
                || Boolean.class.isAssignableFrom(clazz)
                || String.class.isAssignableFrom(clazz)
                || Enum.class.isAssignableFrom(clazz)) {
            ret = true;
        } else {
            ret = false;
        }
        return ret;

    }

    public static boolean isArrayOrCollection(Class<?> parameterType) {
        return (parameterType.isArray() || Collection.class.isAssignableFrom(parameterType));
    }

    public static boolean isBoolean(Class<?> parameterType) {
        return Boolean.class.isAssignableFrom(parameterType);
    }

    public static boolean isNumber(Class<?> clazz) {
        return (
                Number.class.isAssignableFrom(clazz) ||
                        int.class == clazz ||
                        long.class == clazz ||
                        float.class == clazz ||
                        byte.class == clazz ||
                        short.class == clazz ||
                        double.class == clazz
        );
    }


}
