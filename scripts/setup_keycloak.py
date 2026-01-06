import requests
import time
import sys

KEYCLOAK_URL = "http://localhost:8180"
ADMIN_USER = "admin"
ADMIN_PASS = "admin"
REALM = "enterprise"

def get_admin_token():
    url = f"{KEYCLOAK_URL}/realms/master/protocol/openid-connect/token"
    payload = {
        "client_id": "admin-cli",
        "username": ADMIN_USER,
        "password": ADMIN_PASS,
        "grant_type": "password"
    }
    try:
        response = requests.post(url, data=payload)
        response.raise_for_status()
        return response.json()["access_token"]
    except Exception as e:
        print(f"Error getting admin token: {e}")
        return None

def create_client(token):
    url = f"{KEYCLOAK_URL}/admin/realms/{REALM}/clients"
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    
    client_data = {
        "clientId": "microservices",
        "name": "Microservices Frontend",
        "description": "Public client for Microservices Frontend with PKCE",
        "enabled": True,
        "publicClient": True,
        "protocol": "openid-connect",
        "standardFlowEnabled": True,
        "directAccessGrantsEnabled": True,
        "rootUrl": "http://localhost:4200",
        "baseUrl": "http://localhost:4200",
        "redirectUris": ["http://localhost:4200/*", "http://localhost/*"],
        "webOrigins": ["*"],
        "attributes": {
            "pkce.code.challenge.method": "S256"
        }
    }
    
    # Check if exists
    get_res = requests.get(f"{url}?clientId=microservices", headers=headers)
    if get_res.status_code == 200 and len(get_res.json()) > 0:
        print("Client 'microservices' already exists. Updating...")
        client_id = get_res.json()[0]["id"]
        update_res = requests.put(f"{url}/{client_id}", json=client_data, headers=headers)
        if update_res.status_code == 204:
            print("Successfully updated client 'microservices'")
        else:
            print(f"Failed to update client: {update_res.text}")
    else:
        print("Creating client 'microservices'...")
        res = requests.post(url, json=client_data, headers=headers)
        if res.status_code == 201:
            print("Successfully created client 'microservices'")
        else:
            print(f"Failed to create client: {res.text}")

def main():
    print("Waiting for Keycloak to be ready...")
    for i in range(30):
        token = get_admin_token()
        if token:
            print("Keycloak is ready.")
            create_client(token)
            return
        time.sleep(2)
    print("Timed out waiting for Keycloak.")
    sys.exit(1)

if __name__ == "__main__":
    main()
