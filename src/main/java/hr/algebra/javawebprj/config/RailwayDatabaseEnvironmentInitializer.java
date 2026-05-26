package hr.algebra.javawebprj.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Backup path if {@link RailwayDatabaseEnvironmentPostProcessor} is not loaded from META-INF.
 */
public class RailwayDatabaseEnvironmentInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        RailwayDatabaseConfigSupport.applyIfNeeded(environment);
    }
}
