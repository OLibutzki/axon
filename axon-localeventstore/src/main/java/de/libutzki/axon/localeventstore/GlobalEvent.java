package de.libutzki.axon.localeventstore;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark events as global. Those events are copied into the global event store.
 */
@Retention( RUNTIME )
@Target( TYPE )
public @interface GlobalEvent {

}
