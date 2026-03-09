package org.micro.analysis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeminiService {

    private final WebClient webClient;
    
    @Value("${gemini.api-key:MISSING}")
    private String geminiApiKey;

    public GeminiService(WebClient.Builder webClientBuilder) {
        // [핵심] URL 인코딩을 하지 않도록 설정하여 콜론(:) 문제를 해결함
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        
        this.webClient = webClientBuilder
                .uriBuilderFactory(factory)
                .build();
    }

    @PostConstruct
    public void init() {
        String key = getValidKey();
        if (key.isEmpty()) {
            log.error("🛑 [CRITICAL] API KEY가 없습니다!");
        } else {
            log.info("✅ [OK] API KEY 확인됨: {}... (길이: {})", key.substring(0, 5), key.length());
        }
    }

    private String getValidKey() {
        String key = System.getenv("GEMINI_API_KEY");
        if (key == null || key.isEmpty() || key.contains("${")) {
            key = geminiApiKey;
        }
        return key.replace("\"", "").replace("'", "").trim();
    }

    public Long findMatchingEvent(String newTitle, List<NewsEvent> recentEvents) {
        if (recentEvents.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (NewsEvent e : recentEvents) {
            sb.append("[").append(e.getId()).append("] ").append(e.getMainTitle()).append("\n");
        }
        String prompt = "Headline: " + newTitle + "\nExisting:\n" + sb.toString() + "\nMatch ID or NEW.";
        String response = callGemini(prompt).trim();
        try {
            if (response.contains("NEW") || response.isEmpty() || response.length() > 10) return null;
            return Long.parseLong(response.replaceAll("[^0-9]", ""));
        } catch (Exception e) { return null; }
    }

    public String analyzePerspective(String title, String source, String currentSummary) {
        String prompt = "Analyze this news article's perspective relative to existing context.\n" +
                "Article: " + title + " (Source: " + source + ")\n" +
                "Current Context Summary: " + currentSummary + "\n\n" +
                "Please provide analysis in Korean including:\n" +
                "1. Summary and perspective\n" +
                "2. Sentiment (Bullish/Bearish/Neutral)\n" +
                "3. Potentially affected stocks (Buy recommendations / Sell warnings if applicable)";
        return callGemini(prompt);
    }

    public String generateInitialSummary(String title) {
        String prompt = "Analyze this news headline for global financial impact.\n" +
                "Headline: " + title + "\n\n" +
                "Please provide a structured analysis in Korean including:\n" +
                "1. Core Summary and background\n" +
                "2. Market Sentiment (Is this Good or Bad for markets?)\n" +
                "3. Affected Sectors/Stocks (Specific Buy/Sell candidates if possible)";
        return callGemini(prompt);
    }

    public String updateGlobalSummary(String currentSummary, String newTitle, String source) {
        String prompt = "Update the global summary based on new info.\n" +
                "Current Global Summary: " + currentSummary + "\n" +
                "New Article: " + newTitle + " (Source: " + source + ")\n\n" +
                "Provide an updated, comprehensive analysis in Korean covering the overall situation, " +
                "market sentiment, and recommended stock movements.";
        return callGemini(prompt);
    }

    private String callGemini(String prompt) {
        String key = getValidKey();
        // 사용자의 요청에 따라 Gemini 2.0 Flash 모델을 사용합니다.
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + key;
        
        try {
            Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                    "parts", List.of(Map.of("text", prompt))
                ))
            );

            log.info("🤖 Calling Gemini API (Flash 2.0) for Deep Analysis...");
            Map response = webClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            if (response != null && response.containsKey("candidates")) {
                List candidates = (List) response.get("candidates");
                Map candidate = (Map) candidates.get(0);
                Map content = (Map) candidate.get("content");
                List parts = (List) content.get("parts");
                String result = (String) ((Map) parts.get(0)).get("text");
                
                log.info("✨ [GEMINI SUCCESS] Analysis Result:\n{}", result.trim());
                return result.trim();
            }
            return "분석 실패";
        } catch (Exception e) {
            log.error("❌ Gemini API Error: {}", e.getMessage());
            return "분석 실패 (" + e.getMessage() + ")";
        }
    }
}
