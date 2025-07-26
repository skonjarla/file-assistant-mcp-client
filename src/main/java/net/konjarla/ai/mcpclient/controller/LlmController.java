package net.konjarla.ai.mcpclient.controller;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.konjarla.ai.mcpclient.llm.LlmService;
import net.konjarla.ai.mcpclient.llm.model.VectorItem;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/llm")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin("*")
public class LlmController {

    @NonNull
    private LlmService llmService;

    @NonNull
    private List<McpSyncClient> mcpSyncClients;

    // Non-streaming endpoint
    @PostMapping("/chat")
    public String chat(
            @RequestParam String prompt,
            @RequestParam(required = false) String systemPrompt,
            @RequestBody(required = false) List<String> history
    ) {
        return llmService.toolChat(prompt, systemPrompt, history);
        // return llmService.chat(prompt, systemPrompt, history);
    }

    // Streaming endpoint
    @PostMapping(value = "/chat/stream", produces = "text/event-stream")
    public Flux<String> chatStream(
            @RequestParam String prompt,
            @RequestParam(required = false) String systemPrompt,
            @RequestBody(required = false) List<String> history
    ) {
        return llmService.chatStream(prompt, systemPrompt, history);
    }

    @GetMapping("/search/similar")
    public List<VectorItem> findSimilar(@RequestParam(value = "text") String query) {
        return llmService.vectorChat(query);
    }

    @GetMapping("/tools")
    public List<McpSchema.Tool> tools() {
        if(!mcpSyncClients.isEmpty()) {
            McpSyncClient mcpSyncClient = mcpSyncClients.get(0);
            if(!mcpSyncClient.listTools().tools().isEmpty()) {
                return mcpSyncClient.listTools().tools();
            }
            else {
                return List.of();
            }
        }
        else {
            return List.of();
        }
    }

    @GetMapping("/prompts")
    public String userPromptTemplate() {
        McpSyncClient mcpSyncClient = mcpSyncClients.get(0);
        if (mcpSyncClient != null) {
            List<McpSchema.Prompt> prompts = mcpSyncClient.listPrompts().prompts();
            if (!prompts.isEmpty()) {
                prompts.forEach(prompt -> {
                    log.info("Prompt: {}", prompt);
                    log.info("Promtp Name: {}", prompt.name());
                    String templateString = prompt.arguments().get(0).name();
                    log.info("Template String: {}", templateString);
                    McpSchema.GetPromptRequest request;
                    if(prompt.arguments().get(0).required()) {
                        request = new McpSchema.GetPromptRequest(prompt.name(),
                                Map.of(templateString, "find travel files"));
                    } else {
                        request = new McpSchema.GetPromptRequest(prompt.name(), Map.of(templateString, " "));
                    }
                    McpSchema.GetPromptResult response = mcpSyncClient.getPrompt(request);
                    log.info("Prompt: {}", mcpSyncClient.getPrompt(request).messages().get(0));
                });
            }
            return prompts.get(0).name();
        }
        return "";
    }
} 