package org.bsc.langgraph4j.spring.ai.agentexecutor.gemini;

import org.bsc.langgraph4j.serializer.Serializer;
import org.springframework.ai.vertexai.gemini.api.VertexAiGeminiApi;

import java.util.List;

public class LogProbsSerializer implements Serializer<VertexAiGeminiApi.LogProbs> {
    @Override
    public void write(VertexAiGeminiApi.LogProbs object, java.io.ObjectOutput out) throws java.io.IOException {
        // Ignore
    }

    @Override
    public VertexAiGeminiApi.LogProbs read(java.io.ObjectInput in) throws java.io.IOException, ClassNotFoundException {
        return new VertexAiGeminiApi.LogProbs( 0.0, List.of(), List.of() );
    }

}
