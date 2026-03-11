package com.recruit.agent.candidate.profile;

import com.recruit.agent.resume.model.ResumeDocument;

/**
 * 候选人画像索引服务接口。
 */
public interface CandidateProfileIndexService {

    /**
     * 从简历文档抽取画像并写入候选人聚合索引。
     *
     * @param document 简历文档
     */
    void indexProfile(ResumeDocument document);
}
