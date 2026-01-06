import requests
import time
import sys

KEYCLOAK_URL = "http://localhost:8180"
ADMIN_USER = "admin"
ADMIN_PASS = "admin"
REALM = "enterprise"

# Client roles for microservices client
CLIENT_ROLES = [
    {"name": "MICROSERVICES_ADMIN", "description": "Administrator role for microservices client"},
    {"name": "MICROSERVICES_USER", "description": "Standard user role for microservices client"}
]

# Microservices admin user configuration
MICROSERVICES_ADMIN_USER = {
    "username": "microservices-admin",
    "email": "microservices-admin@example.com",
    "firstName": "Microservices",
    "lastName": "Admin",
    "enabled": True,
    "emailVerified": True,
    "credentials": [{"type": "password", "value": "microservices-admin123", "temporary": False}]
}


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


def get_client_uuid(token, client_id):
    """Get internal UUID of a client by clientId"""
    url = f"{KEYCLOAK_URL}/admin/realms/{REALM}/clients?clientId={client_id}"
    headers = {"Authorization": f"Bearer {token}"}
    res = requests.get(url, headers=headers)
    if res.status_code == 200 and len(res.json()) > 0:
        return res.json()[0]["id"]
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


def create_client_roles(token, client_uuid):
    """Create client roles for microservices client"""
    url = f"{KEYCLOAK_URL}/admin/realms/{REALM}/clients/{client_uuid}/roles"
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    
    for role in CLIENT_ROLES:
        # Check if role exists
        check_res = requests.get(f"{url}/{role['name']}", headers=headers)
        if check_res.status_code == 200:
            print(f"Client role '{role['name']}' already exists.")
            continue
        
        # Create role
        res = requests.post(url, json=role, headers=headers)
        if res.status_code == 201:
            print(f"Successfully created client role '{role['name']}'")
        else:
            print(f"Failed to create client role '{role['name']}': {res.text}")


def get_client_role(token, client_uuid, role_name):
    """Get client role by name"""
    url = f"{KEYCLOAK_URL}/admin/realms/{REALM}/clients/{client_uuid}/roles/{role_name}"
    headers = {"Authorization": f"Bearer {token}"}
    res = requests.get(url, headers=headers)
    if res.status_code == 200:
        return res.json()
    return None


def get_user_by_username(token, username):
    """Get user by username"""
    url = f"{KEYCLOAK_URL}/admin/realms/{REALM}/users?username={username}&exact=true"
    headers = {"Authorization": f"Bearer {token}"}
    res = requests.get(url, headers=headers)
    if res.status_code == 200 and len(res.json()) > 0:
        return res.json()[0]
    return None


def create_microservices_admin_user(token, client_uuid):
    """Create microservices-admin user and assign MICROSERVICES_ADMIN role"""
    url = f"{KEYCLOAK_URL}/admin/realms/{REALM}/users"
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    
    # Check if user exists
    existing_user = get_user_by_username(token, MICROSERVICES_ADMIN_USER["username"])
    
    if existing_user:
        print(f"User '{MICROSERVICES_ADMIN_USER['username']}' already exists.")
        user_id = existing_user["id"]
    else:
        # Create user
        res = requests.post(url, json=MICROSERVICES_ADMIN_USER, headers=headers)
        if res.status_code == 201:
            print(f"Successfully created user '{MICROSERVICES_ADMIN_USER['username']}'")
            # Get created user's ID
            user = get_user_by_username(token, MICROSERVICES_ADMIN_USER["username"])
            if user:
                user_id = user["id"]
            else:
                print("Failed to get created user ID")
                return
        else:
            print(f"Failed to create user: {res.text}")
            return
    
    # Assign MICROSERVICES_ADMIN role to user
    assign_client_role_to_user(token, user_id, client_uuid, "MICROSERVICES_ADMIN")


def assign_client_role_to_user(token, user_id, client_uuid, role_name):
    """Assign a client role to a user"""
    role = get_client_role(token, client_uuid, role_name)
    if not role:
        print(f"Client role '{role_name}' not found")
        return
    
    url = f"{KEYCLOAK_URL}/admin/realms/{REALM}/users/{user_id}/role-mappings/clients/{client_uuid}"
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    
    res = requests.post(url, json=[role], headers=headers)
    if res.status_code == 204:
        print(f"Successfully assigned role '{role_name}' to user")
    elif res.status_code == 409:
        print(f"Role '{role_name}' already assigned to user")
    else:
        print(f"Failed to assign role '{role_name}': {res.text}")


def main():
    print("Waiting for Keycloak to be ready...")
    for i in range(30):
        token = get_admin_token()
        if token:
            print("Keycloak is ready.")
            
            # Step 1: Create/update microservices client
            create_client(token)
            
            # Step 2: Get client UUID
            client_uuid = get_client_uuid(token, "microservices")
            if not client_uuid:
                print("Failed to get microservices client UUID")
                sys.exit(1)
            
            # Step 3: Create client roles
            create_client_roles(token, client_uuid)
            
            # Step 4: Create microservices-admin user and assign role
            create_microservices_admin_user(token, client_uuid)
            
            print("\n✅ Setup completed successfully!")
            print("=" * 50)
            print("Microservices Admin User:")
            print(f"  Username: {MICROSERVICES_ADMIN_USER['username']}")
            print(f"  Password: {MICROSERVICES_ADMIN_USER['credentials'][0]['value']}")
            print(f"  Email: {MICROSERVICES_ADMIN_USER['email']}")
            print(f"  Client Role: MICROSERVICES_ADMIN")
            print("=" * 50)
            return
        time.sleep(2)
    print("Timed out waiting for Keycloak.")
    sys.exit(1)


if __name__ == "__main__":
    main()

