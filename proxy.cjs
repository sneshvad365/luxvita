const http      = require('http')
const httpProxy = require('/tmp/node_modules/http-proxy')
const fs        = require('fs')
const path      = require('path')

const FRONTEND_DIR = path.join(__dirname, 'frontend/dist/pwa')
const BACKEND      = 'http://localhost:8080'
const PORT         = 9001

const proxy = httpProxy.createProxyServer({})

const MIME = {
  '.html': 'text/html', '.js': 'application/javascript',
  '.css': 'text/css',   '.png': 'image/png',
  '.jpg': 'image/jpeg', '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon', '.json': 'application/json',
  '.woff': 'font/woff', '.woff2': 'font/woff2', '.ttf': 'font/ttf',
  '.webmanifest': 'application/manifest+json',
}

http.createServer((req, res) => {
  if (req.url.startsWith('/api/')) {
    return proxy.web(req, res, { target: BACKEND }, (e) => {
      res.writeHead(502); res.end('Backend unavailable: ' + e.message)
    })
  }

  let filePath = path.join(FRONTEND_DIR, req.url.split('?')[0])
  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    filePath = path.join(FRONTEND_DIR, 'index.html')
  }

  const ext  = path.extname(filePath)
  const mime = MIME[ext] || 'application/octet-stream'
  res.writeHead(200, { 'Content-Type': mime })
  fs.createReadStream(filePath).pipe(res)
}).listen(PORT, () => console.log(`Proxy on http://localhost:${PORT}`))
