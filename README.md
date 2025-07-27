# MCP Client for File Assistant

A Spring Boot application that provides a chat interface for interacting with a Model Context Protocol (MCP) server, enabling intelligent file search and question-answering capabilities over your personal or organizational documents. This client supports multiple LLM backends including local models via Ollama and cloud-based models through OpenAI-compatible APIs like Groq.

---

## What is Model Context Protocol (MCP)?

**Model Context Protocol (MCP)** is an open standard for connecting AI models and agentic applications to external tools, data sources, and APIs. MCP enables AI applications to dynamically discover, select, and orchestrate tools and resources, breaking down data silos and facilitating interoperability across diverse systems. MCP is sometimes called the “USB-C port for AI applications” due to its role in standardizing and simplifying AI-to-tool integrations.

### MCP Architecture
- **MCP Host:** The AI application (e.g., this client) that provides the environment for executing AI-based tasks.
- **MCP Client:** The intermediary that manages communication between the host and one or more MCP servers.
- **MCP Server:** Exposes tools, resources, and prompts to the client, enabling access to external systems and operations.

### How MCP is used in this project
This client acts as an MCP client and host, connecting to one or more MCP servers for semantic search, tool invocation, and prompt management. It leverages the [Spring AI MCP client](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html) for backend integration. The backend securely communicates with MCP servers to:
- Discover available tools and prompts
- Perform semantic search and file operations
- Invoke external APIs and resources via MCP tools

### MCP Configuration Example
In `src/main/resources/application.properties`:
```properties
# MCP Client
spring.ai.mcp.client.enabled=true
spring.ai.mcp.client.name=file-search-mcp-client
spring.ai.mcp.client.version=1.0.0
spring.ai.mcp.client.request-timeout=30s
spring.ai.mcp.client.type=SYNC
spring.ai.mcp.client.sse.connections.server.url=http://localhost:8080
```
- **MCP Server URL:** Set `spring.ai.mcp.client.sse.connections.server.url` to your MCP server endpoint.
- **MCP Client Name/Version:** Used for identification and tool registration.
- **Timeouts and Type:** Configure as needed for your environment.

### MCP Security Best Practices
- **OAuth2.1:** All MCP requests are authenticated using OAuth2 access tokens, managed by Keycloak.
- **Token Handling:** Access tokens are never passed in query strings; always in the `Authorization: Bearer <token>` header.
- **Authorization:** MCP servers must validate all inbound requests and only accept tokens issued specifically for them.
- **Token Passthrough:** Never accept or forward tokens not explicitly issued for the MCP server.
- **Session Management:** Avoid using sessions for authentication; use secure, non-deterministic session IDs if sessions are required.
- **Input Validation:** All tool parameters and inputs must be strictly validated and sanitized.
- **Access Control:** Enforce least privilege, role-based access control, and audit all tool invocations.
- **Consent and Privacy:** Users must explicitly consent to all data access and tool operations.
- **Transport Security:** All endpoints should be served over HTTPS in production.

