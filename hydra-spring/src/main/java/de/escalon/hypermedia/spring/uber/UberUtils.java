/*
 * Copyright (c) 2015. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.spring.uber;

import org.springframework.hateoas.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.Map.Entry;

public class UberUtils {

	private UberUtils () {

	}

	static final Set<String> FILTER_RESOURCE_SUPPORT = new HashSet<String>(Arrays.asList("class", "links", "id"));
	static final Set<String> FILTER_BEAN = new HashSet<String>(Arrays.asList("class"));


	/**
	 * Recursively converts object to nodes of uber data.
	 *
	 * @param object to convert
	 * @param objectNode to convert into
	 */
	public static void toUberData(AbstractUberNode objectNode, Object object) {
		Set<String> filtered = FILTER_RESOURCE_SUPPORT;
		if (object == null) {
			return;
		}
		try {
			// TODO: move all returns to else branch of property descriptor handling
			if (object instanceof Resource) {
				Resource<?> resource = (Resource<?>) object;
				objectNode.addLinks(resource.getLinks());
				toUberData(objectNode, resource.getContent());
				return;
			} else if (object instanceof Resources) {
				Resources<?> resources = (Resources<?>) object;

				// TODO set name using EVO see HypermediaSupportBeanDefinitionRegistrar

				objectNode.addLinks(resources.getLinks());

				Collection<?> content = resources.getContent();
				toUberData(objectNode, content);
				return;
			} else if (object instanceof ResourceSupport) {
				ResourceSupport resource = (ResourceSupport) object;

				objectNode.addLinks(resource.getLinks());

				// wrap object attributes below to avoid endless loop

			} else if (object instanceof Collection) {
				Collection<?> collection = (Collection<?>) object;
				for (Object item : collection) {
					UberNode itemNode = new UberNode();
					objectNode.addData(itemNode);
					toUberData(itemNode, item);
				}
				return;
			}
			if (object instanceof Map) {
				Map<?, ?> map = (Map<?, ?>) object;
				for (Entry<?, ?> entry : map.entrySet()) {
					String key = entry.getKey().toString();
					Object content = entry.getValue();
					Object value = getContentAsScalarValue(content);
					UberNode entryNode = new UberNode();
					entryNode.setName(key);
					objectNode.addData(entryNode);
					if (value != null) {
						entryNode.setValue(value);
					} else {
						toUberData(entryNode, content);
					}
				}
			} else {
				PropertyDescriptor[] propertyDescriptors = getPropertyDescriptors(object);// BeanUtils.getPropertyDescriptors(bean.getClass());
				for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
					String name = propertyDescriptor.getName();
					if (filtered.contains(name)) {
						continue;
					}
					UberNode propertyNode = new UberNode();
					Object content = propertyDescriptor.getReadMethod().invoke(object);

					Object value = getContentAsScalarValue(content);
					propertyNode.setName(name);
					objectNode.addData(propertyNode);
					if (value != null) {
						// for each scalar property of a simple bean, add valuepair nodes to data
						propertyNode.setValue(value);
					} else {
						toUberData(propertyNode, content);
					}

				}
			}
		} catch (Exception ex) {
			throw new RuntimeException("failed to transform object " + object, ex);
		}

	}

	private static PropertyDescriptor[] getPropertyDescriptors(Object bean) {
		try {
			return Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors();
		} catch (IntrospectionException e) {
			throw new RuntimeException("failed to get property descriptors of bean " + bean, e);
		}
	}

	private static Object getContentAsScalarValue(Object content) {
		Object value = null;

		if (content == null) {
			value = UberNode.NULL_VALUE;
		} else if (content instanceof String || content instanceof Number || content.equals(false) || content.equals(true)) {
			value = content;
		}
		return value;
	}

	/**
	 * Converts link to uber node.
	 *
	 * @param link to convert
	 * @return uber link
	 */
	public static UberNode toUberLink(Link link) {
		UberNode uberLink = new UberNode();
		uberLink.setRel(Arrays.asList(link.getRel()));
		uberLink.setUrl(getUrlProperty(link));
		uberLink.setModel(getModelProperty(link));
//		uberLink.setAction(UberAction.forRequestMethod(link.getRequestMethod()));
		if (true) throw new UnsupportedOperationException();
		return uberLink;
	}

	private static String getModelProperty(Link link) {
		if (true) throw new UnsupportedOperationException();
		RequestMethod httpMethod = RequestMethod.DELETE;// link.getRequestMethod();
		UriTemplate uriTemplate = new UriTemplate(link.getHref());
		final String model;
		switch (httpMethod) {
		case GET:
		case DELETE: {
			model = buildModel(uriTemplate.getVariables(), "{?", ",", "}", "%s");
			break;
		}
		case POST:
		case PUT:
		case PATCH: {
			model = buildModel(uriTemplate.getVariables(), "", "&", "", "%s={%s}");
			break;
		}
		default:
			model = null;
		}
		return StringUtils.isEmpty(model) ? null : model;
	}

	private static String getUrlProperty(Link link) {
		throw new UnsupportedOperationException();
//		return UriComponentsBuilder.fromUriString(link.getBaseUri()).build().normalize().toString();
	}

	private static String buildModel(List<TemplateVariable> variables, String prefix, String separator, String suffix,
			String parameterTemplate) {
		StringBuilder sb = new StringBuilder();
		for (TemplateVariable variable : variables) {
			if (sb.length() == 0) {
				sb.append(prefix);
			} else {
				sb.append(separator);
			}
			String parameterName = variable.getName();
			sb.append(String.format(parameterTemplate, parameterName, parameterName));

		}
		if (sb.length() > 0) {
			sb.append(suffix);
		}
		return sb.toString();
	}
}
