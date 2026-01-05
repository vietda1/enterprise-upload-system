# Enterprise Upload System - Frontend

React-based frontend application for enterprise file upload and management.

## Features

- 🔐 JWT-based authentication
- 📤 Direct upload to MinIO using presigned URLs
- 📊 Real-time upload progress tracking
- 🔍 File search and filtering
- 📈 Dashboard with statistics
- 🎨 Modern, responsive UI
- ⚡ Fast and optimized

## Tech Stack

- React 18
- React Router v6
- Axios
- Vite
- Lucide React (icons)
- Recharts (charts)

## Prerequisites

- Node.js >= 18.0.0
- npm >= 9.0.0

## Installation

```bash
# Clone repository
git clone <repo-url>
cd frontend

# Install dependencies
npm install

# Copy environment file
cp .env.example .env

# Update .env with your configuration
```

## Development

```bash
# Start development server
npm run dev

# Application will be available at http://localhost:3000
```

## Build

```bash
# Build for production
npm run build

# Preview production build
npm run preview
```

## Docker

### Build Image

```bash
docker build -t enterprise-upload-frontend:latest .
```

### Run Container

```bash
docker run -d \
  --name frontend \
  -p 3000:80 \
  -e VITE_API_GATEWAY_URL=http://your-api-gateway:8000 \
  enterprise-upload-frontend:latest
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| VITE_API_GATEWAY_URL | API Gateway URL | http://localhost:8000 |
| VITE_APP_NAME | Application name | Enterprise Upload System |
| VITE_APP_VERSION | App version | 1.0.0 |

## Project Structure

```
src/
├── components/          # React components
│   ├── UploadForm.jsx
│   ├── FileList.jsx
│   └── Auth/
├── services/           # API services
│   ├── api.service.js
│   ├── auth.service.js
│   └── upload.service.js
├── hooks/              # Custom React hooks
├── config/             # Configuration files
├── App.jsx             # Main app component
└── index.js            # Entry point
```

## API Integration

The frontend communicates with backend services through Kong API Gateway:

- `/user-service/auth/*` - Authentication
- `/upload-service/api/v1/uploads/*` - Upload management
- `/validate-service/api/v1/validations/*` - Validation
- `/ingest-service/api/v1/ingest/*` - Ingestion

## Testing

```bash
# Run tests
npm test

# Run tests with coverage
npm test -- --coverage
```

## Deployment

### Production Build

```bash
npm run build
```

### Deploy to Cloud

#### AWS S3 + CloudFront

```bash
# Build
npm run build

# Upload to S3
aws s3 sync dist/ s3://your-bucket-name

# Invalidate CloudFront cache
aws cloudfront create-invalidation --distribution-id YOUR_DIST_ID --paths "/*"
```

#### Nginx

```bash
# Build
npm run build

# Copy to nginx directory
sudo cp -r dist/* /var/www/html/
```

## Troubleshooting

### CORS Issues

Ensure Kong Gateway has CORS plugin configured:

```yaml
plugins:
  - name: cors
    config:
      origins:
        - http://localhost:3000
        - https://yourdomain.com
      credentials: true
```

### API Connection Failed

Check:
1. Kong Gateway is running
2. Backend services are healthy
3. Network connectivity
4. VITE_API_GATEWAY_URL is correct

## License

Proprietary - Enterprise License