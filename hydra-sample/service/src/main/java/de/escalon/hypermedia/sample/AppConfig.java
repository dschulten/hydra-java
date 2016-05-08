package de.escalon.hypermedia.sample;

import de.escalon.hypermedia.spring.halforms.HalFormsForwardingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by Dietrich on 07.05.2016.
 */
@Configuration
public class AppConfig {

    @Bean
    public HalFormsForwardingFilter halFormsForwardingFilter() {
        return new HalFormsForwardingFilter();
    }
}
