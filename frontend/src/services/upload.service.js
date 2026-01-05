import apiService from './api.service';
import { API_CONFIG } from '../config/api.config';

class UploadService {
  // Get presigned URL from Upload Management Service
  async getPresignedUrl(fileName, fileType, metadata) {
    const response = await apiService.client.post(
      `${API_CONFIG.SERVICES.UPLOAD}/api/v1/uploads/presigned-url`,
      {
        fileName,
        fileType,
        metadata: {
          department: metadata.department,
          accessLevel: metadata.accessLevel,
          tags: metadata.tags,
          description: metadata.description,
          targetDatabase: metadata.targetDatabase,
          datasetType: metadata.datasetType
        }
      }
    );
    return response.data;
  }

  // Upload file directly to MinIO using presigned URL
  async uploadToMinio(presignedUrl, file, onProgress) {
    return await axios.put(presignedUrl, file, {
      headers: {
        'Content-Type': file.type
      },
      onUploadProgress: (progressEvent) => {
        const progress = Math.round(
          (progressEvent.loaded * 100) / progressEvent.total
        );
        if (onProgress) onProgress(progress);
      }
    });
  }

  // Confirm upload and trigger validation
  async confirmUpload(uploadId) {
    const response = await apiService.client.post(
      `${API_CONFIG.SERVICES.UPLOAD}/api/v1/uploads/${uploadId}/confirm`
    );
    return response.data;
  }

  // Get upload status
  async getUploadStatus(uploadId) {
    const response = await apiService.client.get(
      `${API_CONFIG.SERVICES.UPLOAD}/api/v1/uploads/${uploadId}/status`
    );
    return response.data;
  }

  // List user uploads
  async listUploads(filters = {}) {
    const params = new URLSearchParams(filters);
    const response = await apiService.client.get(
      `${API_CONFIG.SERVICES.UPLOAD}/api/v1/uploads?${params}`
    );
    return response.data;
  }

  // Delete upload
  async deleteUpload(uploadId) {
    return await apiService.client.delete(
      `${API_CONFIG.SERVICES.UPLOAD}/api/v1/uploads/${uploadId}`
    );
  }

  // Get dataset configurations
  async getDatasetConfigs() {
    const response = await apiService.client.get(
      `${API_CONFIG.SERVICES.UPLOAD}/api/v1/configs/datasets`
    );
    return response.data;
  }
}

export default new UploadService();