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

/**
 * Demo package which defines Terms for all contained classes.
 */
@Terms({
        @Term(define = "gr", as = "http://purl.org/goodrelations/v1#"),
        @Term(define = "dc", as = "http://purl.org/dc/elements/1.1/"),
        @Term(define = "children", as = "http://example.com/vocab#parent", reverse = true)
})
package de.escalon.hypermedia.hydra.beans.withterms;

import de.escalon.hypermedia.hydra.mapping.Term;
import de.escalon.hypermedia.hydra.mapping.Terms;
