package au.id.ajlane.ambit.example;

import org.junit.Test;

public class ApplicationTest
{
    @Test
    public void test()
    {
        try (Application application = new TestApplication())
        {
            application.run();
        }
    }
}
