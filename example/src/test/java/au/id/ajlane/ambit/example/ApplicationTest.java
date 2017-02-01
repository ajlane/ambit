package au.id.ajlane.ambit.example;

import org.junit.Test;

public class ApplicationTest
{
    @Test
    public void test()
    {
        try (TestApplication application = new TestApplication())
        {
            application.run();
        }
    }
}
