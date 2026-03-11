package com.recruit.agent.resume.parser;

import com.recruit.agent.resume.model.ResumeParseType;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * 基于 PDFBox 的文本型 PDF 解析器实现。
 */
@Component
public class PdfBoxResumeParser implements ResumeParser {

    private final ResumeTextCleaner resumeTextCleaner;

    public PdfBoxResumeParser(ResumeTextCleaner resumeTextCleaner) {
        this.resumeTextCleaner = resumeTextCleaner;
    }

    @Override
    public boolean supports(Path filePath) {
        return filePath.toString().toLowerCase().endsWith(".pdf");
    }

    @Override
    public ResumeParseResult parse(Path filePath) throws IOException {
        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            int pageCount = document.getNumberOfPages();
            List<String> pageTexts = new ArrayList<>(pageCount);
            PDFTextStripper pdfTextStripper = new PDFTextStripper();

            StringBuilder rawTextBuilder = new StringBuilder();
            boolean textExtractable = false;
            for (int page = 1; page <= pageCount; page++) {
                pdfTextStripper.setStartPage(page);
                pdfTextStripper.setEndPage(page);
                String pageText = pdfTextStripper.getText(document);
                String cleanedPageText = resumeTextCleaner.clean(pageText);
                pageTexts.add(cleanedPageText);
                if (!cleanedPageText.isBlank()) {
                    textExtractable = true;
                }
                if (!cleanedPageText.isBlank()) {
                    if (rawTextBuilder.length() > 0) {
                        rawTextBuilder.append("\n\n");
                    }
                    rawTextBuilder.append(cleanedPageText);
                }
            }

            ResumeParseResult result = new ResumeParseResult();
            result.setParseType(textExtractable ? ResumeParseType.PDF_TEXT : ResumeParseType.UNKNOWN);
            result.setRawText(rawTextBuilder.toString());
            result.setCleanedText(resumeTextCleaner.clean(rawTextBuilder.toString()));
            result.setPageTexts(pageTexts);
            result.setPageCount(pageCount);
            result.setTextExtractable(textExtractable);
            result.setParserLog(textExtractable ? "PDFBox 文本提取成功" : "PDFBox 未提取到有效文本，可能为扫描件 PDF");
            return result;
        }
    }
}
