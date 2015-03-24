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

import org.omg.CORBA.Current;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;


/**
 * Distinguishes and creates data types, e.g. for serialization/deserialization.
 * Created by dschulten on 22.10.2014.
 */
public class DataType {

    /**
     * Determines if the given class holds only one data item.
     *
     * @param clazz to check
     * @return true if class is scalar
     */
    public static boolean isSingleValueType(Class<?> clazz) {
        boolean ret;
        if (clazz.isPrimitive()
                || Number.class.isAssignableFrom(clazz)
                || Boolean.class.isAssignableFrom(clazz)
                || String.class.isAssignableFrom(clazz)
                || Enum.class.isAssignableFrom(clazz)
                || Date.class.isAssignableFrom(clazz)
                || Calendar.class.isAssignableFrom(clazz)
                || Currency.class.isAssignableFrom(clazz)
                ) {
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

    public static boolean isInteger(Class<?> clazz) {
        return (
                Integer.class.isAssignableFrom(clazz) ||
                        int.class == clazz
        );
    }

    public static boolean isLong(Class<?> clazz) {
        return (
                Long.class.isAssignableFrom(clazz) ||
                        long.class == clazz
        );
    }

    public static boolean isFloat(Class<?> clazz) {
        return (
                Float.class.isAssignableFrom(clazz) ||
                        float.class == clazz
        );
    }

    public static boolean isDouble(Class<?> clazz) {
        return (
                Double.class.isAssignableFrom(clazz) ||
                        double.class == clazz
        );
    }

    public static boolean isByte(Class<?> clazz) {
        return (
                Byte.class.isAssignableFrom(clazz) ||
                        byte.class == clazz
        );
    }

    public static boolean isShort(Class<?> clazz) {
        return (
                Short.class.isAssignableFrom(clazz) ||
                        short.class == clazz
        );
    }

    public static boolean isBigInteger(Class<?> clazz) {
        return BigInteger.class.isAssignableFrom(clazz);
    }

    public static boolean isBigDecimal(Class<?> clazz) {
        return BigDecimal.class.isAssignableFrom(clazz);
    }

    public static boolean isDate(Class<?> clazz) {
        return Date.class.isAssignableFrom(clazz);
    }

    public static boolean isCalendar(Class<?> clazz) {
        return Calendar.class.isAssignableFrom(clazz);
    }

    public static boolean isCurrency(Class<?> clazz) {
        return Currency.class.isAssignableFrom(clazz);
    }





    public static Object asType(Class<?> type, String string) {
        if (isBoolean(type)) {
            return Boolean.parseBoolean(string);
        } else if (isInteger(type)) {
            return Integer.parseInt(string);
        } else if (isLong(type)) {
            return Long.parseLong(string);
        } else if (isDouble(type)) {
            return Double.parseDouble(string);
        } else if (isFloat(type)) {
            return Float.parseFloat(string);
        } else if (isByte(type)) {
            return Byte.parseByte(string);
        } else if (isShort(type)) {
            return Short.parseShort(string);
        } else if (isBigInteger(type)) {
            return new BigInteger(string);
        } else if (isBigDecimal(type)) {
            return new BigDecimal(string);
        } else if (isDate(type)) {
            // TODO handle ISO date or epoch
            return new Date(Long.parseLong(string));
        } else if (isCurrency(type)) {
            return Currency.getInstance(string);
        } else if (type.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) type, string);
        } else {
            return string;
        }
    }
}
