package com.lucyplugin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit test for LucyPlugin.
 */
public class AppTest {

    /**
     * Test that the plugin class exists.
     */
    @Test
    public void testPluginClassExists() {
        // Just verify the class exists and can be referenced
        Class<?> pluginClass = MyPlugin.class;
        assertTrue(pluginClass != null);
        assertTrue(pluginClass.getSimpleName().equals("MyPlugin"));
    }

    /**
     * Basic test to ensure JUnit is working.
     */
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }
}
