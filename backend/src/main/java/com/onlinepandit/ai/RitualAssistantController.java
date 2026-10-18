package com.onlinepandit.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/ai")
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class RitualAssistantController {
    private final ChatClient chatClient;
    private final List<String> knowledge = List.of(
            "Griha Pravesh is a house-entry ceremony. Families commonly ask about an auspicious date, a clean home, kalash, flowers, and the specific regional tradition they follow.",
            "A Hindu wedding ceremony may include Ganesh puja, kanyadaan, pheras, and saptapadi. The sequence and customs vary by family and region; confirm details with the couple and practitioner.",
            "Satyanarayan Puja is commonly performed as a devotional observance. The practitioner should confirm the family tradition, preferred language, date, and any requested samagri.",
            "A havan is a fire ritual whose materials, duration, and procedure vary by purpose and tradition. Ask the practitioner what is included and whether the venue permits an open flame.",
            "OnlinePandit bookings are requests until the practitioner confirms. Displayed base prices are estimates; payment remains pending until a payment provider verifies the transaction.");

    public RitualAssistantController(ChatClient.Builder builder) { this.chatClient = builder.build(); }

    @PostMapping("/chat")
    @CircuitBreaker(name = "aiAssistant", fallbackMethod = "chatFallback")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        String question = request.message().trim();
        String context = knowledge.stream().mapToInt(text -> overlap(question, text)).max().orElse(0) == 0
                ? "No directly relevant local guide found. Do not invent a ritual rule; encourage asking a qualified practitioner."
                : knowledge.stream().filter(text -> overlap(question, text) > 0)
                    .sorted((left, right) -> Integer.compare(overlap(question, right), overlap(question, left)))
                    .limit(3).reduce((left, right) -> left + "\n" + right).orElse("");
        String answer = chatClient.prompt()
                .system("You are a respectful planning assistant for ceremony bookings. Be inclusive, avoid presenting religious advice as universal, and use only the supplied local guide as factual ritual context. Ask clarifying questions when regional or family practice matters.\nLocal guide:\n" + context)
                .user(question)
                .call()
                .content();
        return new ChatResponse(answer);
    }

    public ChatResponse chatFallback(ChatRequest request, Throwable cause) {
        return new ChatResponse("The ceremony assistant is temporarily unavailable. Please try again or contact our planning team.");
    }

    private static int overlap(String question, String text) {
        var words = question.toLowerCase(Locale.ROOT).split("[^\\p{L}]+");
        int score = 0;
        String lower = text.toLowerCase(Locale.ROOT);
        for (String word : words) if (word.length() > 3 && lower.contains(word)) score++;
        return score;
    }

    public record ChatRequest(@NotBlank String message) { }
    public record ChatResponse(String answer) { }
}
