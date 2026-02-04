import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

// Stress test - push system beyond normal limits
export const options = {
    stages: [
        // Quick ramp to 500 users
        { duration: '1m', target: 500 },

        // Ramp to 1500 users (50% over capacity)
        { duration: '2m', target: 1500 },
        { duration: '5m', target: 1500 },

        // Push to 2000 users (2x capacity)
        { duration: '1m', target: 2000 },
        { duration: '3m', target: 2000 },

        // Recovery test - drop to 100
        { duration: '1m', target: 100 },
        { duration: '2m', target: 100 },

        // Ramp down
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        // More lenient thresholds for stress test
        'http_req_duration': ['p(95)<2000', 'p(99)<5000'],
        'http_req_failed': ['rate<0.15'], // Allow 15% error rate
        'errors': ['rate<0.15'],
    },
};

const BASE_URL = __ENV.K6_TARGET_URL || 'http://localhost:8080';

export default function () {
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${__ENV.AUTH_TOKEN || 'mock-token'}`,
    };

    const payload = JSON.stringify({
        name: `Stress Test ${__VU}-${__ITER}`,
        sku: `STRESS-${__VU}-${__ITER}-${Date.now()}`,
        price: 99.99,
        stockQuantity: 100,
    });

    const res = http.post(`${BASE_URL}/api/products`, payload, { headers });

    const success = check(res, {
        'status is 2xx': (r) => r.status >= 200 && r.status < 300,
    });

    errorRate.add(!success ? 1 : 0);

    // Minimal sleep to maximize stress
    sleep(0.1);
}
