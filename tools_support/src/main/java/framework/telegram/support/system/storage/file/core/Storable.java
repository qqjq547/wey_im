package framework.telegram.support.system.storage.file.core;

/**
 * As it sounds, anything that can stored and represented as byte array.
 */
public interface Storable {
    byte[] getBytes();
}
