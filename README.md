# JaviGG — League of Legends Stats API

Spring Boot REST API that powers the JaviGG stats client. Wraps the Riot Games API to provide summoner profiles, match history, ranked standing, and live game data.

## Features

- **Summoner lookup** — resolves Riot ID → PUUID → summoner profile + ranked entry
- **Match history** — fetches the last 10 match IDs and retrieves all match details in parallel (finished matches are cached in memory)
- **Ranked standing** — solo/duo queue tier, LP, wins, and losses via League v4 API
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
riot.api.account.url=https://americas.api.riotgames.com
riot.api.match.url=https://americas.api.riotgames.com
riot.api.summoner.url=https://na1.api.riotgames.com
riot.api.spectator.url=https://na1.api.riotgames.com
allowed.origins=http://localhost:5173

# Optional
# riot.api.rate-limits=20:1,100:120     # requests:seconds windows; default matches a dev key
# riot.api.rate-limit-max-wait=10s      # fail with 503 instead of waiting longer than this
# riot.executor.threads=16
```

With a production key, set `riot.api.rate-limits` to the limits shown for your app on the Riot developer portal (e.g. `500:10,30000:600`).

### Run

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The server starts on `http://localhost:8080`.

## API Endpoints

### `GET /summoner`

Returns full summoner profile with match history and ranked data.

**Query params:** `gameName`, `tagLine`

**Response:**
```json
{
  "account":      { "puuid": "...", "gameName": "...", "tagLine": "..." },
  "summoner":     { "puuid": "...", "profileIconId": 123, "summonerLevel": 200 },
  "matchHistory": ["NA1_1234567890", ...],
  "matchDetails": [ ...10 full match objects... ],
  "rankedSolo":   { "tier": "GOLD", "rank": "II", "leaguePoints": 45, "wins": 75, "losses": 60 }
}
```

`rankedSolo` is `null` if the summoner is unranked.

### `GET /live-game`

Returns the current active game for a summoner.

**Query params:** `puuid`

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
    dto/             # Riot response DTOs (records)
  match/dto/
    IMatchDto        # Full match response DTO (participants, perks, etc.)
  config/            # CORS, Riot thread pool, global exception handler
```

## Notes

- Development Riot API keys expire every 24 hours — restart with a fresh key as needed
- Region is currently NA only, via the configured base URLs
- For production deployment, set `allowed.origins` to the deployed frontend URL(s), e.g. `https://www.javigg.com,https://javigg.com`
