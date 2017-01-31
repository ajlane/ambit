package au.id.ajlane.ambit.example;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import au.id.ajlane.ambit.Module;

/**
 * A module which configures the application with the standard configuration.
 */
@Module(Application.class)
abstract class StandardModule
{
    /**
     * The application's main thread pool.
     *
     * @return A ready executor service.
     */
    ExecutorService threadPool()
    {
        return Executors.newCachedThreadPool();
    }
}