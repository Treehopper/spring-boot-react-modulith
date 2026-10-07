import { useEffect, useState } from 'react'
import { getGreeting } from './api'

function App() {
  const [greeting, setGreeting] = useState<string>('Loading…')

  useEffect(() => {
    // Generated from openapi/modulith-api.yaml. Same origin as the page, so no CORS configuration is needed.
    getGreeting({ throwOnError: true })
      .then(({ data }) => setGreeting(data.message))
      .catch((error: unknown) => setGreeting(`Backend unreachable: ${String(error)}`))
  }, [])

  return (
    <main>
      <h1>{greeting}</h1>
      <p>React frontend served by Spring Boot.</p>
    </main>
  )
}

export default App
