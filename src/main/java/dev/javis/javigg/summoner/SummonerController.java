package dev.javis.javigg.summoner;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.javis.javigg.riot.Region;
import dev.javis.javigg.service.ProfileService;
import jakarta.validation.constraints.Pattern;

@RestController
public class SummonerController {

    private final ProfileService profileService;

    public SummonerController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // Riot IDs: gameName is 3-16 letters, digits or spaces; tagLine is 3-5 letters or digits.
    // Rejecting anything else here also keeps junk paths from reaching Riot, which answers
    // unknown paths with a misleading 403.
    @GetMapping("/summoner")
    public SummonerProfileDto getSummoner(
            @RequestParam @Pattern(regexp = "\\s*[\\p{L}\\p{N}][\\p{L}\\p{N} ]{0,15}\\s*") String gameName,
            @RequestParam @Pattern(regexp = "\\s*[\\p{L}\\p{N}]{2,5}\\s*") String tagLine,
            @RequestParam(defaultValue = "NA1") Region region
    ) {
        return profileService.getProfile(gameName.trim(), tagLine.trim(), region);
    }
}
