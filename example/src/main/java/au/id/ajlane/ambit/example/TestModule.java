package au.id.ajlane.ambit.example;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import au.id.ajlane.ambit.InheritModule;
import au.id.ajlane.ambit.Module;

/**
 * A module which configures the application with a transient test configuration.
 */
@Module(Application.class)
@InheritModule(StandardModule.class)
class TestModule
{
    /**
     * The message to be printed.
     *
     * @return A string.
     */
    public String message()
    {
        return "This is a test.";
    }

    /**
     * The application's main thread pool.
     *
     * @return A ready executor service.
     */
    public ExecutorService threadPool()
    {
        return Executors.newSingleThreadExecutor();
    }
}