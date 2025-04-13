package com.krm.ReseachAssistant;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/research")
@CrossOrigin(origins = "*")
@AllArgsConstructor
public class ResearchController {
    private final ResearchService researchService;
@PostMapping("/process")
    public ResponseEntity<String> processContent(@RequestBody ResearchRequest researchRequest) {
        String result = researchService.processContent(researchRequest);
        return ResponseEntity.ok(result);
    }

}
