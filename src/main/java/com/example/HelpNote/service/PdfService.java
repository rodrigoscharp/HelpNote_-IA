package com.example.HelpNote.service;

import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class PdfService {

    public byte[] generatePdfFromHtml(String htmlContent) throws IOException {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(htmlContent, target);
        return target.toByteArray();
    }

    public String generateNoteHtml(String title, String date, String content, String keywords) {
        String keywordsHtml = "";
        if (keywords != null && !keywords.isBlank()) {
            String tags = Arrays.stream(keywords.split(","))
                    .map(k -> "<span class='tag'>" + k.trim() + "</span>")
                    .collect(Collectors.joining(" "));
            keywordsHtml = "<div class='keywords'>" + tags + "</div>";
        }
        return "<!DOCTYPE html><html><head><style>" +
                "body{font-family:Arial,sans-serif;padding:40px;color:#333;}" +
                ".header{border-bottom:2px solid #6366f1;padding-bottom:20px;margin-bottom:30px;}" +
                "h1{color:#6366f1;margin:0;font-size:24px;}" +
                ".meta{color:#666;font-size:14px;margin-top:8px;}" +
                ".section{margin-bottom:25px;}" +
                ".section-title{font-weight:bold;color:#444;margin-bottom:10px;border-left:4px solid #6366f1;padding-left:10px;}" +
                ".content{line-height:1.8;white-space:pre-wrap;}" +
                ".keywords{margin-top:14px;display:flex;flex-wrap:wrap;gap:8px;}" +
                ".tag{background:#eef2ff;color:#4f46e5;padding:4px 12px;border-radius:20px;font-size:12px;font-weight:500;}" +
                ".footer{margin-top:50px;font-size:12px;color:#999;text-align:center;border-top:1px solid #eee;padding-top:10px;}" +
                "</style></head><body>" +
                "<div class='header'><h1>HelpNote IA — Anotação Inteligente</h1>" +
                "<div class='meta'>Título: " + (title != null ? title : "Sem título") + "</div>" +
                "<div class='meta'>Data: " + date + "</div>" +
                keywordsHtml + "</div>" +
                "<div class='section'><div class='section-title'>Conteúdo</div>" +
                "<div class='content'>" + (content != null ? content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") : "") + "</div></div>" +
                "<div class='footer'>Gerado automaticamente pelo HelpNote IA</div>" +
                "</body></html>";
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
