/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.hydra.serialize;

import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;

/**
 * Unwrapping variant of JacksonHydraSerializer, e.g. to support serialization of wrapper objects which
 * hold the actual bean as a member.
 * Created by dschulten on 16.09.2014.
 */
public class UnwrappingJacksonHydraSerializer extends JacksonHydraSerializer {

    /**
     * Creates unwrapping serializer from given serializer.
     * @param source to decorate.
     */
    UnwrappingJacksonHydraSerializer(BeanSerializerBase source) {
        super(source);
    }

    @Override
    public boolean isUnwrappingSerializer() {
        return true;
    }

}
