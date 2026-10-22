# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Spring Boot 3.2.5 / Java 17 REST API backing JaviGG, a League of Legends stats site. It wraps the Riot Games API. The React frontend lives in the sibling repo `../javigg-client`, which calls this API's `/summoner` and `/live-game` endpoints.

## Commands

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local   # run on :8080
./mvnw clean package                                       # build jar (runs tests)
./mvnw test                                                # all tests
./mvnw test -Dtest=RiotApiClientTest                       # single class
./mvnw test -Dtest=RiotApiClientTest#retriesAfter429ThenSucceeds   # single method
```

## Configuration

`application.properties` and `application-local.properties` are gitignored, so they never show up in the repo; the README lists the required keys. `src/test/resources/application.properties` holds dummy values so tests run without a key. That file is explicitly un-ignored in `.gitignore`.

Code reads these properties through `@Value`:

- `riot.api.key`
- `allowed.origins`: comma-separated; read by `config/CorsConfig`, which allows GET only
- Optional: `riot.api.rate-limits` (default `20:1,100:120`, the dev-key limits), `riot.api.rate-limit-max-wait`, `riot.executor.threads`

Riot hosts are not configuration — they're derived per-request from a `Region` (see Architecture below), so there's no `riot.api.*.url` property anymore.

## Architecture

There is no database. Every request fans out to Riot, so Riot's rate limits are the main scaling constraint.

- **`riot/Region`** is a platform routing value (`NA1`, `EUW1`, `KR`, ...). It knows its own `platformHost()` (Summoner v4, League v4, Spectator v5) and `accountMatchHost()` (Account v1, Match v5 — these two predate the `sea` regional cluster, so `OC1`/`SG2`/`TW2`/`VN2`/`PH2`/`TH2` route through `asia` instead of a nonexistent `sea` host). `RegionConverter` binds the `region` request param case-insensitively; an unrecognized code fails binding and `ApiExceptionHandler` turns that into a 400. Every service method that calls Riot takes a `Region` and builds its host from it — there is no stored/injected base URL anywhere.
- **`riot/RiotApiClient`** is the only class that makes HTTP calls. Services call `get(baseUrl, pathTemplate, Type.class, vars...)`, and the client:
  - sends the key in the `X-Riot-Token` header, never in the URL
  - strictly encodes URI variables, so user input can't escape its path segment
  - acquires a slot from `RiotRateLimiter`, which keeps sliding windows per host because Riot limits per routing value
  - retries 429s that carry a short `Retry-After`
  - turns every failure into a `RiotApiException` carrying an HTTP status

  Never create a `RestTemplate` anywhere else.
- **Services** (`service/`) come in two kinds, following the separation from GitHub issue #3:
  - one per Riot API, each owning its endpoint paths: `AccountService`, `SummonerService`, `LeagueService`, `MatchService`, `SpectatorService`
  - aggregators that call only other services: `ProfileService` (behind `/summoner`), `LiveGameService` (behind `/live-game`), and the pure-logic `StreakService`, `TagService`, `PlayerStatsService`
- **Ranked data.** `LeagueService.getRankedEntries` fetches league-v4 once and returns both solo (`RANKED_SOLO_5x5`) and flex (`RANKED_FLEX_SR`) entries from the same response — there's no separate "get flex" call. Riot's API only exposes the *current* season's standing per queue; there's no history endpoint, so past-season ranks aren't available without javigg standing up its own persistence to snapshot them over time (tracked separately, not implemented here).
- **Player stats.** `PlayerStatsService` is pure logic (no Riot calls): it aggregates the already-fetched `matchDetails` into `championBreakdown` (top 5 most-played champions by games, with win rate) and `roleBreakdown` (grouped by Riot's `teamPosition` field). Both only cover as many recent games as `ProfileService.MATCH_HISTORY_COUNT` fetches (currently 20) — they are not full-career stats.
- **Concurrency.** Parallel calls run on the bounded `riotExecutor` pool (`config/RiotExecutorConfig`), never the common pool. Compose futures with `thenCompose`/`thenCombine` instead of calling `join()` inside a pool task, because a bounded pool can deadlock when tasks wait on their own subtasks. `LiveGameService` is the example to follow.
- **Caching.** `MatchService` caches match details in Caffeine; finished matches are immutable. Nothing else is cached.
- **Errors.** `config/ApiExceptionHandler` maps failures to ProblemDetail responses:
  - Riot 404 → 404
  - `NotInGameException` → 404
  - 429 or exhausted local limit → 503 with `Retry-After`
  - validation failures → 400
  - any other Riot failure → 502
- **Validation.** Request params are validated with Jakarta annotations directly on controller method params. This relies on Spring 6.1's built-in method validation, so no `@Validated` is needed.
- **Response shapes.** `/summoner` returns `summoner/SummonerProfileDto`. The client's hand-written TypeScript interfaces in `javigg-client/src/App.tsx` mirror this record and `LiveGameLobbyDto`, so keep both sides in sync.
- **DTOs** are Java records. The full Match v5 payload lives in `match/dto/IMatchDto`, and `MatchDto.participant(puuid)` finds one player's stats.
- **Region.** Both `/summoner` and `/live-game` take an optional `region` query param (defaults to `NA1`); see `riot/Region` above. `MatchService`'s cache is keyed by match ID alone — Riot match IDs are prefixed with their originating platform (e.g. `NA1_...`), so they're globally unique and safe to cache without the region.
