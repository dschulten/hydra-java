/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.spring.action;

import de.escalon.hypermedia.spring.UriTemplateComponents;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Describes an HTTP action. Has knowledge about possible request data, e.g. which types and values are suitable for an
 * action. For example, an action descriptor can be used to create a form with select options and typed input fields
 * that calls a Controller method which handles the request built by the form.
 *
 * @author Dietrich Schulten
 */
public class ActionDescriptor {

//    private final UriTemplateComponents uriTemplateComponents;
    private RequestMethod httpMethod;
    private String actionName;

    private String semanticActionType;
    private Map<String, ActionInputParameter> requestParams = new LinkedHashMap<String, ActionInputParameter>();
    private Map<String, ActionInputParameter> pathVariables = new LinkedHashMap<String, ActionInputParameter>();
    private ActionInputParameter requestBody;

    /**
     * Creates an action descriptor.
     *
     * @param actionName can be used by the action representation, e.g. to identify the action using a form name.
     * @param httpMethod used during submit
     */
    public ActionDescriptor(String actionName, RequestMethod httpMethod) {
//        this.uriTemplateComponents = uriTemplateComponents;
        this.httpMethod = httpMethod;
        this.actionName = actionName;
    }


    public String getActionName() {
        return actionName;
    }

    public RequestMethod getHttpMethod() {
        return httpMethod;
    }

//    public String getActionLink() {
//        return uriTemplateComponents.toString();
//    }

//    public UriTemplateComponents getUriTemplateComponents() {
//        return uriTemplateComponents;
//    }

//	public String getRelativeActionLink() {
//		return actionLink.getPath();
//	}

    public Collection<String> getPathVariableNames() {
        return pathVariables.keySet();
    }

    public Collection<String> getRequestParamNames() {
        return requestParams.keySet();
    }

    public void addRequestParam(String key, ActionInputParameter actionInputParameter) {
        requestParams.put(key, actionInputParameter);
    }

    public void addPathVariable(String key, ActionInputParameter actionInputParameter) {
        pathVariables.put(key, actionInputParameter);
    }

    /**
     * Gets input parameter, both request parameters and path variables.
     *
     * @param name to retrieve
     * @return parameter descriptor
     */
    public ActionInputParameter getActionInputParameter(String name) {
        ActionInputParameter ret = requestParams.get(name);
        if (ret == null) {
            ret = pathVariables.get(name);
        }
        return ret;
    }

    public ActionInputParameter getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(ActionInputParameter requestBody) {
        this.requestBody = requestBody;
    }

    /**
     * Gets semantic type of action, e.g. a subtype of hydra:Operation or schema:Action.
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


//    /**
//     * Creates new ActionDescriptor, but with given uri template components.
//     * @param templateComponents to apply
//     * @return new instance
//     */
//    public ActionDescriptor withUriTemplateComponents(UriTemplateComponents templateComponents) {
//        ActionDescriptor actionDescriptor = new ActionDescriptor(/*templateComponents, */actionName, httpMethod);
//        actionDescriptor.semanticActionType = semanticActionType;
//        actionDescriptor.requestBody = requestBody;
//        actionDescriptor.pathVariables = pathVariables;
//        actionDescriptor.requestParams = requestParams;
//        return actionDescriptor;
//    }
}
