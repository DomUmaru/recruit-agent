package com.recruit.agent.candidate.school;

import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;

public interface SchoolTierResolver {

    SchoolTier resolve(String schoolName, DegreeLevel degreeLevel);
}
