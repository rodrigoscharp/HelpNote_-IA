package com.example.HelpNote.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.example.HelpNote.dto.AiSuggestionRequest;
import com.example.HelpNote.dto.AiSuggestionResponse;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final ChatClient chatClient;
    private final String groqApiKey;
    private final RestTemplate restTemplate = new RestTemplate();

    public AiService(ChatClient.Builder chatClientBuilder, @Value("${spring.ai.openai.api-key}") String apiKey) {
        this.groqApiKey = apiKey.trim();
        var openAiApi = new OpenAiApi("https://api.groq.com/openai", this.groqApiKey);

        var chatOptions = OpenAiChatOptions.builder()
                .model("llama-3.3-70b-versatile")
                .build();

        var chatModel = new OpenAiChatModel(openAiApi, chatOptions);
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    /**
     * Transcribes an audio file using Groq Whisper API.
     */
    public String transcribeAudio(Path audioFilePath, String originalFilename) throws IOException {
        byte[] audioBytes = Files.readAllBytes(audioFilePath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(groqApiKey);

        String filename = originalFilename != null ? originalFilename : "audio.mp3";

        ByteArrayResource audioResource = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", audioResource);
        body.add("model", "whisper-large-v3");
        body.add("language", "pt");
        body.add("response_format", "text");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            String result = restTemplate.postForObject(
                    "https://api.groq.com/openai/v1/audio/transcriptions",
                    requestEntity,
                    String.class
            );
            log.info("Transcrição concluída: {} chars", result != null ? result.length() : 0);
            return result;
        } catch (Exception e) {
            log.error("Erro ao transcrever áudio: {}", e.getMessage());
            throw new RuntimeException("Falha na transcrição de áudio: " + e.getMessage(), e);
        }
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
                4. suggestedTitle: um título CURTO (máximo 5 palavras) que represente bem o tema principal do texto.

                REGRAS IMPORTANTES:
                - Responda SOMENTE com JSON puro, sem markdown, sem ```json, sem explicações.
                - Use aspas duplas para strings.
                - A sugestão deve ser uma continuação natural, não uma repetição.
                - Em 'correctedText', entregue o texto totalmente reescrito e melhorado, não apenas correções ortográficas. Se o texto for apenas uma ou duas palavras, expanda em uma frase útil.

                Formato exato (retorne apenas as chaves e os dados em uma linha JSON compacta):
                {"keywords":["palavra1","palavra2"],"suggestedCompletion":"continuação aqui","correctedText":"texto reescrito e mais explicativo aqui","suggestedTitle":"Título da nota"}
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

            String suggestedTitle = extractJsonString(cleanJson, "suggestedTitle");

            AiSuggestionResponse response = new AiSuggestionResponse(keywords, completion);
            response.setCorrectedText(correctedText.isEmpty() ? originalText : correctedText);
            response.setSuggestedTitle(suggestedTitle.isEmpty() ? null : suggestedTitle);
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

    public String analyzeTranscript(String transcript) {
        if (transcript == null || transcript.trim().isEmpty()) {
            return "{\"summary\":\"Sem transcrição disponível.\",\"todos\":[]}";
        }

        String systemPrompt = """
                Você é um assistente especializado em análise de reuniões.
                Analise a transcrição fornecida e retorne um JSON com:
                1. "summary": resumo conciso da reunião em 2-3 frases
                2. "todos": lista de até 5 ações/próximos passos extraídos da reunião

                REGRAS:
                - Responda SOMENTE com JSON puro, sem markdown, sem ```json.
                - Use aspas duplas para strings.
                - Formato exato: {"summary":"resumo aqui","todos":["ação 1","ação 2"]}
                """;

        try {
            log.info("Analisando transcrição...");
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user("Transcrição: " + transcript)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Erro ao analisar transcrição: {}", e.getMessage());
            return "{\"summary\":\"Erro ao processar transcrição.\",\"todos\":[]}";
        }
    }

    public String chatAboutTranscript(String transcript, String question) {
        if (transcript == null || transcript.trim().isEmpty()) {
            return "Não há transcrição disponível para responder sua pergunta.";
        }
        if (question == null || question.trim().isEmpty()) {
            return "Por favor, faça uma pergunta.";
        }

        String systemPrompt = """
                Você é um assistente especializado em análise de reuniões.
                Você tem acesso à transcrição de uma reunião e deve responder perguntas sobre ela.
                Seja conciso e direto. Responda em português.
                Se a informação não estiver na transcrição, diga que não encontrou na gravação.
                """;

        String userPrompt = String.format("Transcrição da reunião:\n%s\n\nPergunta: %s", transcript, question);

        try {
            log.info("Respondendo pergunta sobre transcrição...");
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Erro ao responder pergunta: {}", e.getMessage());
            return "Erro ao processar sua pergunta.";
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
