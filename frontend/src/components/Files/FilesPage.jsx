import React, { useState, useEffect } from 'react';
import { Search, Filter, Download, Trash2, Eye, FileText } from 'lucide-react';
import Header from '../Layout/Header';
import uploadService from '../../services/upload.service';
import validationService from '../../services/validation.service';
import ingestService from '../../services/ingest.service';
import toast from 'react-hot-toast';

const FilesPage = ({ setIsAuthenticated }) => {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterDepartment, setFilterDepartment] = useState('all');
  const [filterStatus, setFilterStatus] = useState('all');
  const [selectedFile, setSelectedFile] = useState(null);
  const [showDetailModal, setShowDetailModal] = useState(false);

  useEffect(() => {
    loadFiles();
  }, []);

  const loadFiles = async () => {
    try {
      setLoading(true);
      const data = await uploadService.listUploads();
      setFiles(data);
    } catch (error) {
      toast.error('Failed to load files');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (fileId) => {
    if (!window.confirm('Are you sure you want to delete this file?')) {
      return;
    }

    try {
      await uploadService.deleteUpload(fileId);
      setFiles(files.filter(f => f.id !== fileId));
      toast.success('File deleted successfully');
    } catch (error) {
      toast.error('Failed to delete file');
    }
  };

  const handleViewDetails = async (file) => {
    setSelectedFile(file);
    
    // Load additional details
    try {
      const [validationData, ingestionData] = await Promise.all([
        validationService.getValidationResult(file.id).catch(() => null),
        ingestService.getIngestionStatus(file.id).catch(() => null)
      ]);
      
      setSelectedFile({
        ...file,
        validation: validationData,
        ingestion: ingestionData
      });
    } catch (error) {
      console.error('Failed to load details:', error);
    }
    
    setShowDetailModal(true);
  };

  const formatFileSize = (bytes) => {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  };

  const getStatusBadge = (status) => {
    const badges = {
      COMPLETED: 'bg-green-100 text-green-800',
      PENDING: 'bg-yellow-100 text-yellow-800',
      UPLOADING: 'bg-blue-100 text-blue-800',
      VALIDATING: 'bg-purple-100 text-purple-800',
      VALID: 'bg-green-100 text-green-800',
      INVALID: 'bg-red-100 text-red-800',
      FAILED: 'bg-red-100 text-red-800'
    };
    return badges[status] || 'bg-gray-100 text-gray-800';
  };

  // Filter files
  const filteredFiles = files.filter(file => {
    const matchesSearch = file.fileName.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesDepartment = filterDepartment === 'all' || file.department === filterDepartment;
    const matchesStatus = filterStatus === 'all' || file.status === filterStatus;
    return matchesSearch && matchesDepartment && matchesStatus;
  });

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Header setIsAuthenticated={setIsAuthenticated} />
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="text-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
            <p className="mt-4 text-gray-600">Loading files...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Header setIsAuthenticated={setIsAuthenticated} />
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">My Files</h1>
          <p className="mt-2 text-gray-600">Browse and manage your uploaded files</p>
        </div>

        {/* Search and Filters */}
        <div className="bg-white rounded-xl shadow-sm border p-6 mb-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-5 w-5" />
              <input
                type="text"
                placeholder="Search files..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
            
            <div className="relative">
              <Filter className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-5 w-5" />
              <select
                value={filterDepartment}
                onChange={(e) => setFilterDepartment(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="all">All Departments</option>
                <option value="hr">Human Resources</option>
                <option value="finance">Finance</option>
                <option value="tech">Technology</option>
                <option value="marketing">Marketing</option>
                <option value="sales">Sales</option>
              </select>
            </div>

            <div className="relative">
              <Filter className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-5 w-5" />
              <select
                value={filterStatus}
                onChange={(e) => setFilterStatus(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="all">All Status</option>
                <option value="COMPLETED">Completed</option>
                <option value="PENDING">Pending</option>
                <option value="UPLOADING">Uploading</option>
                <option value="VALIDATING">Validating</option>
                <option value="FAILED">Failed</option>
              </select>
            </div>
          </div>
        </div>

        {/* Files Grid */}
        {filteredFiles.length === 0 ? (
          <div className="bg-white rounded-xl shadow-sm border p-12 text-center">
            <FileText className="h-16 w-16 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-500 text-lg">No files found</p>
            <p className="text-gray-400 text-sm mt-2">Try adjusting your filters or upload new files</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredFiles.map(file => (
              <div key={file.id} className="bg-white rounded-xl shadow-sm border hover:shadow-md transition-shadow">
                <div className="p-6">
                  <div className="flex items-start justify-between mb-4">
                    <FileText className="h-8 w-8 text-blue-600" />
                    <span className={`px-3 py-1 text-xs font-medium rounded-full ${getStatusBadge(file.status)}`}>
                      {file.status}
                    </span>
                  </div>
                  
                  <h3 className="font-semibold text-gray-900 mb-2 truncate" title={file.fileName}>
                    {file.fileName}
                  </h3>
                  
                  <div className="space-y-2 mb-4 text-sm text-gray-600">
                    <div className="flex items-center justify-between">
                      <span className="text-gray-500">Size:</span>
                      <span className="font-medium">{formatFileSize(file.fileSize)}</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-gray-500">Department:</span>
                      <span className="font-medium capitalize">{file.department}</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-gray-500">Uploaded:</span>
                      <span className="font-medium">
                        {new Date(file.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                  </div>
                  
                  <div className="flex items-center space-x-2">
                    <button
                      onClick={() => handleViewDetails(file)}
                      className="flex-1 px-3 py-2 bg-blue-50 text-blue-600 rounded-lg text-sm font-medium hover:bg-blue-100 transition-colors flex items-center justify-center"
                    >
                      <Eye className="h-4 w-4 mr-1" />
                      Details
                    </button>
                    <button
                      onClick={() => handleDelete(file.id)}
                      className="px-3 py-2 bg-red-50 text-red-600 rounded-lg text-sm font-medium hover:bg-red-100 transition-colors"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Detail Modal */}
        {showDetailModal && selectedFile && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
              <div className="p-6 border-b flex items-center justify-between sticky top-0 bg-white">
                <h2 className="text-xl font-semibold text-gray-900">File Details</h2>
                <button
                  onClick={() => setShowDetailModal(false)}
                  className="text-gray-400 hover:text-gray-600"
                >
                  <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
              
              <div className="p-6 space-y-6">
                {/* Basic Info */}
                <div>
                  <h3 className="text-sm font-medium text-gray-500 mb-3">Basic Information</h3>
                  <div className="bg-gray-50 rounded-lg p-4 space-y-2">
                    <div className="flex justify-between">
                      <span className="text-gray-600">File Name:</span>
                      <span className="font-medium">{selectedFile.fileName}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Size:</span>
                      <span className="font-medium">{formatFileSize(selectedFile.fileSize)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Type:</span>
                      <span className="font-medium">{selectedFile.fileType}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Department:</span>
                      <span className="font-medium capitalize">{selectedFile.department}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Access Level:</span>
                      <span className="font-medium capitalize">{selectedFile.accessLevel}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Dataset Type:</span>
                      <span className="font-medium">{selectedFile.datasetType}</span>
                    </div>
                  </div>
                </div>

                {/* Validation Results */}
                {selectedFile.validation && (
                  <div>
                    <h3 className="text-sm font-medium text-gray-500 mb-3">Validation Results</h3>
                    <div className="bg-gray-50 rounded-lg p-4 space-y-2">
                      <div className="flex justify-between">
                        <span className="text-gray-600">Status:</span>
                        <span className={`px-2 py-1 text-xs font-medium rounded ${getStatusBadge(selectedFile.validation.status)}`}>
                          {selectedFile.validation.status}
                        </span>
                      </div>
                      {selectedFile.validation.rowCount && (
                        <div className="flex justify-between">
                          <span className="text-gray-600">Rows:</span>
                          <span className="font-medium">{selectedFile.validation.rowCount.toLocaleString()}</span>
                        </div>
                      )}
                      {selectedFile.validation.dataQualityScore !== undefined && (
                        <div className="flex justify-between">
                          <span className="text-gray-600">Quality Score:</span>
                          <span className="font-medium">{selectedFile.validation.dataQualityScore}/100</span>
                        </div>
                      )}
                      {selectedFile.validation.errors && selectedFile.validation.errors.length > 0 && (
                        <div className="mt-3">
                          <p className="text-sm font-medium text-red-600 mb-2">Errors:</p>
                          <ul className="text-sm text-red-600 space-y-1">
                            {selectedFile.validation.errors.map((error, i) => (
                              <li key={i}>• {error}</li>
                            ))}
                          </ul>
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {/* Ingestion Status */}
                {selectedFile.ingestion && (
                  <div>
                    <h3 className="text-sm font-medium text-gray-500 mb-3">Ingestion Status</h3>
                    <div className="bg-gray-50 rounded-lg p-4 space-y-2">
                      <div className="flex justify-between">
                        <span className="text-gray-600">Status:</span>
                        <span className={`px-2 py-1 text-xs font-medium rounded ${getStatusBadge(selectedFile.ingestion.status)}`}>
                          {selectedFile.ingestion.status}
                        </span>
                      </div>
                      {selectedFile.ingestion.rowsProcessed > 0 && (
                        <>
                          <div className="flex justify-between">
                            <span className="text-gray-600">Rows Processed:</span>
                            <span className="font-medium">{selectedFile.ingestion.rowsProcessed.toLocaleString()}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-gray-600">Rows Inserted:</span>
                            <span className="font-medium text-green-600">{selectedFile.ingestion.rowsInserted.toLocaleString()}</span>
                          </div>
                        </>
                      )}
                      <div className="flex justify-between">
                        <span className="text-gray-600">Target Database:</span>
                        <span className="font-medium text-xs">{selectedFile.targetDatabase}</span>
                      </div>
                    </div>
                  </div>
                )}

                {/* Timestamps */}
                <div>
                  <h3 className="text-sm font-medium text-gray-500 mb-3">Timeline</h3>
                  <div className="bg-gray-50 rounded-lg p-4 space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-gray-600">Created:</span>
                      <span>{new Date(selectedFile.createdAt).toLocaleString()}</span>
                    </div>
                    {selectedFile.uploadedAt && (
                      <div className="flex justify-between">
                        <span className="text-gray-600">Uploaded:</span>
                        <span>{new Date(selectedFile.uploadedAt).toLocaleString()}</span>
                      </div>
                    )}
                    {selectedFile.validatedAt && (
                      <div className="flex justify-between">
                        <span className="text-gray-600">Validated:</span>
                        <span>{new Date(selectedFile.validatedAt).toLocaleString()}</span>
                      </div>
                    )}
                    {selectedFile.ingestedAt && (
                      <div className="flex justify-between">
                        <span className="text-gray-600">Ingested:</span>
                        <span>{new Date(selectedFile.ingestedAt).toLocaleString()}</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default FilesPage;