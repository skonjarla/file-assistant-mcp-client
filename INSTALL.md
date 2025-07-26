# MCP Client Installation Guide

This guide provides comprehensive instructions for setting up, configuring, and running the MCP Client for File Assistant. The MCP Client is a Spring Boot and React application that provides a chat and semantic search interface for interacting with a Model Context Protocol (MCP) server, enabling intelligent file search and question-answering capabilities.

---

## What is Model Context Protocol (MCP)?

**Model Context Protocol (MCP)** is an open standard for connecting AI models and agentic applications to external tools, data sources, and APIs. MCP enables AI applications to dynamically discover, select, and orchestrate tools and resources, breaking down data silos and facilitating interoperability across diverse systems. MCP is sometimes called the “USB-C port for AI applications” due to its role in standardizing and simplifying AI-to-tool integrations.

**In this project:**
- The backend acts as an MCP client, connecting to one or more MCP servers for semantic search, tool invocation, and prompt management.
- The backend securely communicates with MCP servers to:
  - Discover available tools and prompts
  - Perform semantic search and file operations
  - Invoke external APIs and resources via MCP tools

**MCP Security Best Practices:**
- All MCP requests are authenticated using OAuth2 access tokens (managed by Keycloak).
- MCP servers must validate all inbound requests and only accept tokens issued specifically for them.
- Never accept or forward tokens not explicitly issued for the MCP server.
- Strictly validate and sanitize all tool parameters and inputs.
- Enforce least privilege, role-based access control, and audit all tool invocations.
- All endpoints should be served over HTTPS in production.

