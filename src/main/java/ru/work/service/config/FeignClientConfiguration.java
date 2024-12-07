package ru.work.service.config;

import feign.Logger;
import feign.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignFormatterRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.format.Formatter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class FeignClientConfiguration {

    @Bean
    public Request.Options options(@Value("${rest.default.connect-timeout:10}") int connectTimeout,
                                   @Value("${rest.default.read-timeout:10}") int readTimeout) {
        return new Request.Options(
                connectTimeout, TimeUnit.SECONDS,
                readTimeout, TimeUnit.SECONDS,
                true);
    }

    @Bean
    Logger.Level level(@Value("${rest.default.logger-level:BASIC}") Logger.Level level) {
        return level;
    }

    @Bean
    public FeignFormatterRegistrar localDateFeignFormatterRegistrar() {
        return formatterRegistry -> formatterRegistry.addFormatter(
                new Formatter<LocalDate>() {
                    @Override
                    public String print(LocalDate object, Locale locale) {
                        return object.format(DateTimeFormatter.ISO_LOCAL_DATE);
                    }

                    @Override
                    public LocalDate parse(String text, Locale locale) {
                        return LocalDate.parse(text);
                    }
                }
        );
    }

}
