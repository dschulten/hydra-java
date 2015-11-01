package de.escalon.hypermedia.hydra.serialize;

/**
 * Created by Dietrich on 19.10.2015.
 */
public interface ProxyUnwrapper {

    /**
     * Unwraps target object of the given proxy, if available. If the given instance is not a proxy, it will be returned
     * as-is.
     *
     * @param possibleProxy
     *         to unwrap
     * @return target object or null if there is no underlying target.
     */
    Object unwrapProxy(Object possibleProxy);
}
