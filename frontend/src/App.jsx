import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import Login from './components/Auth/Login';
import Register from './components/Auth/Register';
import Dashboard from './components/Dashboard/Dashboard';
import UploadPage from './components/Upload/UploadPage';
import FilesPage from './components/Files/FilesPage';
import authService from './services/auth.service';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check if user is authenticated on mount
    const checkAuth = () => {
      const authenticated = authService.isAuthenticated();
      setIsAuthenticated(authenticated);
      setLoading(false);
    };

    checkAuth();
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <Router>
      <div className="min-h-screen bg-gray-50">
        <Toaster position="top-right" />
        
        <Routes>
          {/* Public Routes */}
          <Route 
            path="/login" 
            element={
              isAuthenticated ? 
                <Navigate to="/dashboard" /> : 
                <Login setIsAuthenticated={setIsAuthenticated} />
            } 
          />
          <Route 
            path="/register" 
            element={
              isAuthenticated ? 
                <Navigate to="/dashboard" /> : 
                <Register setIsAuthenticated={setIsAuthenticated} />
            } 
          />

          {/* Protected Routes */}
          <Route 
            path="/dashboard" 
            element={
              isAuthenticated ? 
                <Dashboard setIsAuthenticated={setIsAuthenticated} /> : 
                <Navigate to="/login" />
            } 
          />
          <Route 
            path="/upload" 
            element={
              isAuthenticated ? 
                <UploadPage setIsAuthenticated={setIsAuthenticated} /> : 
                <Navigate to="/login" />
            } 
          />
          <Route 
            path="/files" 
            element={
              isAuthenticated ? 
                <FilesPage setIsAuthenticated={setIsAuthenticated} /> : 
                <Navigate to="/login" />
            } 
          />

          {/* Default Route */}
          <Route 
            path="/" 
            element={<Navigate to={isAuthenticated ? "/dashboard" : "/login"} />} 
          />
        </Routes>
      </div>
    </Router>
  );
}

export default App;