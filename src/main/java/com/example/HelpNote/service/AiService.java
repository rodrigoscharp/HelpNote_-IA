package com.example.HelpNote.service;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.example.HelpNote.dto.AiSuggestionRequest;
import com.example.HelpNote.dto.AiSuggestionResponse;

@Service
public class AiService {

    // Mock implementation that considers the 'Title' context to simulate a coherent lecture AI
    public AiSuggestionResponse generateSuggestion(AiSuggestionRequest request) {
        String currentText = request.getText();
        String titleContext = request.getTitle() != null ? request.getTitle().toLowerCase() : "";
        
        // Simulating some processing time (Optional but realistic for AI)
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (currentText == null || currentText.trim().isEmpty()) {
            return new AiSuggestionResponse(List.of(), "");
        }

        String lowerText = currentText.toLowerCase();

        List<String> keywords;
        String completion;

        // Contextual AI Mock Routing based on the Lecture Title
        if (titleContext.contains("medicina") || titleContext.contains("saúde") || titleContext.contains("médico") || titleContext.contains("anatomia")) {
            keywords = Arrays.asList("Diagnóstico", "Tratamento", "Paciente", "Saúde Digital");
            if (lowerText.endsWith(" ")) {
                completion = "e auxilia na redução de erros diagnósticos em exames complexos.";
            } else {
                completion = " focado na recuperação rápida do paciente no pós-operatório.";
            }
        } 
        else if (titleContext.contains("marketing") || titleContext.contains("vendas") || titleContext.contains("negócios")) {
            keywords = Arrays.asList("Conversão", "Leads", "Funil", "Engajamento");
            if (lowerText.endsWith(" ")) {
                completion = "focando diretamente na dor do cliente ideal para aumentar o ROI da campanha.";
            } else {
                completion = ", otimizando as conversões no final do funil de vendas.";
            }
        } 
        else if (titleContext.contains("história") || titleContext.contains("sociologia") || titleContext.contains("filosofia")) {
            keywords = Arrays.asList("Contexto Histórico", "Sociedade", "Revolução", "Cultura");
            if (lowerText.endsWith(" ")) {
                completion = "demonstrando o impacto profundo das mudanças nas estruturas de poder da época.";
            } else {
                completion = " alterando drasticamente as relações sociais daquele período.";
            }
        } 
        else if (titleContext.contains("tecnologia") || titleContext.contains("programação") || titleContext.contains("software")) {
            keywords = Arrays.asList("Algoritmos", "Arquitetura", "Escalabilidade", "Código");
            if (lowerText.endsWith(" ")) {
                completion = "permitindo uma arquitetura mais desacoplada e resiliente a falhas no servidor.";
            } else {
                completion = " facilitando a refatoração, manutenção e testes automatizados no futuro.";
            }
        } 
        else {
            // General presentation mock if no specific title keyword is matched
            keywords = Arrays.asList("Tópico Principal", "Discussão", "Conceito Chave");
            String[] commonCompletions = {
                " destacando a importância desse conceito para o entendimento geral do tema abordado hoje.",
                " conforme mencionado pelo palestrante durante a introdução da aula.",
                " e essa nova perspectiva abre inúmeras possibilidades para estudos futuros.",
                " gerando um impacto significativo na forma como abordamos esse problema atualmente."
            };
            completion = commonCompletions[new Random().nextInt(commonCompletions.length)];
            
            if (lowerText.endsWith(" ")) {
                completion = completion.trim(); // remove leading space if the user just typed a space
            }
        }

        return new AiSuggestionResponse(keywords, completion);
    }
}
