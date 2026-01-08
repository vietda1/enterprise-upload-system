# generate_jwt.py
import jwt
import datetime

# Credentials from Kong
key = "554QK16bxZbSZqXZo9ej4fiPyRiIXuJh"  # From step 3
secret = "my-secret-key"  # From step 3

# Create payload
payload = {
    'iss': key,  # Issuer (JWT key from Kong)
    'exp': datetime.datetime.utcnow() + datetime.timedelta(hours=24),  # Expires in 24 hours
    'iat': datetime.datetime.utcnow(),  # Issued at
    'sub': 'test-user',  # Subject
    'userId': 'user-123',  # Custom claims
    'department': 'finance'
}

# Generate token
token = jwt.encode(payload, secret, algorithm='HS256')
print(f"JWT Token: {token}")