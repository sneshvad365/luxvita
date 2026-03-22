const http      = require('http')
const httpProxy = require('http-proxy')
const fs        = require('fs')
const path      = require('path')

const PORT = 9001

const APPS = [
  {
    prefix:   '/luxvita',
    frontend: path.join(__dirname, 'frontend/dist/pwa'),
    backend:  'http://localhost:8080',
  },
  // Add future apps here:
  // { prefix: '/otherapp', frontend: path.join(__dirname, 'otherapp/dist'), backend: 'http://localhost:9002' },
]

const MIME = {
  '.html': 'text/html', '.js': 'application/javascript',
  '.css': 'text/css',   '.png': 'image/png',
  '.jpg': 'image/jpeg', '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon', '.json': 'application/json',
  '.woff': 'font/woff', '.woff2': 'font/woff2', '.ttf': 'font/ttf',
  '.webmanifest': 'application/manifest+json',
}

const proxy = httpProxy.createProxyServer({})

http.createServer((req, res) => {
  const url = req.url

  // Redirect root to first app
  if (url === '/' || url === '') {
    res.writeHead(302, { Location: APPS[0].prefix })
    return res.end()
  }

  // Find matching app by prefix
  const app = APPS.find(a => url === a.prefix || url.startsWith(a.prefix + '/'))
  if (!app) {
    res.writeHead(404)
    return res.end('Not found')
  }

  // Strip prefix from URL for downstream routing
  const stripped = url.slice(app.prefix.length) || '/'

  // API: proxy to backend (with prefix stripped)
  if (stripped.startsWith('/api/')) {
    req.url = stripped
    return proxy.web(req, res, { target: app.backend }, (e) => {
      res.writeHead(502); res.end('Backend unavailable: ' + e.message)
    })
  }

  // Static files: serve from frontend dir, fallback to index.html
  let filePath = path.join(app.frontend, stripped.split('?')[0])
  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    filePath = path.join(app.frontend, 'index.html')
  }

  const ext  = path.extname(filePath)
  const mime = MIME[ext] || 'application/octet-stream'

  // Never cache HTML or service worker files; hashed assets can be cached forever
  const noCache = ['.html', '.webmanifest'].includes(ext) || path.basename(filePath) === 'sw.js'
  const cacheControl = noCache
    ? 'no-cache, no-store, must-revalidate'
    : 'public, max-age=31536000, immutable'

  res.writeHead(200, { 'Content-Type': mime, 'Cache-Control': cacheControl })
  fs.createReadStream(filePath).pipe(res)

}).listen(PORT, () => console.log(`Proxy on http://localhost:${PORT}`))
