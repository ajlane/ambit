package au.id.ajlane.ambit;

/**
 * Marks an abstract class as a module.
 */
public @interface Module
{
    /**
     * The set of scope interfaces that the module services.
     *
     * @return An array of classes.
     */
    Class<?>[] value();
}
