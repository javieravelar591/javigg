package dev.javis.javigg.riot;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/** Lets {@code @RequestParam Region} bind case-insensitively; Spring Boot registers this automatically. */
@Component
public class RegionConverter implements Converter<String, Region> {

    @Override
    public Region convert(String source) {
        return Region.valueOf(source.trim().toUpperCase());
    }
}
