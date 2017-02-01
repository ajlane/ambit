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
    /**
     * The message to be printed.
     *
     * @return A string.
     */
    public String message()
    {
        return "Hello World";
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