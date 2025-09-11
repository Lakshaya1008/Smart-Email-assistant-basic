import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8081';

// Create axios instance with default config
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 seconds timeout
});

// Request interceptor for logging
apiClient.interceptors.request.use(
  (config) => {
    console.log('Making API request:', config.method?.toUpperCase(), config.url);
    console.log('Request data:', config.data);
    return config;
  },
  (error) => {
    console.error('Request error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => {
    console.log('API Response:', response.data);
    return response;
  },
  (error) => {
    console.error('API Error:', error);
    
    if (error.code === 'ECONNABORTED') {
      throw new Error('Request timeout - please try again');
    }
    
    if (error.response) {
      // Server responded with error status
      const { status, data } = error.response;
      switch (status) {
        case 400:
          throw new Error(data.message || 'Invalid request data');
        case 500:
          throw new Error(data.error || 'Server error - please try again later');
        default:
          throw new Error(data.message || `HTTP Error ${status}`);
      }
    } else if (error.request) {
      // Network error
      throw new Error('Network error - please check your connection and ensure the backend is running on port 8081');
    } else {
      throw new Error('Unexpected error occurred');
    }
  }
);

export const generateEmailReply = async (emailData) => {
  try {
    const response = await apiClient.post('/api/email/generate', emailData);
    return response.data;
  } catch (error) {
    throw error;
  }
};

// Test API connection
export const testApiConnection = async () => {
  try {
    const response = await apiClient.get('/api/email/test');
    return response.data;
  } catch (error) {
    throw error;
  }
};