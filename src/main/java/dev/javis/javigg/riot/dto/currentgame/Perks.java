package dev.javis.javigg.riot.dto.currentgame;

import java.util.List;

public record Perks(
        List<Long> perkIds,
        long perkStyle,
        long perkSubStyle
) {}