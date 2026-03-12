package com.recruit.agent.agent.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.recruit.agent.agent.selection.impl.CandidateSelectionServiceImpl;
import com.recruit.agent.chat.state.ChatSessionState;
import java.util.List;
import org.junit.jupiter.api.Test;

class CandidateSelectionServiceImplTest {

    @Test
    void shouldResolveTopAndOrdinalSelection() {
        CandidateSelectionServiceImpl service = new CandidateSelectionServiceImpl();
        ChatSessionState state = new ChatSessionState();
        state.setLastCandidateIds(List.of("candidate-1", "candidate-2", "candidate-3", "candidate-4"));

        assertEquals(List.of("candidate-1", "candidate-2"),
            service.resolveSelectedCandidateIds(state, "把前两个拿出来对比一下"));
        assertEquals(List.of("candidate-1"),
            service.resolveSelectedCandidateIds(state, "给第一个出面试题"));
        assertEquals(List.of("candidate-2"),
            service.resolveSelectedCandidateIds(state, "给第2个候选人出题"));
    }

    @Test
    void shouldResolveScopedAndCombinedOrdinalSelection() {
        CandidateSelectionServiceImpl service = new CandidateSelectionServiceImpl();
        ChatSessionState state = new ChatSessionState();
        state.setLastCandidateIds(List.of("candidate-1", "candidate-2", "candidate-3", "candidate-4"));

        assertEquals(List.of("candidate-2"),
            service.resolveSelectedCandidateIds(state, "保留前3个，给第2个出题"));
        assertEquals(List.of("candidate-1", "candidate-3"),
            service.resolveSelectedCandidateIds(state, "把第一个和第三个拉出来对比"));
        assertEquals(List.of("candidate-2"),
            service.resolveSelectedCandidateIds(state, "第2个和第2个再看一下"));
    }

    @Test
    void shouldResolvePronounAndAllSelection() {
        CandidateSelectionServiceImpl service = new CandidateSelectionServiceImpl();
        ChatSessionState state = new ChatSessionState();
        state.setLastCandidateIds(List.of("candidate-1", "candidate-2", "candidate-3"));
        state.setSelectedCandidateIds(List.of("candidate-2", "candidate-3"));

        assertEquals(List.of("candidate-2", "candidate-3"),
            service.resolveSelectedCandidateIds(state, "给这两个人出面试题"));
        assertEquals(List.of("candidate-1", "candidate-2", "candidate-3"),
            service.resolveSelectedCandidateIds(state, "把所有人都对比一下"));
    }

    @Test
    void shouldReturnNullWhenNoSelectionPhraseExists() {
        CandidateSelectionServiceImpl service = new CandidateSelectionServiceImpl();
        ChatSessionState state = new ChatSessionState();
        state.setLastCandidateIds(List.of("candidate-1", "candidate-2"));

        assertNull(service.resolveSelectedCandidateIds(state, "帮我总结一下这些人的情况"));
    }
}
