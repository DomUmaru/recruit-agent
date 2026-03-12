package com.recruit.agent.resume.parser.ocr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaddleOcrServiceImplTest {

    @Test
    void shouldReportAvailableWhenBaseUrlExists() {
        OcrProviderProperties properties = new OcrProviderProperties();
        properties.setProvider("paddle");
        properties.setBaseUrl("http://localhost:9001");

        PaddleOcrServiceImpl service = new PaddleOcrServiceImpl(properties, new ObjectMapper());

        assertTrue(service.isAvailable());
    }

    @Test
    void shouldFailFastWhenBaseUrlIsMissing() {
        OcrProviderProperties properties = new OcrProviderProperties();
        properties.setProvider("paddle");

        PaddleOcrServiceImpl service = new PaddleOcrServiceImpl(properties, new ObjectMapper());

        assertThrows(IOException.class, () -> service.recognize(java.nio.file.Path.of("resume.pdf")));
    }

    @Test
    void shouldMapAdapterResponseToOcrResult() throws Exception {
        OcrProviderProperties properties = new OcrProviderProperties();
        properties.setProvider("paddle");
        properties.setBaseUrl("http://localhost:9001");
        PaddleOcrServiceImpl service = new PaddleOcrServiceImpl(properties, new ObjectMapper());

        PaddleOcrRecognizeResponse response = new PaddleOcrRecognizeResponse();
        response.setEngineName("paddleocr");
        response.setPages(List.of(page(1, "第一页"), page(2, "第二页")));

        Method method = PaddleOcrServiceImpl.class.getDeclaredMethod("toOcrResult", PaddleOcrRecognizeResponse.class);
        method.setAccessible(true);
        OcrResult result = (OcrResult) method.invoke(service, response);

        assertEquals(List.of("第一页", "第二页"), result.getPageTexts());
        assertEquals("第一页\n\n第二页", result.getRawText());
        assertEquals("paddleocr", result.getEngineName());
    }

    private PaddleOcrPageResponse page(int pageNo, String text) {
        PaddleOcrPageResponse page = new PaddleOcrPageResponse();
        page.setPageNo(pageNo);
        page.setText(text);
        return page;
    }
}
