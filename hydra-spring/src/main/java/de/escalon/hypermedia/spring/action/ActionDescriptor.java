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

import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UriComponents;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Describes an HTTP action. Has knowledge about possible request data, e.g. which types and values are suitable for an
 * action. For example, an action descriptor can be used to create a form with select options and typed input fields
 * that calls a Controller method which handles the request built by the form.
 * 
 * @author Dietrich Schulten
 * 
 */
public class ActionDescriptor {

	private UriComponents actionLink;
	private Map<String, ActionInputParameter> requestParams = new LinkedHashMap<String, ActionInputParameter>();
	private RequestMethod httpMethod;
	private String actionName;
	private Map<String, ActionInputParameter> pathVariables = new LinkedHashMap<String, ActionInputParameter>();

    /**
     * Creates an action descriptor.
     *
     * @param actionName can be used by the action representation, e.g. to identify the action using a form name.
     * @param requestMethod used during submit
     */
    public ActionDescriptor(String actionName, RequestMethod requestMethod) {
        this.httpMethod = requestMethod;
        this.actionName = actionName;
        this.actionLink = null;
    }

    public void setActionLink(UriComponents actionLink) {
        this.actionLink = actionLink;
    }

    public String getActionName() {
		return actionName;
	}

	public RequestMethod getHttpMethod() {
		return httpMethod;
	}

	public String getActionLink() {
		return actionLink.toString();
	}

	public String getRelativeActionLink() {
		return actionLink.getPath();
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

	public ActionInputParameter getParameterValue(String name) {
		ActionInputParameter ret = requestParams.get(name);
		if (ret == null) {
			ret = pathVariables.get(name);
		}
		return ret;
	}

}
