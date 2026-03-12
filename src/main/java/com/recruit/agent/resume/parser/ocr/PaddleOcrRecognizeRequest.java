package com.recruit.agent.resume.parser.ocr;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaddleOcrRecognizeRequest {

    private String filename;

    private String fileBase64;
}
