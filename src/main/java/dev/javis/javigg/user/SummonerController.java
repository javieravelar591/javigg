package dev.javis.javigg.user;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SummonerController {
    
    @GetMapping({"/", "/home"})
    String home() {
        return "Main Page";
    }
    
}
