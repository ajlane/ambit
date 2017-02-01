package au.id.ajlane.ambit.example;

import java.util.concurrent.ExecutorService;

/**
 * An application.
 */
public interface Application extends AutoCloseable
{
    /**
     * An entry point to the application.
     *
     * @param args
     *     Command line arguments.
     *
     * @throws Exception
     *     If the application could not be started.
     */
    static void main(final String... args) throws Exception
    {
        try (Application application = new StandardApplication())
        {
            application.run();
        }
    }

    @Override
    default void close()
    {
        threadPool().shutdownNow();
        System.out.println("Done.");
    }

    /**
     * The message that the application should print.
     *
     * @return A string.
     */
    String message();

    /**
     * Runs the application.
     */
    default void run()
    {
        threadPool()
            .submit(() -> System.out.println(message()));
    }

    /**
     * The application's main thread pool.
     *
     * @return A ready executor service.
     */
    ExecutorService threadPool();
}
