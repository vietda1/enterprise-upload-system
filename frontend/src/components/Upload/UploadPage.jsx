import React from 'react';
import Header from '../Layout/Header';
import UploadForm from './UploadForm';

const UploadPage = ({ setIsAuthenticated }) => {
  return (
    <div className="min-h-screen bg-gray-50">
      <Header setIsAuthenticated={setIsAuthenticated} />
      
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Upload Files</h1>
          <p className="mt-2 text-gray-600">Upload and manage your files securely</p>
        </div>

        <UploadForm />
      </div>
    </div>
  );
};

export default UploadPage;