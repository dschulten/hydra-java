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
 * Allows to determine possible values for an argument annotated with {@link Select}.
 * 
 * @author Dietrich Schulten
 * 
 */
public interface Options {

	/**
	 * Gets possible values for an argument annotated with {@link Select}. The default implementation
	 * {@link StringOptions} just passes on a given String array as possible values. Sometimes the possible values are
	 * more dynamic and depend on the context. Therefore, an Options implementation can also determine possible values
	 * based on another argument to the same call. The example below shows how to get possible options from a
	 * DetailOptions implementation which needs a personId for that:
	 * 
	 * <pre>
	 * &#064;RequestMapping(value = &quot;/customer/{personId}/details&quot;, 
	 *     params = { &quot;detail&quot; })
	 * public HttpEntity&lt;Resource&lt;List&lt;String&gt;&gt; showDetails(
	 *     &#064;PathVariable Long personId,
	 * 		 &#064;RequestParam(&quot;detail&quot;) 
	 *     &#064;Select(options = DetailOptions.class, args = &quot;personId&quot;) 
	 *         List&lt;String&gt; detail) {
	 *    ...
	 * }
	 * </pre>
	 * 
	 * The &#064;Select annotation above says that the possible detail values come from a DetailOptions bean which
	 * determines those values based on the personId. Note how the personId is passed to showDetails when creating an
	 * ActionDescriptor:
	 * 
	 * <pre>
	 * &#064;RequestMapping(&quot;/customer/{personId}/details&quot;)
	 * public HttpEntity&lt;ActionDescriptor&gt; showDetailsAction(@PathVariable(&quot;personId&quot;) Long personId) {
	 * 	ActionDescriptor action = actionFor(methodOn(this.getClass()).showDetails(personId, null), &quot;showDetails&quot;);
	 * 	return new HttpEntity&lt;ActionDescriptor&gt;(action);
	 * }
	 * </pre>
	 * 
	 * Within the call to {@link Options#get} the args array contains the values specified by the args annotation
	 * attribute in the given order. In the example above, DetailOptions receives the personId and can read possible
	 * options for that particular person.
	 * 
	 * @param value parameters to be used by the implementation. Could be literal values as used by {@link StringOptions}
	 *          or some argument to a custom implementation of Options, such as an SQL string.
	 * @param args from the method call as defined by {@link Select#args()}. The possible values for a parameter might
	 *          depend on the context. In that case, you can use {@link Select#args()} to pass other argument values. See
	 *          above for an example.
	 * @return
	 * @see StringOptions
	 */
	public Object[] get(String[] value, Object... args);

}