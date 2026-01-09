# Validation Service

Python FastAPI microservice for validating uploaded files.

## Features

- 📊 Multi-format validation (CSV, JSON, Parquet)
- ✅ Schema validation
- 📈 Data quality scoring
- 🔍 Constraint checking
- 📝 Detailed error reporting
- 🎯 Kafka event-driven architecture

## Tech Stack

- Python 3.11
- FastAPI
- SQLAlchemy + PostgreSQL
- Kafka (python-kafka)
- MinIO
- Pandas, PyArrow

## Installation

```bash
# Create virtual environment
python -m venv venv
source venv/bin/activate  # Linux/Mac
venv\\Scripts\\activate  # Windows

# Install dependencies
pip install -r requirements.txt

# Copy environment file
cp .env.example .env

# Edit .env with your configuration
nano .env
```

## Database Setup

```bash
# Initialize database
python scripts/init_db.py

# Or use Alembic
alembic upgrade head
```

## Running

### Development
```bash
# Run API server
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Or use make
make run
```

### Production
```bash
# With Gunicorn
gunicorn app.main:app -w 4 -k uvicorn.workers.UvicornWorker --bind 0.0.0.0:8000
```

### Docker
```bash
# Build
docker build -t validation-service:latest .

# Run
docker run -d -p 8000:8000 \\
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \\
  -e MINIO_ENDPOINT=minio:9000 \\
  validation-service:latest

# With Docker Compose
docker-compose up -d
```

## API Endpoints

### Trigger Validation
```http
POST /api/v1/validations/trigger
Content-Type: application/json

{
  "upload_id": "uuid",
  "object_key": "path/to/file.csv",
  "dataset_type": "CSV_TRANSACTION"
}
```

### Get Validation Result
```http
GET /api/v1/validations/{upload_id}
```

### Retry Validation
```http
POST /api/v1/validations/{upload_id}/retry
```

### List Validations
```http
GET /api/v1/validations?status=VALID&limit=20&offset=0
```

### Get Statistics
```http
GET /api/v1/validations/stats/summary
```

### Health Check
```http
GET /health
```

## Supported Dataset Types

- `CSV_TRANSACTION` - Transaction data
- `JSON_USER_PROFILE` - User profiles
- `PARQUET_ANALYTICS` - Analytics events
- `CSV_SALES` - Sales records
- `CSV_INVENTORY` - Inventory data

## Validation Rules

Rules are defined in `app/rules/validation_rules.py`:

```python
{
    "required_columns": ["col1", "col2"],
    "column_types": {
        "col1": "string",
        "col2": "numeric"
    },
    "constraints": {
        "col2": {"min": 0, "max": 1000}
    }
}
```

## Testing

```bash
# Run tests
pytest tests/ -v

# With coverage
pytest tests/ --cov=app --cov-report=html

# Or use make
make test
```

## Project Structure

```
validation-service/
├── app/
│   ├── __init__.py
│   ├── main.py
│   ├── config.py
│   ├── database.py
│   ├── models.py
│   ├── schemas.py
│   ├── api/
│   │   └── endpoints.py
│   ├── services/
│   │   ├── file_validator.py
│   │   ├── kafka_service.py
│   │   └── validation_service.py
│   ├── validators/
│   │   ├── base_validator.py
│   │   ├── csv_validator.py
│   │   ├── json_validator.py
│   │   └── parquet_validator.py
│   └── rules/
│       └── validation_rules.py
├── tests/
│   ├── conftest.py
│   ├── test_validators.py
│   └── test_api.py
├── scripts/
│   ├── init_db.py
│   └── run_consumer.py
├── alembic/
│   └── versions/
├── requirements.txt
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── Makefile
└── README.md
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| KAFKA_BOOTSTRAP_SERVERS | Kafka servers | localhost:9092 |
| MINIO_ENDPOINT | MinIO endpoint | localhost:9000 |
| POSTGRES_HOST | PostgreSQL host | localhost |
| POSTGRES_DB | Database name | upload_db |
| MAX_FILE_SIZE_MB | Max file size | 500 |

## Monitoring

### Logs
```bash
# View logs
docker-compose logs -f validation-service

# Or in Kubernetes
kubectl logs -f deployment/validation-service
```

### Metrics
- Total validations
- Success rate
- Average processing time
- Error rates

## Troubleshooting

### Kafka Connection Issues
```bash
# Test Kafka connectivity
docker exec validation-service python -c "from kafka import KafkaConsumer; print('OK')"
```

### MinIO Connection Issues
```bash
# Test MinIO connectivity
docker exec validation-service python -c "from minio import Minio; print('OK')"
```

### Database Issues
```bash
# Check database connection
docker exec validation-service python -c "from app.database import engine; print(engine.connect())"
```

## License

Proprietary - Enterprise License