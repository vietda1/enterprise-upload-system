import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Upload, FileText, CheckCircle, Clock, TrendingUp } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import Header from '../Layout/Header';
import uploadService from '../../services/upload.service';
import toast from 'react-hot-toast';

const Dashboard = ({ setIsAuthenticated }) => {
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    totalUploads: 0,
    completedUploads: 0,
    pendingUploads: 0,
    totalSize: 0
  });
  const [recentUploads, setRecentUploads] = useState([]);
  const [chartData, setChartData] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      const uploads = await uploadService.listUploads({ limit: 10 });
      
      // Calculate stats
      const totalUploads = uploads.length;
      const completedUploads = uploads.filter(u => u.status === 'COMPLETED').length;
      const pendingUploads = uploads.filter(u => ['PENDING', 'UPLOADING', 'VALIDATING'].includes(u.status)).length;
      const totalSize = uploads.reduce((sum, u) => sum + (u.fileSize || 0), 0);

      setStats({ totalUploads, completedUploads, pendingUploads, totalSize });
      setRecentUploads(uploads.slice(0, 5));

      // Chart data - uploads by day
      const last7Days = [...Array(7)].map((_, i) => {
        const date = new Date();
        date.setDate(date.getDate() - (6 - i));
        return date.toISOString().split('T')[0];
      });

      const chartData = last7Days.map(date => ({
        date: new Date(date).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
        uploads: uploads.filter(u => u.createdAt?.startsWith(date)).length
      }));

      setChartData(chartData);
    } catch (error) {
      toast.error('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
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
      FAILED: 'bg-red-100 text-red-800'
    };
    return badges[status] || 'bg-gray-100 text-gray-800';
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Header setIsAuthenticated={setIsAuthenticated} />
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="text-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
            <p className="mt-4 text-gray-600">Loading dashboard...</p>
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
          <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
          <p className="mt-2 text-gray-600">Overview of your uploads and system status</p>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
          <div className="bg-white rounded-xl shadow-sm border p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500 font-medium">Total Uploads</p>
                <p className="text-3xl font-bold text-gray-900 mt-2">{stats.totalUploads}</p>
              </div>
              <div className="bg-blue-100 p-3 rounded-lg">
                <FileText className="h-6 w-6 text-blue-600" />
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm border p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500 font-medium">Completed</p>
                <p className="text-3xl font-bold text-gray-900 mt-2">{stats.completedUploads}</p>
              </div>
              <div className="bg-green-100 p-3 rounded-lg">
                <CheckCircle className="h-6 w-6 text-green-600" />
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm border p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500 font-medium">Pending</p>
                <p className="text-3xl font-bold text-gray-900 mt-2">{stats.pendingUploads}</p>
              </div>
              <div className="bg-yellow-100 p-3 rounded-lg">
                <Clock className="h-6 w-6 text-yellow-600" />
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm border p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500 font-medium">Total Size</p>
                <p className="text-3xl font-bold text-gray-900 mt-2">{formatFileSize(stats.totalSize)}</p>
              </div>
              <div className="bg-purple-100 p-3 rounded-lg">
                <TrendingUp className="h-6 w-6 text-purple-600" />
              </div>
            </div>
          </div>
        </div>

        {/* Chart */}
        <div className="bg-white rounded-xl shadow-sm border p-6 mb-8">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Upload Activity (Last 7 Days)</h2>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="uploads" fill="#3B82F6" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Recent Uploads */}
        <div className="bg-white rounded-xl shadow-sm border">
          <div className="p-6 border-b flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900">Recent Uploads</h2>
            <button
              onClick={() => navigate('/files')}
              className="text-sm text-blue-600 hover:text-blue-700 font-medium"
            >
              View All
            </button>
          </div>
          
          {recentUploads.length === 0 ? (
            <div className="p-12 text-center">
              <FileText className="h-12 w-12 text-gray-300 mx-auto mb-4" />
              <p className="text-gray-500">No uploads yet</p>
              <button
                onClick={() => navigate('/upload')}
                className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              >
                Upload Your First File
              </button>
            </div>
          ) : (
            <div className="divide-y">
              {recentUploads.map((upload) => (
                <div key={upload.id} className="p-6 hover:bg-gray-50 transition-colors">
                  <div className="flex items-center justify-between">
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900 truncate">{upload.fileName}</p>
                      <div className="mt-1 flex items-center space-x-4 text-sm text-gray-500">
                        <span>{formatFileSize(upload.fileSize)}</span>
                        <span>•</span>
                        <span>{upload.department}</span>
                        <span>•</span>
                        <span>{new Date(upload.createdAt).toLocaleDateString()}</span>
                      </div>
                    </div>
                    <div className="ml-4">
                      <span className={`inline-flex px-3 py-1 text-xs font-medium rounded-full ${getStatusBadge(upload.status)}`}>
                        {upload.status}
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Quick Actions */}
        <div className="mt-8 grid grid-cols-1 md:grid-cols-2 gap-6">
          <button
            onClick={() => navigate('/upload')}
            className="bg-blue-600 text-white p-6 rounded-xl shadow-sm hover:bg-blue-700 transition-colors text-left"
          >
            <Upload className="h-8 w-8 mb-3" />
            <h3 className="text-lg font-semibold">Upload New File</h3>
            <p className="mt-2 text-blue-100 text-sm">Start uploading your files to the system</p>
          </button>

          <button
            onClick={() => navigate('/files')}
            className="bg-white border-2 border-gray-200 p-6 rounded-xl shadow-sm hover:border-blue-300 transition-colors text-left"
          >
            <FileText className="h-8 w-8 text-gray-600 mb-3" />
            <h3 className="text-lg font-semibold text-gray-900">View All Files</h3>
            <p className="mt-2 text-gray-600 text-sm">Browse and manage your uploaded files</p>
          </button>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;