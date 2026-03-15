package com.example.HelpNote.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.HelpNote.dto.AiSuggestionRequest;
import com.example.HelpNote.dto.AiSuggestionResponse;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final ChatClient chatClient;

    public AiService(ChatClient.Builder chatClientBuilder, @Value("${spring.ai.openai.api-key}") String apiKey) {
        // Groq base URL should be https://api.groq.com/openai (Spring AI appends /v1)
        var openAiApi = new OpenAiApi("https://api.groq.com/openai", apiKey.trim());
        
        var chatOptions = OpenAiChatOptions.builder()
                .model("llama-3.3-70b-versatile")
                .build();
                
        var chatModel = new OpenAiChatModel(openAiApi, chatOptions);
        
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    public AiSuggestionResponse generateSuggestion(AiSuggestionRequest request) {
        if (request.getText() == null || request.getText().trim().isEmpty()) {
            return new AiSuggestionResponse(List.of(), "");
        }

        String systemPrompt = """
                Você é um assistente de escrita inteligente para o aplicativo HelpNote.
                Analise o texto do usuário e forneça:
                1. keywords: até 3 palavras-chave que resumam o contexto do texto.
                2. suggestedCompletion: uma sugestão CURTA (máximo 15 palavras) para continuar o texto naturalmente.
                3. correctedText: reescreva o texto do usuário para que se torne uma anotação MUITO MELHOR e MAIS EXPLICATIVA. Melhore a clareza, a gramática, expanda levemente as ideias se necessário e deixe o texto mais profissional e completo.
                
                REGRAS IMPORTANTES:
                - Responda SOMENTE com JSON puro, sem markdown, sem ```json, sem explicações.
                - Use aspas duplas para strings.
                - A sugestão deve ser uma continuação natural, não uma repetição.
                - Em 'correctedText', entregue o texto totalmente reescrito e melhorado, não apenas correções ortográficas. Se o texto for apenas uma ou duas palavras, expanda em uma frase útil.
                
                Formato exato (retorne apenas as chaves e os dados em uma linha JSON compacta):
                {"keywords":["palavra1","palavra2"],"suggestedCompletion":"continuação aqui","correctedText":"texto reescrito e mais explicativo aqui"}
                """;

        String userPrompt = String.format("Título: %s\nTexto atual: %s",
                request.getTitle() != null ? request.getTitle() : "Sem título",
                request.getText());

        try {
            log.info("Iniciando geração de sugestão para nota: {}", request.getTitle());
            String responseJson = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            log.debug("Resposta bruta da IA: {}", responseJson);
            return parseSuggestionResponse(responseJson, request.getText());

        } catch (Exception e) {
            log.error("Erro na chamada de IA para a nota '{}': {}", request.getTitle(), e.getMessage());
            return new AiSuggestionResponse(List.of(), "");
        }
    }

    private AiSuggestionResponse parseSuggestionResponse(String json, String originalText) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return new AiSuggestionResponse(List.of(), "");
            }

            // Remove potential markdown code blocks
            String cleanJson = json.replaceAll("```json\\s*", "")
                                   .replaceAll("```\\s*", "")
                                   .trim();

            // Extract keywords
            List<String> keywords = List.of();
            if (cleanJson.contains("\"keywords\"")) {
                int start = cleanJson.indexOf("[", cleanJson.indexOf("\"keywords\"")) + 1;
                int end = cleanJson.indexOf("]", start);
                if (start > 0 && end > start) {
                    String[] kws = cleanJson.substring(start, end).replace("\"", "").split(",");
                    keywords = Arrays.stream(kws)
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
                }
            }

            // Extract suggestedCompletion
            String completion = extractJsonString(cleanJson, "suggestedCompletion");

            // Extract correctedText
            String correctedText = extractJsonString(cleanJson, "correctedText");

            AiSuggestionResponse response = new AiSuggestionResponse(keywords, completion);
            response.setCorrectedText(correctedText.isEmpty() ? originalText : correctedText);
            return response;

        } catch (Exception e) {
            log.error("Erro ao parsear resposta da IA: {}", e.getMessage());
            return new AiSuggestionResponse(List.of(), "");
        }
    }

    /**
     * Extracts a string value from a JSON key. Simple parser for flat JSON objects.
     */
    private String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "";

        // Find the colon after the key
        int colonIndex = json.indexOf(":", keyIndex + searchKey.length());
        if (colonIndex == -1) return "";

        // Find the opening quote of the value
        int openQuote = json.indexOf("\"", colonIndex + 1);
        if (openQuote == -1) return "";

        // Find the closing quote (handle escaped quotes)
        int closeQuote = openQuote + 1;
        while (closeQuote < json.length()) {
            if (json.charAt(closeQuote) == '"' && json.charAt(closeQuote - 1) != '\\') {
                break;
            }
            closeQuote++;
        }

        if (closeQuote >= json.length()) return "";

        return json.substring(openQuote + 1, closeQuote)
                   .replace("\\\"", "\"")
                   .replace("\\n", "\n");
    }

    public String generateMeetingMinutes(String transcript, String title) {
        if (transcript == null || transcript.trim().isEmpty()) {
            return "Transcrição vazia. Não é possível gerar a ata.";
        }

        String systemPrompt = """
                Você é um assistente especializado em criar Atas de Reunião profissionais.
                Com base na transcrição fornecida, gere uma ata estruturada em HTML (sem as tags <html> ou <body>).
                Use <strong> para títulos e listas <ul>/<li> para itens.
                
                Siga esta estrutura:
                - Título da Reunião
                - Data (pode usar a data atual ou extrair se houver)
                - Pauta e Resumo
                - Decisões Tomadas
                - Ações e Próximos Passos (To-dos)
                """;

        String userPrompt = String.format("Título: %s\nTranscrição: %s",
                title != null ? title : "Reunião Sem Título",
                transcript);

        try {
            log.info("Gerando ata de reunião para: {}", title);
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Erro ao gerar ata para '{}': {}", title, e.getMessage());
            return "Erro ao gerar ata: " + e.getMessage();
        }
    }
}
