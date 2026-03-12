package com.recruit.agent.resume.parser.ocr;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaddleOcrRecognizeResponse {

    private String engineName;

    private String rawText;

    private List<PaddleOcrPageResponse> pages;
}
