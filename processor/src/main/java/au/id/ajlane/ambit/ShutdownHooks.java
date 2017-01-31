package au.id.ajlane.ambit;

import java.util.function.Consumer;

/**
 * A utility for working with shutdown hooks.
 */
public abstract class ShutdownHooks
{
    /**
     * Adds a shutdown hook to the current runtime.
     *
     * @param task
     *     The task to perform when the runtime shuts down.
     * @param handler
     *     A handler for any failures in the shutdown task.
     */
    public static void add(AutoCloseable task, Consumer<Throwable> handler)
    {
        Runtime.getRuntime()
            .addShutdownHook(new Thread(() ->
            {
                try
                {
                    task.close();
                }
                catch (Throwable ex)
                {
                    handler.accept(ex);
                }
            }));
    }

    private ShutdownHooks()
    {
    }
}
