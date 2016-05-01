/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the License.
 */
package de.escalon.hypermedia.hydra.mapping;

import java.lang.annotation.*;

/**
 * The Expose annotation allows to make the annotated item known under an alias which is meaningful in a vocabulary.
 *
 * Created by dschulten on 10.08.2014.
 */
@Documented
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Expose {

    /**
     * Iri describing the attribute, type or method parameter e.g. http://schema.org/name or http://schema.org/Person.
     * Can also be a compact iri like foaf:name or a term from the current vocab (e.g. just name if @vocab is
     * schema.org).
     *
     * @return iri
     */
    String value();

}
