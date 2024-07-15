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

    
    @Value("${external.profileicon.url}")
    private String externalProfileIconUrl;

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

    public String getProfileIconUrl(String profileIconId) {
        // System.out.println(profileIconData.get("data"));
        // System.out.println("iconId: " + profileIconId);
        // // logger.info("Fetching profile icon for ID: " + profileIconId);
        // Map<String, Object> data = (Map<String, Object>) profileIconData.get("data");

        // if (data != null) {
        //     Map<String, Object> iconData = (Map<String, Object>) data.get(profileIconId);
        //     System.out.println(iconData);
        //     if (iconData != null) {
        //         Map<String, String> image = (Map<String, String>) iconData.get("image");
        //         // System.out.println("image: " + image);
        //         if (image != null) {
        //             String imageUrl = "../../img/profileicon/" + image.get("full");
        //             return imageUrl;
        //         }
        //     }
        // }
        // logger.warning("Profile icon not found for ID: " + profileIconId);
        return externalProfileIconUrl + profileIconId + ".png";
        // return null;
    }

    public Map<String, Object> getProfileIconData() {
        return profileIconData;
    }

    public Map<String, Object> getChampData() {
        return champData;
    }

}