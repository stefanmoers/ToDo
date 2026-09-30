import { defineConfig } from '@playwright/test'

// Die Tests laufen gegen einen bereits gestarteten kompletten Stack (siehe scripts/e2e.sh im Repository-Stamm).
export default defineConfig({
  testDir: './e2e',
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8080',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    locale: 'de-DE',
  },
})
