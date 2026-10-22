package dev.javis.javigg.riot;

/**
 * A Riot platform routing value (one per game server, e.g. {@code na1}, {@code euw1}). Every
 * request to Riot needs a host built from one of these, so this is the one place that knows both
 * the platform hosts and which regional cluster each platform belongs to.
 */
public enum Region {

    NA1(Routing.AMERICAS),
    BR1(Routing.AMERICAS),
    LA1(Routing.AMERICAS),
    LA2(Routing.AMERICAS),

    KR(Routing.ASIA),
    JP1(Routing.ASIA),

    EUW1(Routing.EUROPE),
    EUN1(Routing.EUROPE),
    TR1(Routing.EUROPE),
    RU(Routing.EUROPE),

    OC1(Routing.SEA),
    SG2(Routing.SEA),
    TW2(Routing.SEA),
    VN2(Routing.SEA),
    PH2(Routing.SEA),
    TH2(Routing.SEA);

    private final Routing routing;

    Region(Routing routing) {
        this.routing = routing;
    }

    /** Host for platform-scoped APIs: Summoner v4, League v4, Spectator v5. */
    public String platformHost() {
        return "https://" + name().toLowerCase() + ".api.riotgames.com";
    }

    /**
     * Host for regionally-scoped APIs: Account v1, Match v5. Both predate the {@code sea} routing
     * value and never got one added, so SEA platforms route through {@code asia} instead.
     */
    public String accountMatchHost() {
        Routing effective = routing == Routing.SEA ? Routing.ASIA : routing;
        return "https://" + effective.name().toLowerCase() + ".api.riotgames.com";
    }

    private enum Routing { AMERICAS, ASIA, EUROPE, SEA }
}
