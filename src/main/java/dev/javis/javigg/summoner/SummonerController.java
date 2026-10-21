package dev.javis.javigg.summoner;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.javis.javigg.service.ProfileService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
public class SummonerController {

    private final ProfileService profileService;

    public SummonerController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // Riot ID limits: gameName 3-16 characters, tagLine 3-5.
    @GetMapping("/summoner")
    public SummonerProfileDto getSummoner(
            @RequestParam @NotBlank @Size(max = 16) String gameName,
            @RequestParam @NotBlank @Size(max = 5) String tagLine
    ) {
        return profileService.getProfile(gameName.trim(), tagLine.trim());
    }
}
