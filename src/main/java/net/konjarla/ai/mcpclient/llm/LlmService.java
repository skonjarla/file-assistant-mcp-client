package net.konjarla.ai.mcpclient.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.konjarla.ai.mcpclient.llm.model.VectorItem;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmService {
    @NonNull
    private final ChatClient chatClient;
    @NonNull
    private final ChatClient openAiChatClient;
    @NonNull
    private List<McpSyncClient> mcpSyncClients;
    @NonNull
    SyncMcpToolCallbackProvider toolCallbackProvider;

    @Value("${file.assistant.system.prompt}")
    private Resource systemPromptFileAssistant;
    @Value("${result.format.system.prompt}")
    private Resource formatSystemPrompt;
    @Value("${vector.query.template}")
    private Resource vectorQueryTemplate;
    @NonNull
    ObjectMapper objectMapper;
    // Map<String, String> promptMessages = new java.util.HashMap<>();

    // Non-streaming chat with system prompt and history
    public String toolChat(String prompt, String systemPrompt, List<String> history) {
        List<Message> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        } else {
            Map<String, String> promptMessages = getPromptMessagesForChat(prompt);
            if (!promptMessages.isEmpty()) {
                log.debug("Using custom system prompt from MCP for file assistant: {}", promptMessages.get("system-message"));
                messages.add(new SystemMessage(promptMessages.get("system-message")));
            } else {
                log.debug("Using default system prompt for file assistant: {}", systemPromptFileAssistant);
                messages.add(new SystemMessage(systemPromptFileAssistant.toString()));
            }
        }

        if (history != null) {
            for (String h : history) {
                messages.add(new UserMessage(h));
            }
        }

        messages.add(new UserMessage(prompt));
        Prompt chatPrompt = new Prompt(messages);
        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
        try {
            String response = openAiChatClient.prompt(chatPrompt)
                    .toolCallbacks(toolCallbacks)
                    .call()
                    .content();
            assert response != null;
            return response;
        } catch (Exception e) {
            log.error("Error in tool chat", e);
            return "Error in tool chat";
        }
    }

    public List<VectorItem> vectorChat(String query) {
        // PromptTemplate promptTemplate = new PromptTemplate(vectorQueryTemplate);
        // Message message = promptTemplate.createMessage(Map.of("query", query));
        Map<String, String> promptMessages = getPromptMessagesForVectorSearch(query);
        String systemPrompt;
        String userPrompt;
        if (!promptMessages.isEmpty()) {
            log.debug("Using custom prompt from MCP: {}", promptMessages);
            if (promptMessages.containsKey("system-message")) {
                log.debug("Using custom system prompt from MCP: {}", promptMessages.get("system-message"));
                systemPrompt = promptMessages.get("system-message");
            } else {
                log.debug("Using default system prompt: {}", formatSystemPrompt);
                systemPrompt = formatSystemPrompt.toString();
            }
            if (promptMessages.containsKey("user-message")) {
                log.debug("Using custom user prompt from MCP: {}", promptMessages.get("user-message"));
                userPrompt = promptMessages.get("user-message");
            } else {
                log.debug("Using default user prompt: {}", query);
                PromptTemplate promptTemplate = new PromptTemplate(vectorQueryTemplate);
                Message message = promptTemplate.createMessage(Map.of("query", query));
                userPrompt = message.toString();
            }
        } else {
            log.debug("Using default system prompt: {}", formatSystemPrompt);
            systemPrompt = formatSystemPrompt.toString();
            log.debug("Using default user prompt: {}", query);
            PromptTemplate promptTemplate = new PromptTemplate(vectorQueryTemplate);
            Message message = promptTemplate.createMessage(Map.of("query", query));
            userPrompt = message.toString();
        }
        //String systemPrompt = promptMessages.getOrDefault("system-message", formatSystemPrompt.toString());
        String message = promptMessages.getOrDefault("user-message", query);
        Prompt chatPrompt = new Prompt(message);
        try {
            String response = openAiChatClient.prompt(userPrompt)
                    //.system(formatSystemPrompt)
                    .system(systemPrompt)
                    .toolCallbacks(toolCallbackProvider)
                    .call()
                    .content();
            assert response != null;
            List<VectorItem> results = objectMapper.readValue(response,
                    new TypeReference<>() {
                    });
            //return response;
            return results;
        } catch (Exception e) {
            log.error("Error in tool chat", e);
            // return "Error in tool chat";
            return (List.of());
        }
    }

    // Streaming chat with system prompt and history
    public Flux<String> chatStream(String prompt, String systemPrompt, List<String> history) {
        List<Message> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        }
        if (history != null) {
            for (String h : history) {
                messages.add(new UserMessage(h));
            }
        }
        messages.add(new UserMessage(prompt));
        Prompt chatPrompt = new Prompt(messages);
        Flux<ChatClientResponse> stream = openAiChatClient.prompt(chatPrompt).stream().chatClientResponse();
        return stream.map(resp -> {
            assert resp.chatResponse() != null;
            return resp.chatResponse().getResult().getOutput().getText();
        });
    }

    private Map<String, String> getPromptMessagesForVectorSearch(String query) {
        McpSyncClient mcpSyncClient = mcpSyncClients.get(0);
        if (mcpSyncClient != null) {
            List<McpSchema.Prompt> prompts = mcpSyncClient.listPrompts().prompts();
            if (!prompts.isEmpty()) {
                Map<String, String> result = new java.util.HashMap<>();
                for (McpSchema.Prompt prompt : prompts) {
                    String promptName = prompt.name();
                    if(promptName.equals("vector-search-query-template")) {
                        String value = getPromptMessage(mcpSyncClient, prompt, query);
                        result.put("user-message", value);
                    } else if(promptName.equals("vector-search-result-format-system-prompt")) {
                        String value = getPromptMessage(mcpSyncClient, prompt, query);
                        result.put("system-message", value);
                    }
                }
                return result;
            }
        }
        return Map.of();
    }
    
    private Map<String, String> getPromptMessagesForChat(String query) {
        McpSyncClient mcpSyncClient = mcpSyncClients.get(0);
        if (mcpSyncClient != null) {
            List<McpSchema.Prompt> prompts = mcpSyncClient.listPrompts().prompts();
            if (!prompts.isEmpty()) {
                Map<String, String> result = new java.util.HashMap<>();
                for (McpSchema.Prompt prompt : prompts) {
                    String promptName = prompt.name();
                    if(promptName.equals("file-search-system-prompt")) {
                        String value = getPromptMessage(mcpSyncClient, prompt, query);
                        result.put("system-message", value);
                    }
                }
                return result;
            }
        }
        return Map.of();
    }
    
    private String getPromptMessage(McpSyncClient mcpSyncClient, McpSchema.Prompt prompt, String query) {
        String templateString = prompt.arguments().get(0).name();
        McpSchema.GetPromptRequest request;
        if (prompt.arguments().get(0).required()) {
            request = new McpSchema.GetPromptRequest(prompt.name(),
                    Map.of(templateString, query));
        } else {
            request = new McpSchema.GetPromptRequest(prompt.name(), Map.of(templateString, ""));
        }
        McpSchema.GetPromptResult response = mcpSyncClient.getPrompt(request);
        return (response.messages() != null && !response.messages().isEmpty()) ? response.messages().get(0).toString() : "";
    }

    private Map<String, String> getPromptMessages(String query) {
        McpSyncClient mcpSyncClient = mcpSyncClients.get(0);
        if (mcpSyncClient != null) {
            List<McpSchema.Prompt> prompts = mcpSyncClient.listPrompts().prompts();
            if (!prompts.isEmpty()) {
                Map<String, String> result = new java.util.HashMap<>();
                for (McpSchema.Prompt prompt : prompts) {
                    String templateString = prompt.arguments().get(0).name();
                    McpSchema.GetPromptRequest request;
                    if (prompt.arguments().get(0).required()) {
                        request = new McpSchema.GetPromptRequest(prompt.name(),
                                Map.of(templateString, query));
                    } else {
                        request = new McpSchema.GetPromptRequest(prompt.name(), Map.of(templateString, ""));
                    }
                    McpSchema.GetPromptResult response = mcpSyncClient.getPrompt(request);
                    String key = prompt.arguments().get(0).required() ? "user-message" : "system-message";
                    // Use the first message's toString() if available, else empty string
                    String value = (response.messages() != null && !response.messages().isEmpty()) ? response.messages().get(0).toString() : "";
                    result.put(key, value);
                }
                //this.promptMessages = result;
                return result;
            }
        }
        return Map.of();
    }
}