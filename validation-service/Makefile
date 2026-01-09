.PHONY: install run test lint format clean docker-build docker-run

install:
\tpip install -r requirements.txt

run:
\tuvicorn app.main:app --reload --host 0.0.0.0 --port 8000

test:
\tpytest tests/ -v --cov=app --cov-report=html

lint:
\tflake8 app tests
\tmypy app

format:
\tblack app tests
\tisort app tests

clean:
\tfind . -type d -name __pycache__ -exec rm -rf {} +
\tfind . -type f -name "*.pyc" -delete
\trm -rf .pytest_cache .coverage htmlcov

docker-build:
\tdocker build -t validation-service:latest .

docker-run:
\tdocker-compose up -d

docker-stop:
\tdocker-compose down

init-db:
\tpython scripts/init_db.py

run-consumer:
\tpython scripts/run_consumer.py