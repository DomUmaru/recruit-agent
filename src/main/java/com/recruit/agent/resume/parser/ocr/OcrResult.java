package com.recruit.agent.resume.parser.ocr;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OcrResult {

    private List<String> pageTexts;

    private String rawText;

    private String engineName;
}
