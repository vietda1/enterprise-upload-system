export const API_CONFIG = {
    KONG_GATEWAY: 'http://localhost:8000',
    SERVICES: {
      UPLOAD: '/upload-service',
      USER: '/user-service',
      VALIDATE: '/validation-service',
      INGEST: '/ingest-service'
    },
    TIMEOUT: 30000
  };