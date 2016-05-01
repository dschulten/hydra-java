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

package de.escalon.hypermedia.hydra.beans.withvocab;

import de.escalon.hypermedia.hydra.mapping.Expose;

/**
 * Person class in a package with wocab definition in package-info.java.
 *
 * Created by dschulten on 11.08.2014.
 */
public class Person {
    public String birthDate;

    @Expose("http://schema.org/familyName")
    public String surname;

    public Person(String birthDate, String surname) {
        this.birthDate = birthDate;
        this.surname = surname;
    }
}
