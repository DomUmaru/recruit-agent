package com.recruit.agent.search.parser;

public interface SearchIntentParser {

    boolean isAvailable();

    SearchIntentParseResult parse(String text);
}
