import React, { useState } from "react";
import ReactMarkdown from 'react-markdown';
import { authFetch } from '../authFetch';

export default function SemanticSearch() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const handleSearch = async () => {
    if (!query.trim()) return;
    
    setIsLoading(true);
    try {
      const res = await authFetch(
        `/llm/search/similar?text=${encodeURIComponent(query)}&topK=5`
      );
      const data = await res.json();
      
      // Ensure results is always an array
      if (Array.isArray(data)) {
        setResults(data);
      } else if (data && Array.isArray(data.results)) {
        setResults(data.results);
      } else if (data && Array.isArray(data.data)) {
        setResults(data.data);
      } else {
        console.warn('Unexpected response format:', data);
        setResults([]);
      }
    } catch (error) {
      console.error('Search failed:', error);
      setResults([]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="p-4 bg-white rounded shadow mt-4">
      <input
        className="border p-2 rounded w-2/3"
        value={query}
        onChange={e => setQuery(e.target.value)}
        placeholder="Enter text for semantic search..."
        disabled={isLoading}
      />
      <button
        className={`ml-2 px-4 py-2 rounded ${
          isLoading 
            ? 'bg-gray-400 cursor-not-allowed' 
            : 'bg-blue-600 hover:bg-blue-700'
        } text-white`}
        onClick={handleSearch}
        disabled={isLoading}
      >
        {isLoading ? (
          <span className="flex items-center">
            <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Searching...
          </span>
        ) : (
          'Search'
        )}
      </button>
      
      {isLoading && (
        <div className="mt-4 text-center text-gray-600">
          <div className="inline-flex items-center">
            <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-blue-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Searching for results...
          </div>
        </div>
      )}
      
      <ul className="mt-4">
        {Array.isArray(results) && results.length > 0 ? (
          results.map((r, i) => (
            <li key={i} className="border-b py-2">
              <div className="font-bold">Chunk: {r.id}</div>
              <div><ReactMarkdown>{r.text}</ReactMarkdown>{r.text}</div>
              <div className="text-xs text-gray-500">{r.filename}</div>
            </li>
          ))
        ) : !isLoading && query && (
          <li className="text-gray-500 text-center py-4">
            No results found for "{query}"
          </li>
        )}
      </ul>
    </div>
  );
} 