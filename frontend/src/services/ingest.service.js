import apiService from './api.service';
import { API_CONFIG } from '../config/api.config';

class IngestService {
  // Trigger ingestion
  async triggerIngestion(uploadId) {
    const response = await apiService.client.post(
      `${API_CONFIG.SERVICES.INGEST}/api/v1/ingest/trigger`,
      { uploadId }
    );
    return response.data;
  }

  // Get ingestion status
  async getIngestionStatus(uploadId) {
    const response = await apiService.client.get(
      `${API_CONFIG.SERVICES.INGEST}/api/v1/ingest/${uploadId}/status`
    );
    return response.data;
  }

  // Get ingestion history
  async getIngestionHistory(uploadId) {
    const response = await apiService.client.get(
      `${API_CONFIG.SERVICES.INGEST}/api/v1/ingest/${uploadId}/history`
    );
    return response.data;
  }
}

export default new IngestService();