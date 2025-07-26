import React, { useState, useRef } from "react";
import ReactMarkdown from 'react-markdown';
import { authFetch } from '../authFetch';

// Loading dots component
const LoadingDots = () => (
  <div className="flex space-x-1 items-center">
    <div className="w-2 h-2 bg-gray-600 rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></div>
    <div className="w-2 h-2 bg-gray-600 rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></div>
    <div className="w-2 h-2 bg-gray-600 rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></div>
  </div>
);

export default function LlmChat() {
  const [prompt, setPrompt] = useState("");
  const [systemPrompt, setSystemPrompt] = useState("");
  const [history, setHistory] = useState<string[]>([]);
  const [response, setResponse] = useState("");
  const [streaming, setStreaming] = useState(false);
  const [loading, setLoading] = useState(false);
  const streamRef = useRef<EventSource | null>(null);

  const handleChat = async () => {
    if (!prompt.trim()) return;
    
    setLoading(true);
    setStreaming(false);
    setResponse("");
    
    try {
      const res = await authFetch(
          `/llm/chat?prompt=${encodeURIComponent(prompt)}&systemPrompt=${encodeURIComponent(systemPrompt)}`,

        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(history),
        }
      );
      const responseText = await res.text();
      setResponse(responseText);
      setHistory([...history, prompt]);
    } catch (error) {
      setResponse("Error: Failed to get response from the server.");
    } finally {
      setLoading(false);
    }
  };

  const handleStream = async () => {
    if (!prompt.trim()) return;
    setStreaming(true);
    setLoading(true);
    setResponse("");

    // Get token for SSE
    let token = "";
    try {
      // @ts-ignore
      token = await import('../keycloak').then(mod => mod.default.updateToken(5).then(() => mod.default.token));
    } catch (e) {
      setResponse("Error: Could not get auth token for streaming.");
      setLoading(false);
      setStreaming(false);
      return;
    }

    const eventSource = new EventSource(
      `/llm/chat/stream?prompt=${encodeURIComponent(prompt)}&systemPrompt=${encodeURIComponent(systemPrompt)}&authToken=${encodeURIComponent(token)}`
    );
    
    eventSource.onmessage = (e) => {
      setResponse((prev) => prev + e.data);
      setLoading(false);
    };
    
    eventSource.onerror = () => {
      eventSource.close();
      setLoading(false);
      setStreaming(false);
    };
    
    streamRef.current = eventSource;
    setHistory([...history, prompt]);
  };

  return (
    <div className="p-4 bg-white rounded shadow mt-4">
      <input
        className="border p-2 rounded w-full mb-2"
        value={systemPrompt}
        onChange={e => setSystemPrompt(e.target.value)}
        placeholder="System prompt (optional)"
      />
      <textarea
        className="border p-2 rounded w-full mb-2"
        value={prompt}
        onChange={e => setPrompt(e.target.value)}
        placeholder="Your message..."
        disabled={loading || streaming}
      />
      <div>
        <button
          className={`px-4 py-2 text-white rounded mr-2 ${
            loading && !streaming ? 'bg-gray-400 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700'
          }`}
          onClick={handleChat}
          disabled={loading || streaming}
        >
          {loading && !streaming ? (
            <div className="flex items-center">
              <LoadingDots />
              <span className="ml-2">Processing...</span>
            </div>
          ) : (
            "Send (Non-Streaming)"
          )}
        </button>
        <button
          className={`px-4 py-2 text-white rounded ${
            streaming ? 'bg-gray-400 cursor-not-allowed' : 'bg-green-600 hover:bg-green-700'
          }`}
          onClick={handleStream}
          disabled={loading || streaming}
        >
          {streaming ? (
            <div className="flex items-center">
              <LoadingDots />
              <span className="ml-2">Streaming...</span>
            </div>
          ) : (
            "Send (Streaming)"
          )}
        </button>
      </div>
      
      {/* Response area with loading indicator */}
      <div className="mt-4 prose max-w-none">
        {loading && !response && (
          <div className="flex items-center space-x-2 text-gray-600 mb-4">
            <LoadingDots />
            <span>AI is thinking...</span>
          </div>
        )}
        {response && <ReactMarkdown>{response}</ReactMarkdown>}
      </div>
    </div>
  );
} 