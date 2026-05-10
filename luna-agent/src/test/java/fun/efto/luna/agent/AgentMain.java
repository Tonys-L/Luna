package fun.efto.luna.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.jar.JarFile;

/**
 * Main class for testing the Luna agent
 */
public class AgentMain {
    public static void main(String[] args) {
        try {
            System.out.println("Starting Luna agent test...");
            
            // Call the main method of Agent class directly
            Agent.main(new String[0]);
            
            System.out.println("Luna agent started successfully!");
            
            // Keep the program running
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}