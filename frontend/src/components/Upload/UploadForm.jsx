import React, { useState, useEffect, useRef } from 'react';
import { Upload as UploadIcon, X, CheckCircle, AlertCircle, File } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useUpload } from '../../hooks/useUpload';
import uploadService from '../../services/upload.service';

const UploadForm = () => {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);
  const [files, setFiles] = useState([]);
  const [datasetConfigs, setDatasetConfigs] = useState([]);
  const [metadata, setMetadata] = useState({
    department: '',
    accessLevel: 'private',
    tags: [],
    description: '',
    datasetType: '',
    targetDatabase: '',
    autoIngest: false
  });
  
  const { uploadFile, uploading, progress, errors } = useUpload();

  useEffect(() => {
    loadDatasetConfigs();
  }, []);

  const loadDatasetConfigs = async () => {
    try {
      const configs = await uploadService.getDatasetConfigs();
      setDatasetConfigs(configs);
    } catch (error) {
      toast.error('Failed to load dataset configurations');
    }
  };

  const handleFileSelect = (e) => {
    const selectedFiles = Array.from(e.target.files);
    setFiles(selectedFiles);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    const droppedFiles = Array.from(e.dataTransfer.files);
    setFiles(droppedFiles);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
  };

  const removeFile = (index) => {
    setFiles(files.filter((_, i) => i !== index));
  };

  const handleMetadataChange = (field, value) => {
    setMetadata(prev => ({ ...prev, [field]: value }));
    
    if (field === 'datasetType') {
      const config = datasetConfigs.find(c => c.type === value);
      if (config) {
        setMetadata(prev => ({ ...prev, targetDatabase: config.defaultDatabase }));
      }
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (files.length === 0) {
      toast.error('Please select at least one file');
      return;
    }

    if (!metadata.department) {
      toast.error('Please select a department');
      return;
    }

    if (!metadata.datasetType) {
      toast.error('Please select a dataset type');
      return;
    }

    try {
      const uploadPromises = files.map(file => uploadFile(file, metadata));
      await Promise.all(uploadPromises);
      
      toast.success('All files uploaded successfully!');
      setFiles([]);
      setTimeout(() => navigate('/files'), 1500);
    } catch (error) {
      console.error('Upload failed:', error);
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border">
      <form onSubmit={handleSubmit} className="p-6 space-y-6">
        {/* File Drop Zone */}
        <div
          onDrop={handleDrop}
          onDragOver={handleDragOver}
          onClick={() => fileInputRef.current?.click()}
          className="border-2 border-dashed border-gray-300 rounded-xl p-12 text-center hover:border-blue-500 transition-colors cursor-pointer bg-gray-50"
        >
          <UploadIcon className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <p className="text-lg font-medium text-gray-900 mb-2">
            Click to upload or drag and drop
          </p>
          <p className="text-sm text-gray-500">
            Support for PDF, CSV, JSON, Parquet, Excel files
          </p>
          <input
            ref={fileInputRef}
            type="file"
            multiple
            onChange={handleFileSelect}
            className="hidden"
          />
        </div>

        {/* Selected Files */}
        {files.length > 0 && (
          <div className="space-y-2">
            <h3 className="text-sm font-medium text-gray-900">Selected Files ({files.length})</h3>
            {files.map((file, index) => (
              <div key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div className="flex items-center space-x-3">
                  <File className="h-5 w-5 text-blue-600" />
                  <div>
                    <p className="text-sm font-medium text-gray-900">{file.name}</p>
                    <p className="text-xs text-gray-500">
                      {(file.size / 1024 / 1024).toFixed(2)} MB
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => removeFile(index)}
                  className="text-red-500 hover:text-red-700"
                  disabled={uploading}
                >
                  <X className="h-5 w-5" />
                </button>
              </div>
            ))}
          </div>
        )}

        {/* Metadata Form */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Department *
            </label>
            <select
              value={metadata.department}
              onChange={(e) => handleMetadataChange('department', e.target.value)}
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="">Select department</option>
              <option value="hr">Human Resources</option>
              <option value="finance">Finance</option>
              <option value="tech">Technology</option>
              <option value="marketing">Marketing</option>
              <option value="sales">Sales</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Dataset Type *
            </label>
            <select
              value={metadata.datasetType}
              onChange={(e) => handleMetadataChange('datasetType', e.target.value)}
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="">Select dataset type</option>
              {datasetConfigs.map(config => (
                <option key={config.type} value={config.type}>
                  {config.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Access Level
            </label>
            <select
              value={metadata.accessLevel}
              onChange={(e) => handleMetadataChange('accessLevel', e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="private">Private</option>
              <option value="shared">Shared (Department)</option>
              <option value="public">Public</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Target Database
            </label>
            <input
              type="text"
              value={metadata.targetDatabase}
              onChange={(e) => handleMetadataChange('targetDatabase', e.target.value)}
              placeholder="Auto-filled based on dataset type"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              readOnly
            />
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Description (Optional)
          </label>
          <textarea
            value={metadata.description}
            onChange={(e) => handleMetadataChange('description', e.target.value)}
            rows="3"
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            placeholder="Add a description for this upload..."
          />
        </div>

        <div className="flex items-center">
          <input
            type="checkbox"
            id="autoIngest"
            checked={metadata.autoIngest}
            onChange={(e) => handleMetadataChange('autoIngest', e.target.checked)}
            className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
          />
          <label htmlFor="autoIngest" className="ml-2 block text-sm text-gray-700">
            Auto-ingest after validation
          </label>
        </div>

        {/* Progress */}
        {uploading && Object.keys(progress).length > 0 && (
          <div className="space-y-3">
            {Object.entries(progress).map(([fileId, prog]) => (
              <div key={fileId}>
                <div className="flex items-center justify-between text-sm mb-1">
                  <span className="text-gray-700">Uploading...</span>
                  <span className="text-gray-600">{prog}%</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div
                    className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                    style={{ width: `${prog}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Errors */}
        {Object.keys(errors).length > 0 && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <div className="flex items-start">
              <AlertCircle className="h-5 w-5 text-red-600 mt-0.5" />
              <div className="ml-3">
                <h3 className="text-sm font-medium text-red-800">Upload Errors</h3>
                <div className="mt-2 text-sm text-red-700">
                  {Object.entries(errors).map(([fileId, error]) => (
                    <p key={fileId}>• {error}</p>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Submit Button */}
        <div className="flex justify-end space-x-3">
          <button
            type="button"
            onClick={() => navigate('/dashboard')}
            className="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
            disabled={uploading}
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={uploading || files.length === 0}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors flex items-center"
          >
            {uploading ? (
              <>
                <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Uploading...
              </>
            ) : (
              <>
                <UploadIcon className="h-5 w-5 mr-2" />
                Upload {files.length} file(s)
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
};

export default UploadForm;