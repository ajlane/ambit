package au.id.ajlane.ambit.example;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import au.id.ajlane.ambit.Module;

/**
 * A module which configures the application with the standard configuration.
 */
@Module(Application.class)
class StandardModule
{
    private final String name;

    /**
     * Initialises the module.
     *
     * @param name
     *     The name that the application should use. Must not be null or empty.
     */
    public StandardModule(String name)
    {
        this.name = name;
    }

    /**
     * The message to be printed.
     *
     * @return A string.
     */
    public String message()
    {
        return "Hello " + name;
    }

    /**
     * The application's main thread pool.
     *
     * @return A ready executor service.
     */
    public ExecutorService threadPool()
    {
        return Executors.newCachedThreadPool();
    }
}