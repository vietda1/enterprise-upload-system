import apiService from './api.service';
import { API_CONFIG } from '../config/api.config';

class AuthService {
  async login(username, password) {
    const response = await apiService.client.post(
      `${API_CONFIG.SERVICES.USER}/auth/login`,
      { username, password }
    );
    
    localStorage.setItem('access_token', response.data.accessToken);
    localStorage.setItem('refresh_token', response.data.refreshToken);
    localStorage.setItem('user', JSON.stringify(response.data.user));
    
    return response.data;
  }

  async register(userData) {
    const response = await apiService.client.post(
      `${API_CONFIG.SERVICES.USER}/auth/register`,
      userData
    );
    
    localStorage.setItem('access_token', response.data.accessToken);
    localStorage.setItem('refresh_token', response.data.refreshToken);
    localStorage.setItem('user', JSON.stringify(response.data.user));
    
    return response.data;
  }

  async logout() {
    try {
      await apiService.client.post(`${API_CONFIG.SERVICES.USER}/auth/logout`);
    } finally {
      localStorage.clear();
    }
  }

  getCurrentUser() {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  }

  isAuthenticated() {
    return !!localStorage.getItem('access_token');
  }
}

export default new AuthService();