package dev.javis.javigg.service;

import dev.javis.javigg.match.dto.IMatchDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TagService {

    public List<String> generateTags(String puuid, List<IMatchDto.MatchDto> matches) {
        List<String> tags = new ArrayList<>();

        try {
            List<IMatchDto.ParticipantDto> myStats = matches.stream()
                    .map(m -> m.info().participants().stream()
                            .filter(p -> puuid.equals(p.puuid()))
                            .findFirst()
                            .orElse(null))
                    .filter(p -> p != null)
                    .collect(Collectors.toList());

            if (myStats.isEmpty()) return tags;

            // OTP: same champion in 3+ of last 5 games
            Map<Integer, Long> champFreq = myStats.stream()
                    .collect(Collectors.groupingBy(IMatchDto.ParticipantDto::championId, Collectors.counting()));
            boolean isOtp = champFreq.values().stream().anyMatch(count -> count >= 3);
            if (isOtp) tags.add("OTP");

            double avgKills = myStats.stream().mapToInt(IMatchDto.ParticipantDto::kills).average().orElse(0);
            double avgDeaths = myStats.stream().mapToInt(IMatchDto.ParticipantDto::deaths).average().orElse(0);
            double avgVisionScore = myStats.stream().mapToInt(IMatchDto.ParticipantDto::visionScore).average().orElse(0);

            // HARD CARRY: consistently high kills, low deaths
            if (avgKills >= 7 && avgDeaths <= 3) tags.add("HARD CARRY");

            // AGGRESSIVE: high kills but also dying a lot
            if (avgKills >= 5 && avgDeaths >= 5) tags.add("AGGRESSIVE");

            // VISION FOCUSED: support-style high vision score
            if (avgVisionScore >= 35) tags.add("VISION FOCUSED");

        } catch (Exception e) {
            // fail silently — tags are best-effort
        }

        return tags;
    }
}
