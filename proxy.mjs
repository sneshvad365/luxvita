import http from 'http'
import pkg from '/tmp/node_modules/http-proxy/lib/http-proxy/index.js'
const { createProxyServer } = pkg
import fs from 'fs'
import path from 'path'

const FRONTEND_DIR = './frontend/dist/pwa'
const BACKEND      = 'http://localhost:8080'
const PORT         = 9001

const proxy = createProxyServer({})

const MIME = {
  '.html': 'text/html', '.js': 'application/javascript',
  '.css': 'text/css',   '.png': 'image/png',
  '.jpg': 'image/jpeg', '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon', '.json': 'application/json',
  '.woff': 'font/woff', '.woff2': 'font/woff2', '.ttf': 'font/ttf',
  '.webmanifest': 'application/manifest+json',
}

http.createServer((req, res) => {
  // Proxy /api/* to backend
  if (req.url.startsWith('/api/')) {
    return proxy.web(req, res, { target: BACKEND }, (e) => {
      res.writeHead(502); res.end('Backend unavailable: ' + e.message)
    })
  }

  // Serve static files
  let filePath = path.join(FRONTEND_DIR, req.url.split('?')[0])
  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    filePath = path.join(FRONTEND_DIR, 'index.html')
  }

  const ext  = path.extname(filePath)
  const mime = MIME[ext] || 'application/octet-stream'
  res.writeHead(200, { 'Content-Type': mime })
  fs.createReadStream(filePath).pipe(res)
}).listen(PORT, () => console.log(`Proxy running on http://localhost:${PORT}`))
