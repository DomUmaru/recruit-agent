package com.recruit.agent.chat.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.chat.model.ChatSession;
import com.recruit.agent.chat.state.impl.ChatSessionStateServiceImpl;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatSessionStateServiceImplTest {

    @Test
    void shouldLoadAndApplySessionState() {
        ChatSessionStateServiceImpl service = new ChatSessionStateServiceImpl(new ObjectMapper());

        ChatSession session = new ChatSession();
        session.setPositionId("position-1");
        session.setCurrentScene(ChatScene.FILTER_REFINE);
        session.setCurrentQuery("推荐系统");
        session.setFiltersJson("{\"careerStage\":null,\"highestDegrees\":null,\"schoolTiers\":null,\"minYearsOfExperience\":3.0,\"technicalSkills\":[\"Java\"],\"currentCity\":null,\"bigTech\":null,\"outsourcing\":null}");
        session.setLastCandidateIdsJson("[\"candidate-1\",\"candidate-2\"]");
        session.setSelectedCandidateIdsJson("[\"candidate-1\"]");
        session.setSortMode("score_desc");

        ChatSessionState state = service.load(session);
        assertEquals("position-1", state.getPositionId());
        assertEquals(ChatScene.FILTER_REFINE, state.getCurrentScene());
        assertEquals("推荐系统", state.getCurrentQuery());
        assertIterableEquals(List.of("Java"), state.getFilter().getTechnicalSkills());
        assertEquals(new BigDecimal("3.0"), state.getFilter().getMinYearsOfExperience());

        CandidateSearchFilter filter = new CandidateSearchFilter();
        filter.setTechnicalSkills(List.of("Java", "Elasticsearch"));
        state.setFilter(filter);
        state.setLastCandidateIds(List.of("candidate-3"));
        service.apply(session, state);

        assertEquals("position-1", session.getPositionId());
        assertEquals("[\"candidate-3\"]", session.getLastCandidateIdsJson());
        assertEquals("{\"careerStage\":null,\"highestDegrees\":null,\"schoolTiers\":null,\"minYearsOfExperience\":null,\"technicalSkills\":[\"Java\",\"Elasticsearch\"],\"currentCity\":null,\"bigTech\":null,\"outsourcing\":null}", session.getFiltersJson());
    }
}
