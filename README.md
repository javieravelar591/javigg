# JaviGG — League of Legends Stats API

Spring Boot REST API that powers the JaviGG stats client. Wraps the Riot Games API to provide summoner profiles, match history, ranked standing, and live game data.

## Features

- **Summoner lookup** — resolves Riot ID → PUUID → summoner profile + ranked entry
- **Match history** — fetches the last 20 match IDs and retrieves all match details in parallel (finished matches are cached in memory)
- **Ranked standing** — current-season solo and flex queue tier, LP, wins, and losses via League v4 API
- **Champion & role breakdown** — top 5 most-played champions and per-role win rates, computed from the fetched match history
- **Live game** — active game info with per-player streak calculation (HOT/COLD/NEUTRAL based on last 3 results)
- **Parallel fetching** — independent Riot calls run concurrently on a bounded thread pool
- **Rate limiting** — client-side enforcement of Riot's app rate limits per routing host, plus automatic retry on 429 using `Retry-After`

## Tech Stack

- Java 17
- Spring Boot 3.2.5
- Riot Games API (Account v1, Summoner v4, Match v5, League v4, Spectator v5)

## Getting Started

### Prerequisites

- Java 17+
- Maven wrapper included (`./mvnw`)
- A [Riot Games API key](https://developer.riotgames.com/) (development keys refresh every 24 hours)

### Configuration

Create `application-local.properties` in the project root (already in `.gitignore`):

```properties
riot.api.key=RGAPI-your-key-here
allowed.origins=http://localhost:5173

# Optional
# riot.api.rate-limits=20:1,100:120     # requests:seconds windows; default matches a dev key
# riot.api.rate-limit-max-wait=10s      # fail with 503 instead of waiting longer than this
# riot.executor.threads=16
```

Riot hosts are no longer configured — they're derived per-request from the `region` query param (see below), so there's nothing to set here for them.

With a production key, set `riot.api.rate-limits` to the limits shown for your app on the Riot developer portal (e.g. `500:10,30000:600`).

### Run

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The server starts on `http://localhost:8080`.

## API Endpoints

### `GET /summoner`

Returns full summoner profile with match history and ranked data.

**Query params:** `gameName`, `tagLine`, `region` (optional, defaults to `NA1`)

`region` is a Riot platform routing value: `NA1`, `BR1`, `LA1`, `LA2`, `EUW1`, `EUN1`, `TR1`, `RU`, `KR`, `JP1`, `OC1`, `SG2`, `TW2`, `VN2`, `PH2`, `TH2`. It's case-insensitive; an unrecognized value is a 400.

**Response:**
```json
{
  "account":       { "puuid": "...", "gameName": "...", "tagLine": "..." },
  "summoner":      { "puuid": "...", "profileIconId": 123, "summonerLevel": 200 },
  "matchHistory":  ["NA1_1234567890", ...],
  "matchDetails":  [ ...20 full match objects... ],
  "rankedSolo":    { "tier": "GOLD", "rank": "II", "leaguePoints": 45, "wins": 75, "losses": 60 },
  "rankedFlex":    { "tier": "SILVER", "rank": "I", "leaguePoints": 12, "wins": 30, "losses": 28 },
  "championStats": [
    { "championId": 157, "championName": "Yasuo", "games": 9, "wins": 6, "losses": 3, "winRate": 66.7 }
  ],
  "roleStats": [
    { "role": "MIDDLE", "games": 14, "wins": 8, "losses": 6, "winRate": 57.1 }
  ]
}
```

`rankedSolo`/`rankedFlex` are each `null` if the summoner is unranked in that queue — these are Riot's *current*-season standings only, there's no API for past seasons. `championStats` is capped to the top 5 most-played champions; `championStats`/`roleStats` are both computed from `matchDetails`, so they only reflect as many recent games as `matchHistory` includes.

### `GET /live-game`

Returns the current active game for a summoner.

**Query params:** `puuid`, `region` (optional, defaults to `NA1`; same values as `/summoner`)

**Response:**
```json
{
  "gameId": 1234567890,
  "gameMode": "CLASSIC",
  "players": [
    {
      "puuid": "...",
      "summonerName": "PlayerName",
      "championId": 157,
      "profileIconId": 4321,
      "teamId": 100,
      "streak": { "winStreak": 3, "lossStreak": 0, "status": "HOT" }
    }
  ]
}
```

### Errors

Errors are returned as RFC 7807 `application/problem+json`:

| Status | When |
|--------|------|
| 400 | Missing or malformed query params |
| 404 | Riot ID not found, or (for `/live-game`) the player is not in a game |
| 502 | Riot API failed or rejected the key |
| 503 | Rate limit reached; includes a `Retry-After` header |

## Project Structure

```
src/main/java/dev/javis/javigg/
  summoner/          # GET /summoner controller + response record
  livegame/          # GET /live-game controller
  service/           # One service per Riot API (Account, Summoner, League, Match, Spectator)
                     #   plus aggregators: ProfileService, LiveGameService, StreakService, TagService
  riot/
    RiotApiClient    # The only HTTP client: auth header, URL encoding, rate limiting, 429 retries
    RiotRateLimiter  # Per-host sliding-window limiter
    Region           # Platform routing values and their regional (account/match) hosts
    dto/             # Riot response DTOs (records)
  match/dto/
    IMatchDto        # Full match response DTO (participants, perks, etc.)
  config/            # CORS, Riot thread pool, global exception handler
```

## Notes

- Development Riot API keys expire every 24 hours — restart with a fresh key as needed
- All Riot platform regions are supported via the `region` query param; see `riot/Region.java` for the full list and how platform codes map to Account v1/Match v5 regional hosts
- For production deployment, set `allowed.origins` to the deployed frontend URL(s), e.g. `https://www.javigg.com,https://javigg.com`
