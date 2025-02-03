package com.example.html2pdf;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PdfController {

    private final String PUPPETEER_API = "http://localhost:5000/convert";

    @PostMapping("/convert")
    public ResponseEntity<?> convertHtmlToPdf(@RequestParam("file") MultipartFile file) {
        try {
            // Debugging: Check if file is received
            System.out.println("Received file: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("No file uploaded");
            }

            // Send request to Puppeteer
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();

            // Send request to Puppeteer and get the response JSON
            ResponseEntity<Map> response = restTemplate.exchange(PUPPETEER_API, HttpMethod.POST, requestEntity,
                    Map.class);

            System.out.println("Puppeteer Response Status: " + response.getStatusCode());

            // Check if the response contains a valid file path
            Map<String, String> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("filePath")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generating PDF");
            }

            String filePath = responseBody.get("filePath");

            // Return the file path so the frontend can display a notification
            Map<String, String> jsonResponse = new HashMap<>();
            jsonResponse.put("message", "File generated successfully.");
            jsonResponse.put("filePath", filePath);

            return ResponseEntity.ok().body(jsonResponse);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing the file");
        }
    }
}
