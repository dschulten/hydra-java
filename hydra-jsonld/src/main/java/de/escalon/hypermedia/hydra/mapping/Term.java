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
 * Defines a single term. If you need to define several terms, use @Terms.
 */
@Documented
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Term {
    /**
     * Term, e.g. a shorthand for a vocabulary to be used in compact uris, like foaf, or a manual term definition such
     * as 'fullName'.
     *
     * @return term
     */
    String define();

    /**
     * What the term stands for, e.g. a vocabulary uri like http://xmlns.com/foaf/0.1/ could stand for foaf. an uri for
     * a property like http://xmlns.com/foaf/0.1/name could stand for fullName.
     *
     * @return definition of term
     */
    String as();

    /**
     * Whether the term is a reversed property. Default is false.
     *
     * @return true if reversed, false otherwise
     */
    boolean reverse() default false;
}
