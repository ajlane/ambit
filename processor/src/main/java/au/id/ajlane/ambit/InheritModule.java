package au.id.ajlane.ambit;

/**
 * Marks a module which inherits any undefined factory methods from another module.
 */
public @interface InheritModule
{
    /**
     * A set of other modules to inherit from.
     *
     * @return An array of classes.
     */
    Class<?>[] value();
}
