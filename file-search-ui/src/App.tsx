import React, { useState, useRef } from "react";
import LlmChat from "./components/LlmChat";
import SemanticSearch from "./components/SemanticSearch";
import { Routes, Route, Navigate } from 'react-router-dom';
import { useKeycloak } from '@react-keycloak/web';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { keycloak, initialized } = useKeycloak();
  if (!initialized) return null; // or a spinner
  if (!keycloak?.authenticated) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

function LoginPage() {
  const { keycloak, initialized } = useKeycloak();

  if (!initialized) {
    // Optionally show a loading spinner here
    return null;
  }

  if (keycloak?.authenticated) {
    // Already logged in, redirect to main app
    return <Navigate to="/" replace />;
  }

  const handleLogin = () => keycloak?.login();
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gray-100">
      <div className="bg-white p-8 rounded shadow">
        <h2 className="text-2xl font-bold mb-4">Login</h2>
        <button
          className="px-4 py-2 bg-blue-600 text-white rounded"
          onClick={handleLogin}
        >
          Login with Keycloak
        </button>
      </div>
    </div>
  );
}

function UserMenu() {
  const { keycloak } = useKeycloak();
  const [open, setOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  // Get user info from token
  const username = keycloak?.tokenParsed?.preferred_username || keycloak?.tokenParsed?.email || 'User';
  const avatarLetter = username.charAt(0).toUpperCase();

  // Close dropdown on outside click
  React.useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    if (open) {
      document.addEventListener('mousedown', handleClickOutside);
    } else {
      document.removeEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [open]);

  return (
    <div className="absolute top-6 right-8 z-50" ref={menuRef}>
      <button
        className="flex items-center space-x-2 focus:outline-none"
        onClick={() => setOpen((v) => !v)}
        aria-label="User menu"
      >
        <span className="w-9 h-9 rounded-full bg-blue-600 text-white flex items-center justify-center text-lg font-bold">
          {avatarLetter}
        </span>
        <span className="font-medium text-gray-700">{username}</span>
        <svg className="w-4 h-4 ml-1 text-gray-500" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" /></svg>
      </button>
      {open && (
        <div className="absolute right-0 mt-2 w-40 bg-white border rounded shadow-lg py-2">
          <button
            className="block w-full text-left px-4 py-2 text-gray-700 hover:bg-gray-100"
            onClick={() => keycloak?.logout({ redirectUri: window.location.origin + '/login' })}
          >
            Logout
          </button>
        </div>
      )}
    </div>
  );
}

function MainApp() {
  return (
    <div className="min-h-screen bg-gray-100 p-8 relative">
      <UserMenu />
      <h1 className="text-3xl font-bold mb-6">Personal Files Assistant</h1>
      <SemanticSearch />
      <LlmChat />
    </div>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/*" element={
        <ProtectedRoute>
          <MainApp />
        </ProtectedRoute>
      } />
    </Routes>
  );
} 