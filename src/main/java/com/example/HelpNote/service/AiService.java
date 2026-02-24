package com.example.HelpNote.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.HelpNote.dto.AiSuggestionRequest;
import com.example.HelpNote.dto.AiSuggestionResponse;

@Service
public class AiService {

    public AiSuggestionResponse generateSuggestion(AiSuggestionRequest request) {
        // ... existing generateSuggestion code ...
        return generateSuggestionInternal(request);
    }

    private AiSuggestionResponse generateSuggestionInternal(AiSuggestionRequest request) {
        String currentText = request.getText();
        String titleContext = request.getTitle() != null ? request.getTitle().toLowerCase() : "";
        
        try {
            Thread.sleep(600); // Faster response for better UX
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (currentText == null || currentText.trim().isEmpty()) {
            return new AiSuggestionResponse(List.of(), "");
        }

        String lowerText = currentText.toLowerCase().trim();
        String[] words = lowerText.split("\\s+");
        
        List<String> keywords;
        String completion;

        // Simulate "Prompt-like" logic by analyzing the end of the user's text
        if (lowerText.contains("então") || lowerText.contains("portanto") || lowerText.contains("concluimos que")) {
            completion = " é fundamental revisarmos os pontos discutidos para garantir o alinhamento total da equipe.";
            keywords = Arrays.asList("Conclusão", "Alinhamento", "Próximos Passos");
        } else if (lowerText.contains("por exemplo") || lowerText.contains("como")) {
            completion = " podemos citar o caso de sucesso da implementação anterior, que reduziu custos em 15%.";
            keywords = Arrays.asList("Exemplo", "Estudo de Caso", "Eficiência");
        } else if (lowerText.contains("o problema") || lowerText.contains("a dificuldade")) {
            completion = " reside na falta de integração entre os sistemas legados e a nova infraestrutura de nuvem.";
            keywords = Arrays.asList("Problema", "Infraestrutura", "Integração");
        } else if (words.length > 10) {
            // Continuation based on general context if text is getting long
            if (titleContext.contains("tecnologia") || lowerText.contains("sistema") || lowerText.contains("app")) {
                completion = " facilitando a escalabilidade do sistema através de microsserviços bem definidos.";
                keywords = Arrays.asList("Escalabilidade", "Sistemas", "Arquitetura");
            } else if (titleContext.contains("negócios") || lowerText.contains("vendas") || lowerText.contains("mercado")) {
                completion = " o que permite uma vantagem competitiva significativa no cenário atual do mercado.";
                keywords = Arrays.asList("Competitividade", "Mercado", "Estratégia");
            } else {
                completion = " essa abordagem permite uma compreensão mais profunda sobre as nuances do assunto.";
                keywords = Arrays.asList("Análise", "Contexto", "Perspectiva");
            }
        } else {
            // Small prompt for short text
            completion = " para continuarmos o desenvolvimento desta ideia central de forma estruturada.";
            keywords = Arrays.asList("Ideia", "Estrutura", "Desenvolvimento");
        }

        // UX check: If the user didn't finish with a space, add one to the completion
        if (!currentText.endsWith(" ")) {
            completion = " " + completion.trim();
        }

        return new AiSuggestionResponse(keywords, completion);
    }

    public String generateMeetingMinutes(String transcript, String title) {
        // Mocking a long LLM generation for meeting minutes
        try {
            Thread.sleep(2000); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        StringBuilder ata = new StringBuilder();
        ata.append("<strong>Título:</strong> ").append(title != null ? title : "Reunião Sem Título").append("<br><br>");
        ata.append("<strong>Data:</strong> 23/02/2026<br>");
        ata.append("<strong>Pauta:</strong> Alinhamento de objetivos e definição de próximos passos.<br><br>");
        
        ata.append("<strong>Discussão:</strong><br>");
        if (transcript.toLowerCase().contains("produto") || transcript.toLowerCase().contains("design")) {
            ata.append("- A equipe revisou os protótipos de alta fidelidade e validou a nova paleta de cores.<br>");
            ata.append("- O módulo de Analytics foi adiado para a v2.0 para focar na estabilidade core.<br>");
        } else {
            ata.append("- Discutiu-se a expansão para o mercado europeu e a implementação de novos métodos de pagamento.<br>");
            ata.append("- Foi levantada a necessidade de uma revisão técnica profunda no gateway atual.<br>");
        }

        ata.append("<br><strong>Decisões:</strong><br>");
        ata.append("- Aprovação unânime do novo cronograma de entregas para o Q3.<br>");
        ata.append("- Alocação de dois desenvolvedores extras para a feature de segurança.<br>");

        ata.append("<br><strong>Ações (To-dos):</strong><br>");
        ata.append("- [ ] Enviar orçamento de marketing para o financeiro.<br>");
        ata.append("- [ ] Agendar reunião com o suporte técnico para terça-feira.<br>");
        ata.append("- [ ] Revisar documentos de tradução do aplicativo.<br>");

        return ata.toString();
    }
}
