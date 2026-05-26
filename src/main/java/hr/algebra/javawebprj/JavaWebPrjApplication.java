package hr.algebra.javawebprj;

import hr.algebra.javawebprj.config.PayPalProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PayPalProperties.class)
public class JavaWebPrjApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaWebPrjApplication.class, args);
    }

}
