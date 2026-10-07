import { defineConfig } from '@hey-api/openapi-ts'

// Generates the typed API client from the contract shared with the backend.
export default defineConfig({
  input: '../openapi/modulith-api.yaml',
  output: 'src/api',
  plugins: ['@hey-api/client-fetch'],
})
