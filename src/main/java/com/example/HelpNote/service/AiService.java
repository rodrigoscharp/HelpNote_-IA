package com.example.HelpNote.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.example.HelpNote.dto.AiSuggestionRequest;
import com.example.HelpNote.dto.AiSuggestionResponse;

@Service
public class AiService {

    private final ChatClient chatClient;

    public AiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public AiSuggestionResponse generateSuggestion(AiSuggestionRequest request) {
        if (request.getText() == null || request.getText().trim().isEmpty()) {
            return new AiSuggestionResponse(List.of(), "");
        }

        String systemPrompt = """
                Você é um assistente de escrita para o aplicativo HelpNote.
                Sua tarefa é analisar o texto atual do usuário e o título da nota, e fornecer:
                1. Uma lista de até 3 palavras-chave (keywords) que resumam o contexto.
                2. Uma sugestão de continuação curta e natural para o texto (completion).
                
                Responda APENAS em formato JSON válido, sem markdown, seguindo esta estrutura:
                {
                  "keywords": ["exemplo1", "exemplo2"],
                  "completion": "continuação sugerida..."
                }
                """;

        String userPrompt = "Título: {title}\nTexto atual: {text}";
        
        try {
            String responseJson = chatClient.prompt()
                    .system(systemPrompt)
                    .user(u -> u.text(userPrompt)
                            .param("title", request.getTitle() != null ? request.getTitle() : "Sem título")
                            .param("text", request.getText()))
                    .call()
                    .content();

            // Simple manual parse for now to avoid extra dependencies, or use a proper JSON library if available.
            // Assuming the LLM follows the JSON instructions strictly.
            // Using a more robust approach with Map if possible, but let's stick to simple logic for now.
            
            return parseSuggestionResponse(responseJson);
            
        } catch (Exception e) {
            // Fallback to minimal response on error
            return new AiSuggestionResponse(List.of("AI Error"), " Tente novamente em instantes.");
        }
    }

    private AiSuggestionResponse parseSuggestionResponse(String json) {
        // Very basic parsing for demo purposes; in production use Jackson
        try {
            // Remove potential markdown code blocks
            String cleanJson = json.replaceAll("```json", "").replaceAll("```", "").trim();
            
            // Extract keywords
            List<String> keywords = List.of();
            if (cleanJson.contains("\"keywords\"")) {
                int start = cleanJson.indexOf("[") + 1;
                int end = cleanJson.indexOf("]");
                String[] kws = cleanJson.substring(start, end).replace("\"", "").split(",");
                keywords = Arrays.stream(kws).map(String::trim).toList();
            }

            // Extract completion (suggestedCompletion)
            String completion = "";
            if (cleanJson.contains("\"suggestedCompletion\"")) {
                int start = cleanJson.indexOf("\"suggestedCompletion\":") + 22;
                int lastQuote = cleanJson.lastIndexOf("\"");
                int firstQuote = cleanJson.indexOf("\"", start);
                completion = cleanJson.substring(firstQuote + 1, lastQuote).trim();
            }

            return new AiSuggestionResponse(keywords, completion);
        } catch (Exception e) {
            return new AiSuggestionResponse(List.of("Analysis"), " Erro ao processar sugestão.");
        }
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

        String userPrompt = "Título: {title}\nTranscrição: {transcript}";

        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(u -> u.text(userPrompt)
                            .param("title", title != null ? title : "Reunião Sem Título")
                            .param("transcript", transcript))
                    .call()
                    .content();
        } catch (Exception e) {
            return "Erro ao gerar ata: " + e.getMessage();
        }
    }
}
