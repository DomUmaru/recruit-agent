package com.recruit.agent.chat.state.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.chat.model.ChatSession;
import com.recruit.agent.chat.state.ChatSessionState;
import com.recruit.agent.chat.state.ChatSessionStateService;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Chat session state persistence adapter.
 */
@Service
public class ChatSessionStateServiceImpl implements ChatSessionStateService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public ChatSessionStateServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatSessionState load(ChatSession session) {
        ChatSessionState state = new ChatSessionState();
        state.setPositionId(session.getPositionId());
        state.setCurrentScene(session.getCurrentScene() == null ? ChatScene.SEARCH : session.getCurrentScene());
        state.setCurrentQuery(session.getCurrentQuery());
        state.setFilter(read(session.getFiltersJson(), CandidateSearchFilter.class, new CandidateSearchFilter()));
        state.setLastCandidateIds(read(session.getLastCandidateIdsJson(), STRING_LIST_TYPE, null));
        state.setSelectedCandidateIds(read(session.getSelectedCandidateIdsJson(), STRING_LIST_TYPE, null));
        state.setSortMode(session.getSortMode());
        return state;
    }

    @Override
    public void apply(ChatSession session, ChatSessionState state) {
        session.setPositionId(state.getPositionId());
        session.setCurrentScene(state.getCurrentScene());
        session.setCurrentQuery(state.getCurrentQuery());
        session.setFiltersJson(write(state.getFilter()));
        session.setLastCandidateIdsJson(write(state.getLastCandidateIds()));
        session.setSelectedCandidateIdsJson(write(state.getSelectedCandidateIds()));
        session.setSortMode(state.getSortMode());
        session.setSessionStateJson(write(state));
    }

    private <T> T read(String json, Class<T> type, T defaultValue) {
        if (json == null || json.isBlank()) {
            return defaultValue;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize chat session state.", ex);
        }
    }

    private <T> T read(String json, TypeReference<T> type, T defaultValue) {
        if (json == null || json.isBlank()) {
            return defaultValue;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize chat session state.", ex);
        }
    }

    private String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize chat session state.", ex);
        }
    }
}
