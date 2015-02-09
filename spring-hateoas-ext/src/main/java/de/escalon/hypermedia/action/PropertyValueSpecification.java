/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.action;

/**
 * Created by dschulten on 20.10.2014.
 */
public class PropertyValueSpecification {

    /**
     * Whether the property must be filled in to complete the action. Default is false.
     * Equivalent to HTML's input@required.
     */
    public Boolean valueRequired;

    /**
     * The default value for the property. For properties that expect a DataType,
     * it's a literal value, for properties that expect an object, it's an ID
     * reference to one of the current values. Equivalent to HTML's input@value.
     */
    public Object defaultValue;
    /**
     * Indicates the name of the PropertyValueSpecification to be used in URL templates
     * and form encoding in a manner analogous to HTML's input@name.
     */
    public String valueName;
    /**
     * Whether or not a property is mutable. Default is false.
     * Equivalent to HTML's input@readonly. Specifying this for a property that also has a value makes it act
     * similar to a ""hidden"" input in an HTML form.
     */
    public Boolean readonlyValue;
    /**
     * Whether multiple values are allowed for the property. Default is false.
     * Equivalent to HTML's input@multiple.
     */
    public Boolean multipleValues;
    /**
     * Specifies the minimum number of characters in a literal value.
     * Equivalent to HTML's input@minlength.
     */
    public Number valueMinLength;
    /**
     * Specifies the maximum number of characters in a literal value.
     * Equivalent to HTML's input@maxlength.
     */
    public Number valueMaxLength;
    /**
     * Specifies a regular expression for testing literal values
     * Equivalent to HTML's input@pattern.
     */
    String valuePattern;
    /**
     * Specifies the allowed range and intervals for literal values.
     * Equivalent to HTML's input@min, max, step.The lower value of some characteristic or property (Number, Date, Time, DateTime)
     */
    public Object minValue;
    /**
     * The upper value of some characteristic or property.
     * Equivalent to HTML's input@min, max, step. (Number, Date, Time, DateTime)
     */
    public Object maxValue;
    /**
     * The step attribute indicates the granularity that is expected (and required) of the value.
     */
    public Number stepValue;

}
