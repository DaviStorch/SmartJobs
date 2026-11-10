package com.storch.smartjobs.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class PdfOcrService {
    private final String executable;
    private final String language;
    private final int maxPages;

    public PdfOcrService(@Value("${smartjobs.ocr.tesseract:}") String executable,
                         @Value("${smartjobs.ocr.language:por+eng}") String language,
                         @Value("${smartjobs.ocr.max-pages:20}") int maxPages) {
        this.executable = executable;
        this.language = language;
        this.maxPages = Math.max(1, Math.min(maxPages, 30));
    }

    public PdfTextExtractor extract(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) throw new IllegalArgumentException("PDF protegido por senha não pode ser importado.");
            if (document.getNumberOfPages() > maxPages) {
                throw new IllegalArgumentException("O PDF excede o limite de " + maxPages + " páginas.");
            }
            String text = new PDFTextStripper().getText(document).strip();
            if (text.length() >= 40) return new PdfTextExtractor(text, null, false);
            if (executable.isBlank()) {
                return new PdfTextExtractor(text, "O PDF parece digitalizado, mas o OCR não está configurado. Instale o Tesseract e defina SMARTJOBS_OCR_TESSERACT.", false);
            }
            String ocrText = extractImages(document);
            if (ocrText.isBlank()) {
                return new PdfTextExtractor(text, "O OCR não identificou texto; confira o documento ou sua configuração de idioma.", true);
            }
            return new PdfTextExtractor(ocrText, null, true);
        }
    }

    private String extractImages(PDDocument document) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        StringBuilder text = new StringBuilder();
        long deadline = System.nanoTime() + Duration.ofSeconds(120).toNanos();
        for (int page = 0; page < document.getNumberOfPages(); page++) {
            Path imageFile = Files.createTempFile("smartjobs-ocr-", ".png");
            Path outputFile = null;
            Path errorFile = null;
            try {
                outputFile = Files.createTempFile("smartjobs-ocr-output-", ".txt");
                errorFile = Files.createTempFile("smartjobs-ocr-error-", ".txt");
                PDPage pdfPage = document.getPage(page);
                double pixelWidth = pdfPage.getCropBox().getWidth() * 150 / 72;
                double pixelHeight = pdfPage.getCropBox().getHeight() * 150 / 72;
                if (pixelWidth <= 0 || pixelHeight <= 0 || pixelWidth > 5000 || pixelHeight > 5000
                        || pixelWidth * pixelHeight > 16_000_000) {
                    throw new IllegalArgumentException("Uma página do PDF excede os limites de imagem para OCR.");
                }
                BufferedImage image = renderer.renderImageWithDPI(page, 150);
                try {
                    if (!ImageIO.write(image, "png", imageFile.toFile())) {
                        throw new IOException("Não foi possível gerar uma imagem temporária para OCR.");
                    }
                } finally {
                    image.flush();
                }
                Process process;
                try {
                    process = new ProcessBuilder(executable, imageFile.toString(), "stdout", "-l", language)
                            .redirectOutput(outputFile.toFile()).redirectError(errorFile.toFile()).start();
                } catch (IOException exception) {
                    throw new IllegalStateException("Tesseract não foi encontrado no caminho configurado.", exception);
                }
                try {
                    long remainingNanos = deadline - System.nanoTime();
                    if (remainingNanos <= 0) throw new IllegalArgumentException("O OCR excedeu o limite de 120 segundos por documento.");
                    long waitMillis = Math.min(Duration.ofSeconds(30).toMillis(),
                            Math.max(1, TimeUnit.NANOSECONDS.toMillis(remainingNanos)));
                    boolean completed = process.waitFor(waitMillis, TimeUnit.MILLISECONDS);
                    if (!completed) {
                        process.destroyForcibly().waitFor();
                        throw new IllegalArgumentException("O OCR excedeu o limite de tempo permitido.");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    process.destroyForcibly();
                    throw new IllegalStateException("A leitura do PDF foi interrompida.", exception);
                }
                if (process.exitValue() != 0) {
                    String output = Files.readString(errorFile, java.nio.charset.StandardCharsets.UTF_8).strip();
                    throw new IllegalStateException("O Tesseract falhou ao processar uma página: "
                            + output);
                }
                text.append(Files.readString(outputFile, java.nio.charset.StandardCharsets.UTF_8)).append('\n');
            } finally {
                Files.deleteIfExists(imageFile);
                if (outputFile != null) Files.deleteIfExists(outputFile);
                if (errorFile != null) Files.deleteIfExists(errorFile);
            }
        }
        return text.toString().strip();
    }
}
