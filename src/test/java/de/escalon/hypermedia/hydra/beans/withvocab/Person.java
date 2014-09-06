package de.escalon.hypermedia.hydra.beans.withvocab;

import de.escalon.hypermedia.hydra.mapping.Expose;

/**
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