For more, see the [MCP Security Best Practices](https://modelcontextprotocol.io/specification/draft/basic/security_best_practices).

---

## Prerequisites

### System Requirements
- **Java**: 17 or higher
- **Maven**: 3.6.3 or higher
- **Node.js**: 16+ (for frontend development)
- **npm**: 8.x or higher (comes with Node.js)
- **Ollama**: Latest version (for local model inference)
- **OpenAI/Groq API key**: For cloud-based models
- **Keycloak Server**: For authentication (v20+ recommended)
- **MCP Server**: For file search and tool invocation

### Memory and Disk Space
- **RAM**: Minimum 8GB (16GB recommended for better performance)
- **Disk Space**: At least 2GB free (more required for local models)
- **CPU**: 4+ cores recommended for local model inference

### Supported Platforms
- **Operating Systems**: Linux, macOS, Windows (WSL2 recommended for Windows)
- **Browsers**: Latest versions of Chrome, Firefox, Safari, or Edge

---

## Installation Steps

### 1. Clone the Repository

```bash
# Clone the repository
git clone <repository-url>
cd mcpclient

# Verify Java and Maven versions
java -version
mvn -v
```

### 2. Backend Setup

#### 2.1 Install Dependencies

```bash
# Install Maven dependencies
mvn clean install
```

#### 2.2 Configuration

1. **Environment Variables**:
   - Set your OpenAI/Groq API key and other variables as needed:
   ```bash
   export OPENAI_API_KEY=your_openai_api_key
   export MCP_SERVER_URL=http://localhost:8080
   export KEYCLOAK_ISSUER_URI=http://localhost:9090/realms/home
   export KEYCLOAK_CLIENT_ID=mcpclient
   export KEYCLOAK_CLIENT_SECRET=your_client_secret
   export OLLAMA_HOST=http://localhost:11434
   ```

2. **Configure `application.properties`**
   The main configuration file is at `src/main/resources/application.properties`. Example MCP configuration:
   ```properties
   # MCP Client
   spring.ai.mcp.client.enabled=true
   spring.ai.mcp.client.name=file-search-mcp-client
   spring.ai.mcp.client.version=1.0.0
   spring.ai.mcp.client.request-timeout=30s
   spring.ai.mcp.client.type=SYNC
   spring.ai.mcp.client.sse.connections.server.url=${MCP_SERVER_URL:http://localhost:8080}
   
   # Keycloak OAuth2 Configuration
   spring.security.oauth2.client.provider.authserver.issuer-uri=${KEYCLOAK_ISSUER_URI:http://localhost:9090/realms/home}
   spring.security.oauth2.client.registration.authserver-client-credentials.client-id=${KEYCLOAK_CLIENT_ID:mcpclient}
   spring.security.oauth2.client.registration.authserver-client-credentials.client-secret=${KEYCLOAK_CLIENT_SECRET:}
   
   # Ollama Configuration
   ollama.host=${OLLAMA_HOST:http://localhost:11434}
   ollama.chat.model=qwen3:14b
   
   # OpenAI/Groq Configuration
   spring.ai.openai.base-url=https://api.groq.com/openai
   spring.ai.openai.api-key=${OPENAI_API_KEY}
   spring.ai.openai.chat.options.model=qwen/qwen3-32b
   spring.ai.openai.chat.options.maxCompletionTokens=32768
   ```

3. **Keycloak Setup**
   - Set up a Keycloak server (version 20+ recommended)
   - Create a new realm or use existing
   - Create a client with `Client ID: mcpclient`
   - Configure valid redirect URIs
   - Note down the client secret

4. **MCP Server**
   - Ensure you have a running MCP server instance
   - Update the `spring.ai.mcp.client.sse.connections.server.url` with the correct URL

#### 2.3 Build the Application

```bash
# Build the application
mvn clean package
# The JAR file will be created at:
# target/mcpclient-0.0.1-SNAPSHOT.jar
```

### 3. Frontend Setup

#### 3.1 Install Dependencies

```bash
# Navigate to the frontend directory
cd file-search-ui
npm install
```

#### 3.2 Configuration

The frontend is pre-configured to connect to the backend at `http://localhost:8383`. To modify this, edit `file-search-ui/.env`:
```
REACT_APP_API_URL=http://localhost:8383
REACT_APP_KEYCLOAK_URL=http://localhost:9090
REACT_APP_KEYCLOAK_REALM=home
REACT_APP_KEYCLOAK_CLIENT_ID=mcpclient
```

#### 3.3 Build the Frontend

```bash
# Development build
npm run build
# Production build (optimized)
NODE_ENV=production npm run build
```

---

## Running the Application

### 4.1 Running in Development Mode

#### Backend

```bash
# From the project root directory
mvn spring-boot:run
```
The backend will be available at `http://localhost:8383`

#### Frontend

In a new terminal:
```bash
cd file-search-ui
npm start
```
The frontend will open automatically in your default browser at `http://localhost:3000`

### 4.2 Running in Production Mode

1. **Build the application**:
   ```bash
   mvn clean package -DskipTests
   ```
2. **Run the application**:
   ```bash
   OPENAI_API_KEY=your_key_here java -jar target/mcpclient-*.jar
   ```
3. **Access the application**:
   - Frontend: `http://localhost:8383` (served by the backend)
   - API: `http://localhost:8383/api`
   - Actuator: `http://localhost:8383/actuator`

### 4.3 Using Docker (Optional)

1. **Build the Docker image**:
   ```bash
   docker build -t mcpclient .
   ```
2. **Run the container**:
   ```bash
   docker run -p 8383:8383 \
     -e OPENAI_API_KEY=your_key_here \
     -e MCP_SERVER_URL=http://host.docker.internal:8080 \
     -e KEYCLOAK_ISSUER_URI=http://host.docker.internal:9090/realms/home \
     mcpclient
   ```
   Note: On Linux, use `--network="host"` instead of `-p` for better performance.

---

## 5. Advanced Configuration

### 5.1 Using Local Models with Ollama

1. **Install Ollama**:
   - [Download and install](https://ollama.ai/) for your platform
   - Start the Ollama service:
     ```bash
     ollama serve
     ```
2. **Download Models**:
   ```bash
   ollama pull qwen3:14b
   ollama pull qwen2.5vl
   ```
3. **Verify Installation**:
   ```bash
   curl http://localhost:11434/api/tags
   ```
4. **Configure in `application.properties`**:
   ```properties
   ollama.host=http://localhost:11434
   ollama.chat.model=qwen3:14b
   ```

### 5.2 Configuring Cloud Models

#### Groq
1. Sign up at [Groq Cloud](https://console.groq.com/)
2. Get your API key
3. Configure in environment variables or `application.properties`:
   ```bash
   export OPENAI_API_KEY=your_groq_api_key
   ```

#### OpenAI
1. Sign up at [OpenAI](https://platform.openai.com/)
2. Get your API key
3. Configure in `application.properties`:
   ```properties
   spring.ai.openai.base-url=https://api.openai.com
   spring.ai.openai.api-key=${OPENAI_API_KEY}
   spring.ai.openai.chat.options.model=gpt-4-turbo
   ```

### 5.3 Environment Variables Reference

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `SERVER_PORT` | Port for the backend server | No | 8383 |
| `OPENAI_API_KEY` | API key for OpenAI/Groq | Yes (cloud) | - |
| `OLLAMA_HOST` | Ollama server URL | No | http://localhost:11434 |
| `MCP_SERVER_URL` | MCP server URL | Yes | http://localhost:8080 |
| `KEYCLOAK_*` | Keycloak configuration | Yes | - |
| `SPRING_PROFILES_ACTIVE` | Active profiles | No | - |

---

## 6. Verifying MCP Connectivity and Tool Discovery

1. **Check MCP server status:**
   - Ensure your MCP server is running and accessible at the configured URL.
2. **Verify tool discovery:**
   - Use the `/llm/tools` endpoint to list available MCP tools:
     ```bash
     curl -H "Authorization: Bearer <access_token>" http://localhost:8383/llm/tools
     ```
   - You should see a list of available tools from the MCP server.
3. **Check prompt templates:**
   - Use the `/llm/prompts` endpoint to list available prompts.

---

## 7. Troubleshooting

### Common Issues

#### 7.1 Authentication Problems
- **Symptom**: 401 Unauthorized errors
- **Solution**:
  1. Verify Keycloak server is running
  2. Check client credentials in `application.properties`
  3. Ensure proper realm and client configuration in Keycloak

#### 7.2 Connection to MCP Server Fails
- **Symptom**: Connection refused or timeouts
- **Solution**:
  1. Verify MCP server is running
  2. Check `spring.ai.mcp.client.sse.connections.server.url`
  3. Ensure network connectivity between services

#### 7.3 Model Loading Issues
- **Symptom**: Model not found errors
- **Solution**:
  1. For Ollama: Verify model is downloaded (`ollama list`)
  2. For cloud providers: Check API key and model name

#### 7.4 Frontend Not Connecting to Backend
- **Symptom**: CORS errors in browser console
- **Solution**:
  1. Check `REACT_APP_API_URL` in frontend `.env`
  2. Verify backend CORS configuration
  3. Ensure both services are running

#### 7.5 MCP Tool Discovery Issues
- **Symptom**: `/llm/tools` returns empty or error
- **Solution**:
  1. Ensure MCP server is running and has tools registered
  2. Check backend logs for errors
  3. Verify OAuth2 token is valid and has correct scopes

### 7.6 Logs and Debugging

#### View Logs
```bash
# Basic logs
tail -f logs/application.log
```

#### Enable Debug Logging
Add to `application.properties`:
```properties
logging.level.root=INFO
logging.level.net.konjarla.ai.mcpclient=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.security=DEBUG
```

---

## 8. Upgrading

1. **Backup your configuration**:
   ```bash
   cp src/main/resources/application.properties application.properties.bak
   ```
2. **Update the code**:
   ```bash
   git pull origin main
   ```
3. **Rebuild the application**:
   ```bash
   mvn clean package
   ```
4. **Restart the service**:
   ```bash
   java -jar target/mcpclient-*.jar
   ```

---

## 9. Uninstallation

1. **Stop the application**
   ```bash
   # Find the process
   ps aux | grep mcpclient
   kill <process_id>
   ```
2. **Remove installation files**
   ```bash
   rm -rf /path/to/mcpclient
   ```
3. **Clean up dependencies**
   ```bash
   rm -rf ~/.m2/repository/net/konjarla/ai/mcpclient
   rm -rf file-search-ui/node_modules
   ```

---

## 10. Getting Help

- **Documentation**: Check the [project wiki](<repository-url>/wiki)
- **Issues**: File a [GitHub issue](<repository-url>/issues)
- **Community**: Join our [Discord/Slack channel](<community-link>)
- **MCP Protocol Docs**: [https://modelcontextprotocol.io/](https://modelcontextprotocol.io/)

---

## 11. Security

### Reporting Security Issues

Please report security issues to security@example.com. We appreciate your help in making our software more secure.

### Security Best Practices

1. **Secrets Management**:
   - Never commit API keys or credentials to version control
   - Use environment variables or a secrets management service
2. **Network Security**:
   - Use HTTPS in production
   - Configure proper CORS settings
   - Enable firewall rules to restrict access
3. **Authentication**:
   - Use strong passwords for all accounts
   - Regularly rotate API keys and credentials
   - Implement proper session management
4. **Updates**:
   - Keep all dependencies up to date
   - Regularly apply security patches
   - Monitor for security advisories for all dependencies

---

## 12. License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
