package com.recruit.agent.candidate.profile;

import com.recruit.agent.resume.model.ResumeDocument;

/**
 * 候选人画像抽取服务接口。
 */
public interface CandidateProfileExtractor {

    /**
     * 从简历文档中抽取候选人画像。
     *
     * @param document 简历文档
     * @return 候选人画像草稿
     */
    CandidateProfileDraft extract(ResumeDocument document);
}
