package com.example.HelpNote.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final ChatClient chatClient;
    private final String groqApiKey;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    public AiService(ChatClient.Builder chatClientBuilder,
                     @Value("${spring.ai.openai.api-key}") String apiKey,
                     ObjectMapper objectMapper) {
        this.groqApiKey = apiKey.trim();
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

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
                - Em 'correctedText', entregue o texto totalmente reescrito e melhorado.

                Formato exato:
                {"keywords":["palavra1","palavra2"],"suggestedCompletion":"continuação aqui","correctedText":"texto reescrito","suggestedTitle":"Título da nota"}
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

            return parseSuggestionResponse(responseJson, request.getText());
        } catch (Exception e) {
            log.error("Erro na chamada de IA para a nota '{}': {}", request.getTitle(), e.getMessage());
            return new AiSuggestionResponse(List.of(), "");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AiSuggestionJson {
        public List<String> keywords;
        public String suggestedCompletion;
        public String correctedText;
        public String suggestedTitle;
    }

    private AiSuggestionResponse parseSuggestionResponse(String json, String originalText) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return new AiSuggestionResponse(List.of(), "");
            }
            String cleanJson = json.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
            AiSuggestionJson parsed = objectMapper.readValue(cleanJson, AiSuggestionJson.class);

            AiSuggestionResponse response = new AiSuggestionResponse(
                    parsed.keywords != null ? parsed.keywords : List.of(),
                    parsed.suggestedCompletion != null ? parsed.suggestedCompletion : ""
            );
            response.setCorrectedText(parsed.correctedText != null && !parsed.correctedText.isBlank()
                    ? parsed.correctedText : originalText);
            response.setSuggestedTitle(parsed.suggestedTitle);
            return response;
        } catch (Exception e) {
            log.error("Erro ao parsear resposta da IA: {}", e.getMessage());
            return new AiSuggestionResponse(List.of(), "");
        }
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
