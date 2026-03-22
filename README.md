# JaviGG — League of Legends Stats API

Spring Boot REST API that powers the JaviGG stats client. Wraps the Riot Games API to provide summoner profiles, match history, ranked standing, and live game data.

## Features

- **Summoner lookup** — resolves Riot ID → PUUID → summoner profile + ranked entry
- **Match history** — fetches last 20 match IDs and retrieves all match details in parallel
- **Ranked standing** — solo/duo queue tier, LP, wins, and losses via League v4 API
- **Live game** — active game info with per-player streak calculation (HOT/COLD/NEUTRAL based on last 3 results)
- **Parallel fetching** — match details and streak lookups use `CompletableFuture` to run concurrently

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
```

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
  "matchDetails": [ ...20 full match objects... ],
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

Returns a 500 if the summoner is not currently in a game.

## Project Structure

```
src/main/java/dev/javis/javigg/
  summoner/          # SummonerController — main profile endpoint
  service/
    SummonerService  # Account + summoner + ranked API calls
    MatchService     # Match history + parallel detail fetching
    LiveGameService  # Active game + per-player streak resolution
    LiveGameController
    StreakService    # HOT/COLD/NEUTRAL from last 3 match results
  riot/
    RiotApiClient    # Low-level HTTP wrappers for Riot endpoints
    dto/             # API response DTOs (Account, Summoner, LeagueEntry, etc.)
  match/dto/
    IMatchDto        # Full match response DTO (participants, perks, etc.)
  datadragon/
    DataDragonService  # Profile icon URL resolution
```

## Notes

- Development Riot API keys expire every 24 hours — restart with a fresh key as needed
- Region is currently hardcoded to NA (`na1.api.riotgames.com` / `americas.api.riotgames.com`)
- For production deployment, `@CrossOrigin` in `LiveGameController` should be updated to the deployed frontend URL
