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

package de.escalon.hypermedia.spring.sample.test;

import de.escalon.hypermedia.hydra.mapping.Expose;

/**
 * Sample Event derived from RepresentationModel. Created by dschulten on 13.09.2014.
 */
@Expose("Event")
public class EventResourceExposed extends EventResource {

    public EventResourceExposed(int id, String performer, String name, String location) {
        super(id, performer, name, location);
    }
}
