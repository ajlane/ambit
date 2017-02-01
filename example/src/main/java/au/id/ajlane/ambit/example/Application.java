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
        try (Application application = new StandardApplication(args.length > 0 ? args[0] : "World"))
        {
            application.run();
        }
    }

    /**
     * The message that the application should print.
     *
     * @return A string.
     */
    String message();

    /**
     * The application's main thread pool.
     *
     * @return A ready executor service.
     */
    ExecutorService threadPool();

    /**
     * Runs the application.
     */
    default void run()
    {
        threadPool()
            .submit(() -> System.out.println(message()));
    }
}
