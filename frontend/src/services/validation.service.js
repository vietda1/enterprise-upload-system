import apiService from './api.service';
import { API_CONFIG } from '../config/api.config';

class ValidationService {
  // Get validation result
  async getValidationResult(uploadId) {
    const response = await apiService.client.get(
      `${API_CONFIG.SERVICES.VALIDATE}/api/v1/validations/${uploadId}`
    );
    return response.data;
  }

  // Retry validation
  async retryValidation(uploadId) {
    return await apiService.client.post(
      `${API_CONFIG.SERVICES.VALIDATE}/api/v1/validations/${uploadId}/retry`
    );
  }
}

export default new ValidationService();