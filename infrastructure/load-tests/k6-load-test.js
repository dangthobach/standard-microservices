import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  stages: [
    { duration: '30s', target: 50 }, // Ramp up to 50 users
    { duration: '1m', target: 50 },  // Stay at 50 users
    { duration: '30s', target: 100 }, // Ramp up to 100 users
    { duration: '1m', target: 100 },  // Stay at 100 users
    { duration: '30s', target: 0 },   // Scala down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests must complete below 500ms
  },
};

export default function () {
  // Replace with a valid endpoint that doesn't require authentication for initial testing
  // OR use the Login flow if needed (more complex). 
  // For now, hitting actuator health or a public endpoint if available.
  // Assuming Actuator is open based on config.
  
  let res = http.get('http://localhost:8080/actuator/health');

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
