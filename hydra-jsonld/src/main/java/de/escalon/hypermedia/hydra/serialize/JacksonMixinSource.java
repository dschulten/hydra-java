package de.escalon.hypermedia.hydra.serialize;

import com.fasterxml.jackson.databind.SerializationConfig;

/**
 * Created by Dietrich on 05.04.2015.
 */
public class JacksonMixinSource implements MixinSource {

    private SerializationConfig serializationConfig;

    public JacksonMixinSource(SerializationConfig serializationConfig) {
        this.serializationConfig = serializationConfig;
    }

    @Override
    public Class<?> findMixInClassFor(Class<?> clazz) {
        return serializationConfig.findMixInClassFor(clazz);
    }
}