For more, see the [MCP Security Best Practices](https://modelcontextprotocol.io/specification/draft/basic/security_best_practices).

---

## Features

- **Chat Interface**: Interact with your documents using natural language
- **File Search**: Search across indexed documents using semantic search
- **Streaming Responses**: Real-time streaming of AI responses with SSE (Server-Sent Events)
- **Multiple LLM Backends**: 
  - Local models via Ollama (e.g., qwen3:14b, qwen2.5vl)
  - Cloud-based models through OpenAI-compatible APIs (e.g., Groq)
- **OAuth2 Integration**: Secure authentication with Keycloak/OpenID Connect
- **REST API**: Simple HTTP endpoints for integration with other applications
- **Large Context Support**: Configured for handling large context windows (up to 32K tokens)

## Prerequisites

- Java 17 or higher
- Node.js 16+ (for frontend development)
- Maven 3.6.3+
- Ollama (optional, for local model inference)
- OpenAI/Groq API key (for cloud-based models)
- Keycloak server (for authentication)
- MCP server (for file search functionality)

## Configuration

1. Set your OpenAI/Groq API key as an environment variable:
   ```
   export OPENAI_API_KEY=your_openai_api_key
   ```

2. Configure the application properties in `src/main/resources/application.properties`:
- Keycloak server port: 9090
- Keycloak realm: home
- Keycloak client ID and secret: mcpclient and <your_client_secret>
- NOTE: Please configure the server and client credentials as per your Keycloak setup.
- Keycloak configuration:
   - Essentially, keycloak is used for authentication and authorization.
   - Following accounts are created for MCP Server and Client in keycloak.
   - For JWT token validation and for client app - mcpfileclient.
   - To obtain MCP Server details at the time of MCP Client startup - mcpclient.
   - Users with roles to login at MCP Client to access MCP Server.
     ```properties
     # Server configuration
     server.port=8383
   
     # Keycloak OAuth2 Configuration
     spring.security.oauth2.client.provider.authserver.issuer-uri=http://localhost:9090/realms/home
     spring.security.oauth2.client.registration.authserver-client-credentials.client-id=mcpclient
     spring.security.oauth2.client.registration.authserver-client-credentials.client-secret=your_client_secret
   
     # MCP Server configuration
     spring.ai.mcp.client.enabled=true
     spring.ai.mcp.client.name=file-search-mcp-client
     spring.ai.mcp.client.version=1.0.0
     spring.ai.mcp.client.request-timeout=30s
     spring.ai.mcp.client.type=SYNC
     spring.ai.mcp.client.sse.connections.server.url=http://localhost:8080
   
     # Ollama configuration (for local models)
     ollama.host=http://localhost:11434
     ollama.chat.model=qwen3:14b
     ollama.photo.chat.model=qwen2.5vl
     ollama.chat.options.temperature=0.9
   
     # OpenAI/Groq configuration
     spring.ai.openai.base-url=https://api.groq.com/openai
     spring.ai.openai.api-key=${OPENAI_API_KEY}
     spring.ai.openai.chat.options.model=qwen/qwen3-32b
     spring.ai.openai.chat.options.temperature=0.9
     spring.ai.openai.chat.options.maxCompletionTokens=32768
   
     file.assistant.system.prompt=classpath:file_assistant_system_prompt_with_tool.txt
     result.format.system.prompt=classpath:result_format_system_prompt.txt
     vector.query.template=classpath:vector_query_template.txt
     ```

3. System Prompts (optional):
   - `file.assistant.system.prompt`: Path to system prompt for file assistant
   - `result.format.system.prompt`: Path to system prompt for result formatting
   - `vector.query.template`: Path to vector query template

## Installation

### Backend

1. Build the project:
   ```bash
   mvn clean install
   ```

2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

### Frontend

1. Navigate to the frontend directory:
   ```bash
   cd file-search-ui
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm start
   ```

## API Endpoints

### Chat

- `POST /llm/chat` - Get a non-streaming chat response
  - Query Parameters:
    - `prompt` (required): The user's message
    - `systemPrompt` (optional): Custom system prompt
    - `history` (optional): List of previous messages for context
    - `temperature` (optional, default: 0.9): Controls randomness in the response
    - `maxTokens` (optional): Maximum number of tokens to generate

- `POST /llm/chat/stream` - Get a streaming chat response (SSE)
  - Same parameters as above

### Authentication

- The API is secured with OAuth2. Include a valid access token in the `Authorization` header:
  ```
  Authorization: Bearer <access_token>
  ```

### Error Handling

- `401 Unauthorized`: Missing or invalid authentication token
- `403 Forbidden`: Insufficient permissions
- `400 Bad Request`: Invalid request parameters
- `500 Internal Server Error`: Server-side error

## Architecture

The application follows a modular architecture:

### Backend
- **Framework**: Spring Boot 3.5.3
- **AI Integration**: 
  - Spring AI 1.0.0
  - Support for multiple LLM providers (Ollama, OpenAI/Groq)
  - MCP client for file search capabilities
- **Security**:
  - OAuth2 with Keycloak
  - Secure client credentials flow
- **API**: RESTful endpoints with JSON
- **Key Files**:
  - Main Application: `McpclientApplication.java`
  - Controller: `LlmController.java`
  - Service: `LlmService.java`
  - Models: `VectorItem.java`, `VectorResults.java`
  - Configuration: `OllamaConfiguration.java`, `OpenAiConfiguration.java`, `SecurityConfiguration.java`
  - Security: `McpSyncClientExchangeFilterFunction.java`
  - Transport: `BufferedClientHttpResponseWrapper.java`, `RestClientInterceptor.java`
  - Utility: `ThinkTagUtil.java`

### Frontend
- **Framework**: React with TypeScript
- **UI**: Tailwind CSS for styling
- **State Management**: React Context API
- **Real-time Updates**: Server-Sent Events (SSE) for streaming responses
- **Authentication**: Keycloak integration using `@react-keycloak/web`
- **Key Files**:
  - Entry Point: `file-search-ui/src/index.tsx`
  - App Root: `file-search-ui/src/App.tsx`
  - Components: `file-search-ui/src/components/LlmChat.tsx`, `file-search-ui/src/components/SemanticSearch.tsx`
  - Auth Utilities: `file-search-ui/src/authFetch.ts`, `file-search-ui/src/keycloak.ts`

#### Frontend Authentication & User Flow
- On app load, Keycloak authentication is checked. If not authenticated, the user is redirected to login.
- After login, the user can:
  - Perform semantic search on files (via `SemanticSearch` component)
  - Chat with the LLM (via `LlmChat` component), with support for both streaming and non-streaming responses
- All API requests are made with the Keycloak access token (handled by `authFetch.ts`)

### Integration
- **MCP Server**: For semantic search and document retrieval
- **LLM Providers**:
  - Local: Ollama (qwen3:14b, qwen2.5vl)
  - Cloud: Groq (qwen/qwen3-32b)

### Data Flow
1. User submits query through the web interface
2. Backend authenticates and processes the request
3. MCP server performs semantic search if needed
4. LLM generates response based on query and context
5. Response is streamed back to the client in real-time

## Development

### Backend Structure

- **Main Application**: `net.konjarla.ai.mcpclient.McpclientApplication`
- **Controller**: `LlmController.java`
- **Service**: `LlmService.java`
- **Models**: `VectorItem.java`, `VectorResults.java`
- **Configuration**: `OllamaConfiguration.java`, `OpenAiConfiguration.java`, `SecurityConfiguration.java`
- **Security**: `McpSyncClientExchangeFilterFunction.java`
- **Transport**: `BufferedClientHttpResponseWrapper.java`, `RestClientInterceptor.java`
- **Utility**: `ThinkTagUtil.java`

### Frontend Structure

- **Entry Point**: `file-search-ui/src/index.tsx`
- **App Root**: `file-search-ui/src/App.tsx`
- **Components**:
  - `LlmChat.tsx`: Main chat interface (streaming and non-streaming)
  - `SemanticSearch.tsx`: Semantic file search UI
- **Auth Utilities**:
  - `authFetch.ts`: Fetch wrapper with Keycloak token
  - `keycloak.ts`: Keycloak configuration

### Building and Running

1. **Backend**:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

2. **Frontend**:
   ```bash
   cd file-search-ui
   npm install
   npm start
   ```

3. **Production Build**:
   ```bash
   mvn clean package
   java -jar target/mcpclient-0.0.1-SNAPSHOT.jar
   ```

### Testing

Run the test suite with:
```bash
mvn test
```

### Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `OPENAI_API_KEY` | API key for OpenAI/Groq services | Yes (for cloud models) | - |
| `SERVER_PORT` | Port for the backend server | No | 8383 |
| `OLLAMA_HOST` | Host for Ollama server | No | http://localhost:11434 |
| `MCP_SERVER_URL` | URL for MCP server | Yes | http://localhost:8080 |

### Troubleshooting

- **Port conflicts**: Update `server.port` in `application.properties`
- **Authentication issues**: Verify Keycloak configuration and client credentials
- **Model not found**: Ensure the specified model is available in your Ollama/Groq account
- **Connection errors**: Check if all required services (MCP server, Keycloak) are running

## License

This project is licensed under the terms of the MIT license.

## Support

For issues and feature requests, please use the [issue tracker](<repository-url>/issues).

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request
