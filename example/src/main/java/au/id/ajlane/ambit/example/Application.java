package au.id.ajlane.ambit.example;

import java.util.concurrent.ExecutorService;

/**
 * An application.
 */
public interface Application
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
        Application application = new StandardApplication();
    }

    /**
     * The application's main thread pool.
     *
     * @return A ready executor service.
     */
    ExecutorService threadPool();
}
