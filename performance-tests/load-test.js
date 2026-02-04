import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const productCreationTime = new Trend('product_creation_time');
const approvalTime = new Trend('approval_time');

// Load test configuration
export const options = {
    stages: [
        // Warm-up
        { duration: '30s', target: 10 },

        // Ramp up to 100 users
        { duration: '1m', target: 100 },
        { duration: '3m', target: 100 },

        // Ramp up to 500 users
        { duration: '1m', target: 500 },
        { duration: '5m', target: 500 },

        // Ramp up to 1000 users (stress test)
        { duration: '1m', target: 1000 },
        { duration: '10m', target: 1000 },

        // Ramp down
        { duration: '1m', target: 0 },
    ],
    thresholds: {
        // Performance targets
        'http_req_duration': ['p(95)<500', 'p(99)<1000'], // 95% < 500ms, 99% < 1000ms
        'http_req_failed': ['rate<0.05'],                 // Error rate < 5%
        'errors': ['rate<0.05'],
        'product_creation_time': ['p(95)<800'],           // Product creation < 800ms
        'approval_time': ['p(95)<600'],                   // Approval < 600ms
    },
};

const BASE_URL = __ENV.K6_TARGET_URL || 'http://localhost:8080';

// Test data generators
function generateProductData(iteration) {
    return JSON.stringify({
        name: `Load Test Product ${__VU}-${iteration}`,
        sku: `SKU-${__VU}-${iteration}-${Date.now()}`,
        description: `Performance test product created by VU ${__VU} in iteration ${iteration}`,
        price: Math.floor(Math.random() * 10000) / 100, // Random price 0-100
        stockQuantity: Math.floor(Math.random() * 1000) + 1,
        category: ['ELECTRONICS', 'CLOTHING', 'FOOD', 'BOOKS'][Math.floor(Math.random() * 4)],
    });
}

// Authentication helper
function getAuthToken() {
    // In real scenario, get token from Keycloak
    // For now, return mock or use environment variable
    return __ENV.AUTH_TOKEN || 'mock-token';
}

export default function () {
    const token = getAuthToken();
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
    };

    // Test 1: Create Product (triggers workflow)
    const createStart = Date.now();
    const createRes = http.post(
        `${BASE_URL}/api/products`,
        generateProductData(__ITER),
        { headers }
    );

    const createDuration = Date.now() - createStart;
    productCreationTime.add(createDuration);

    const createSuccess = check(createRes, {
        'product created': (r) => r.status === 200 || r.status === 201,
        'creation time < 1s': () => createDuration < 1000,
    });

    if (!createSuccess) {
        errorRate.add(1);
        console.error(`Product creation failed: ${createRes.status} - ${createRes.body}`);
    } else {
        errorRate.add(0);
    }

    sleep(0.5);

    // Test 2: Get Products List (cache hit test)
    const listRes = http.get(
        `${BASE_URL}/api/products?page=0&size=20`,
        { headers }
    );

    check(listRes, {
        'get products successful': (r) => r.status === 200,
        'response time < 200ms': (r) => r.timings.duration < 200,
    });

    sleep(0.3);

    // Test 3: Get Product by ID (if creation succeeded)
    if (createSuccess && createRes.json) {
        const productId = createRes.json('id');
        if (productId) {
            const getRes = http.get(
                `${BASE_URL}/api/products/${productId}`,
                { headers }
            );

            check(getRes, {
                'get product by id successful': (r) => r.status === 200,
                'cached response < 100ms': (r) => r.timings.duration < 100,
            });
        }
    }

    sleep(0.5);

    // Test 4: Search Products (database query test)
    const searchRes = http.get(
        `${BASE_URL}/api/products?status=PENDING_APPROVAL&page=0&size=10`,
        { headers }
    );

    check(searchRes, {
        'search products successful': (r) => r.status === 200,
        'search time < 300ms': (r) => r.timings.duration < 300,
    });

    sleep(1);
}

// Setup function (runs once per VU)
export function setup() {
    console.log(`Starting load test against: ${BASE_URL}`);
    console.log('Target: 1000 concurrent users');
    console.log('Duration: ~23 minutes total');

    // Verify API is accessible
    const healthCheck = http.get(`${BASE_URL}/actuator/health`);
    if (healthCheck.status !== 200) {
        throw new Error(`API not ready: ${healthCheck.status}`);
    }

    return { startTime: Date.now() };
}

// Teardown function (runs once after all VUs finish)
export function teardown(data) {
    const duration = (Date.now() - data.startTime) / 1000;
    console.log(`Load test completed in ${duration.toFixed(2)} seconds`);
}
