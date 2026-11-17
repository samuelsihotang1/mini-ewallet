// Load test for the transfer saga, run against the docker compose stack.
//
//   docker run --rm -v "$PWD/loadtest:/scripts" -e BASE_HOST=host.docker.internal \
//     grafana/k6:1.0.0 run /scripts/transfer.js
//
// Two scenarios run together for DURATION:
//   requests  POST /api/v1/transfers at RATE per second between random wallets
//   saga      a few users who each request a transfer and poll it until it is final,
//             measuring the end-to-end saga time while the system is under load
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import exec from 'k6/execution';

const HOST = __ENV.BASE_HOST || 'localhost';
const WALLET = `http://${HOST}:8081/api/v1/wallets`;
const TRANSFER = `http://${HOST}:8082/api/v1/transfers`;
const RATE = Number(__ENV.RATE || 50);
const DURATION = __ENV.DURATION || '60s';
const WALLETS = Number(__ENV.WALLETS || 200);
const JSON_HEADERS = { 'Content-Type': 'application/json' };

const sagaTime = new Trend('saga_completion_ms', true);
const sagaUnfinished = new Counter('saga_unfinished');

export const options = {
  setupTimeout: '5m',
  scenarios: {
    requests: {
      executor: 'constant-arrival-rate',
      exec: 'requestTransfer',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: 50,
      maxVUs: 300,
    },
    saga: {
      executor: 'constant-vus',
      exec: 'followSaga',
      vus: 5,
      duration: DURATION,
    },
  },
  thresholds: {
    'http_req_failed{name:transfer}': ['rate<0.01'],
    'http_req_duration{name:transfer}': ['p(95)<500'],
    'http_req_duration{name:poll}': ['p(95)<500'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  const run = Date.now().toString(36);
  const wallets = [];
  for (let i = 0; i < WALLETS; i++) {
    const opened = http.post(WALLET, JSON.stringify({ userId: `load-${run}-${i}` }),
      { headers: JSON_HEADERS });
    const walletId = opened.json('walletId');
    http.post(`${WALLET}/${walletId}/top-ups`, JSON.stringify({ amount: '1000000.00' }),
      { headers: { ...JSON_HEADERS, 'Idempotency-Key': 'seed' } });
    wallets.push(walletId);
  }
  return { run, wallets };
}

function randomPair(wallets) {
  const a = Math.floor(Math.random() * wallets.length);
  let b = Math.floor(Math.random() * (wallets.length - 1));
  if (b >= a) b++;
  return [wallets[a], wallets[b]];
}

function postTransfer(data, tag) {
  const [source, destination] = randomPair(data.wallets);
  const key = `${data.run}-${tag}-${exec.scenario.iterationInTest}-${exec.vu.idInTest}`;
  return http.post(TRANSFER,
    JSON.stringify({ sourceWalletId: source, destinationWalletId: destination, amount: '1.00' }),
    { headers: { ...JSON_HEADERS, 'Idempotency-Key': key }, tags: { name: 'transfer' } });
}

export function requestTransfer(data) {
  const response = postTransfer(data, 'r');
  check(response, { 'transfer accepted (202)': (r) => r.status === 202 });
}

export function followSaga(data) {
  const response = postTransfer(data, 's');
  if (!check(response, { 'saga transfer accepted (202)': (r) => r.status === 202 })) return;
  const id = response.json('transferId');
  const started = Date.now();
  while (Date.now() - started < 30000) {
    const status = http.get(`${TRANSFER}/${id}`, { tags: { name: 'poll' } }).json('status');
    if (status === 'COMPLETED' || status === 'FAILED' || status === 'COMPENSATED') {
      sagaTime.add(Date.now() - started);
      return;
    }
    sleep(0.05);
  }
  sagaUnfinished.add(1);
}
