package dev.javis.javigg.riot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegionTest {

    @Test
    void platformHostIsTheRegionsOwnHost() {
        assertThat(Region.EUW1.platformHost()).isEqualTo("https://euw1.api.riotgames.com");
        assertThat(Region.KR.platformHost()).isEqualTo("https://kr.api.riotgames.com");
    }

    @Test
    void accountMatchHostUsesTheRegionalCluster() {
        assertThat(Region.NA1.accountMatchHost()).isEqualTo("https://americas.api.riotgames.com");
        assertThat(Region.BR1.accountMatchHost()).isEqualTo("https://americas.api.riotgames.com");
        assertThat(Region.EUW1.accountMatchHost()).isEqualTo("https://europe.api.riotgames.com");
        assertThat(Region.EUN1.accountMatchHost()).isEqualTo("https://europe.api.riotgames.com");
        assertThat(Region.KR.accountMatchHost()).isEqualTo("https://asia.api.riotgames.com");
        assertThat(Region.JP1.accountMatchHost()).isEqualTo("https://asia.api.riotgames.com");
    }

    @Test
    void seaPlatformsRouteAccountAndMatchCallsThroughAsia() {
        // Account v1 and Match v5 predate the "sea" regional cluster and never gained a host for
        // it, so SEA platforms (oc1, sg2, tw2, vn2, ph2, th2) have to use asia instead.
        assertThat(Region.OC1.accountMatchHost()).isEqualTo("https://asia.api.riotgames.com");
        assertThat(Region.SG2.accountMatchHost()).isEqualTo("https://asia.api.riotgames.com");
        assertThat(Region.TH2.accountMatchHost()).isEqualTo("https://asia.api.riotgames.com");

        // But they still get their own platform host for platform-scoped APIs.
        assertThat(Region.OC1.platformHost()).isEqualTo("https://oc1.api.riotgames.com");
    }

    @Test
    void fromRequestParamIsCaseInsensitive() {
        RegionConverter converter = new RegionConverter();
        assertThat(converter.convert("na1")).isEqualTo(Region.NA1);
        assertThat(converter.convert("Euw1")).isEqualTo(Region.EUW1);
        assertThat(converter.convert(" kr ")).isEqualTo(Region.KR);
    }

    @Test
    void fromRequestParamRejectsUnknownCodes() {
        RegionConverter converter = new RegionConverter();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> converter.convert("mars1"));
    }
}
