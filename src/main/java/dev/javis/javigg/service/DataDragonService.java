package dev.javis.javigg.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class DataDragonService {

    @Value("classpath:datadragon/data/en_US/champion.json")
    private Resource champResource; 

    @Value("classpath:datadragon/data/en_US/profileicon.json")
    private Resource profileIconResource;

    private final ObjectMapper objectMapper;

    private Map<String, Object> champData;
    private Map<String, Object> profileIconData;

    public DataDragonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            loadChampData();
            loadProfileIconData();
        } catch (IOException e) {
            // Log the error and handle it appropriately
            e.printStackTrace();
        }
    }

    public void loadChampData() throws IOException {
        this.champData = objectMapper.readValue(champResource.getInputStream(), Map.class);
    }

    private void loadProfileIconData() throws IOException {
        this.profileIconData = objectMapper.readValue(profileIconResource.getInputStream(), Map.class);
    }

    public String getProfileIconUrl(int profileIconId) {
        System.out.println(profileIconData.get("data"));
        System.out.println("iconId: " + profileIconId);
        // logger.info("Fetching profile icon for ID: " + profileIconId);
        Map<String, Object> data = (Map<String, Object>) profileIconData.get("data");

        if (data != null) {
            System.out.println(String.valueOf(profileIconId));
            Map<String, Object> iconData = (Map<String, Object>) data.get(profileIconId);
            if (iconData != null) {
                Map<String, String> image = (Map<String, String>) iconData.get("image");
                if (image != null) {
                    String imageUrl = "/datadragon/images/profileicon/" + image.get("full");
                    // logger.info("Profile icon URL: " + imageUrl);
                    return imageUrl;
                }
            }
        }
        // logger.warning("Profile icon not found for ID: " + profileIconId);
        return null;
    }

    public Map<String, Object> getProfileIconData() {
        return profileIconData;
    }

    public Map<String, Object> getChampData() {
        return champData;
    }

}