package com.example.HelpNote.service;

import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class PdfService {

    public byte[] generatePdfFromHtml(String htmlContent) throws IOException {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(htmlContent, target);
        return target.toByteArray();
    }

    public String generateAtaHtml(String title, String date, String transcription, String summary) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; padding: 40px; color: #333; }" +
                ".header { border-bottom: 2px solid #6c5ce7; padding-bottom: 20px; margin-bottom: 30px; }" +
                "h1 { color: #6c5ce7; margin: 0; font-size: 24px; }" +
                ".meta { color: #666; font-size: 14px; margin-top: 10px; }" +
                ".section { margin-bottom: 25px; }" +
                ".section-title { font-weight: bold; color: #444; margin-bottom: 10px; border-left: 4px solid #6c5ce7; padding-left: 10px; }" +
                ".content { line-height: 1.6; text-align: justify; }" +
                ".footer { margin-top: 50px; font-size: 12px; color: #999; text-align: center; border-top: 1px solid #eee; padding-top: 10px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='header'>" +
                "<h1>HelpNote IA - Ata de Reunião</h1>" +
                "<div class='meta'>Título: " + title + "</div>" +
                "<div class='meta'>Data: " + date + "</div>" +
                "</div>" +
                "<div class='section'>" +
                "<div class='section-title'>Resumo da Reunião</div>" +
                "<div class='content'>" + (summary != null ? summary : "Resumo automático sendo processado...") + "</div>" +
                "</div>" +
                "<div class='section'>" +
                "<div class='section-title'>Transcrição Completa</div>" +
                "<div class='content'>" + transcription.replace("\n", "<br>") + "</div>" +
                "</div>" +
                "<div class='footer'>Gerado automaticamente pelo HelpNote IA</div>" +
                "</body>" +
                "</html>";
    }
}
