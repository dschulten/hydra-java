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

import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;

/**
 * Describes an HTTP method independently of a specific rest framework.
 * Has knowledge about possible request data, i.e. which types and values
 * are suitable for an action. For example, an action descriptor can be used to create a form with select options and
 * typed input fields that calls a POST handler. It has {@link ActionInputParameter}s which represent method handler
 * arguments. Supported method handler arguments are:
 * <ul>
 * <li>path variables</li>
 * <li>request params (url query params)</li>
 * <li>request headers</li>
 * <li>request body</li>
 * </ul>
 *
 * @author Dietrich Schulten
 */
public class ActionDescriptor {

    private RequestMethod httpMethod;
    private String actionName;

    private String semanticActionType;
    private Map<String, ActionInputParameter> requestParams = new LinkedHashMap<String, ActionInputParameter>();
    private Map<String, ActionInputParameter> pathVariables = new LinkedHashMap<String, ActionInputParameter>();
    private Map<String, ActionInputParameter> requestHeaders = new LinkedHashMap<String, ActionInputParameter>();

    private ActionInputParameter requestBody;
    private Cardinality cardinality = Cardinality.SINGLE;

    /**
     * Creates an {@link ActionDescriptor}.
     *
     * @param actionName name of the action, e.g. the method name of the handler method.
     *                   Can be used by an action representation, e.g. to identify the action using a form name.
     * @param httpMethod used during submit
     */
    public ActionDescriptor(String actionName, RequestMethod httpMethod) {
        Assert.notNull(actionName);
        Assert.notNull(httpMethod);
        this.httpMethod = httpMethod;
        this.actionName = actionName;
    }

    /**
     * The name of the action, usually the method name of the handler method.
     *
     * @return action name, never null
     */
    public String getActionName() {
        return actionName;
    }

    /**
     * Gets the http method of this action.
     * @return method, never null
     */
    public RequestMethod getHttpMethod() {
        return httpMethod;
    }

    /**
     * Gets the path variable names.
     * @return names or empty collection, never null
     */
    public Collection<String> getPathVariableNames() {
        return pathVariables.keySet();
    }

    /**
     * Gets the request header names.
     * @return names or empty collection, never null
     */
    public Collection<String> getRequestHeaderNames() {
        return requestHeaders.keySet();
    }

    /**
     * Gets the request parameter (query param) names.
     * @return names or empty collection, never null
     */
    public Collection<String> getRequestParamNames() {
        return requestParams.keySet();
    }

    /**
     * Adds descriptor for request param.
     * @param key name of request param
     * @param actionInputParameter descriptor
     */
    public void addRequestParam(String key, ActionInputParameter actionInputParameter) {
        requestParams.put(key, actionInputParameter);
    }

    /**
     * Adds descriptor for path variable.
     * @param key name of path variable
     * @param actionInputParameter descriptor
     */

    public void addPathVariable(String key, ActionInputParameter actionInputParameter) {
        pathVariables.put(key, actionInputParameter);
    }

    /**
     * Adds descriptor for request header.
     * @param key name of request header
     * @param actionInputParameter descriptor
     */
    public void addRequestHeader(String key, ActionInputParameter actionInputParameter) {
        requestHeaders.put(key, actionInputParameter);
    }

    /**
     * Gets input parameter info which is part of the URL mapping, both request parameters and path variables.
     *
     * @param name to retrieve
     * @return parameter descriptor or null
     */
    public ActionInputParameter getActionInputParameter(String name) {
        ActionInputParameter ret = requestParams.get(name);
        if (ret == null) {
            ret = pathVariables.get(name);
        }
        return ret;
    }

    /**
     * Gets request header info.
     *
     * @param name of the request header
     * @return request header descriptor or null
     */
    public ActionInputParameter getRequestHeader(String name) {
        return requestHeaders.get(name);
    }

    /**
     * Gets request body info.
     *
     * @return request body descriptor or null
     */
    public ActionInputParameter getRequestBody() {
        return requestBody;
    }

    /**
     * Determines if this descriptor has a request body.
     *
     * @return true if request body is present
     */
    public boolean hasRequestBody() {
        return requestBody != null;
    }

    /**
     * Allows to set request body descriptor.
     *
     * @param requestBody descripto
     */
    public void setRequestBody(ActionInputParameter requestBody) {
        this.requestBody = requestBody;
    }

    /**
     * Gets semantic type of action, e.g. a subtype of hydra:Operation or schema:Action.
     * Use {@link Action} on a method handler to define the semantic type of an action.
     *
     * @return URL identifying the type
     */
    public String getSemanticActionType() {
        return semanticActionType;
    }

    /**
     * Sets semantic type of action, e.g. a subtype of hydra:Operation or schema:Action.
     *
     * @param semanticActionType URL identifying the type
     */
    public void setSemanticActionType(String semanticActionType) {
        this.semanticActionType = semanticActionType;
    }

    /**
     * Determines action input parameters for required url variables.
     *
     * @return required url variables
     */
    public Map<String, ActionInputParameter> getRequiredUrlVariables() {
        Map<String, ActionInputParameter> ret = new HashMap<String, ActionInputParameter>();
        for (Map.Entry<String, ActionInputParameter> entry : requestParams.entrySet()) {
            ActionInputParameter actionInputParameter = entry.getValue();
            if (actionInputParameter.isRequired()) {
                ret.put(entry.getKey(), actionInputParameter);
            }
        }
        for (Map.Entry<String, ActionInputParameter> entry : pathVariables.entrySet()) {
            ActionInputParameter actionInputParameter = entry.getValue();
            ret.put(entry.getKey(), actionInputParameter);
        }
        // requestBody not supported, would have to use exploded modifier
        return ret;
    }


    /**
     * Allows to set the cardinality, i.e. specify if the action refers to a collection or a single resource.
     * Default is {@link Cardinality#SINGLE}
     * @param cardinality to set
     */
    public void setCardinality(Cardinality cardinality) {
        this.cardinality = cardinality;
    }

    /**
     * Allows to decide whether or not the action refers to a collection resource.
     * @return cardinality
     */
    public Cardinality getCardinality() {
        return cardinality;
    }
}
