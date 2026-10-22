// Playwright checks for the bundled demo page. Run it through `./gradlew :demo:e2eTest`, or
// directly with `node demo/e2e/demo-page.test.mjs` after a build; Playwright and Chromium come
// from the Kotlin/JS npm workspace under build/js.
import { readFile } from 'node:fs/promises'
import { createServer } from 'node:http'
import { createRequire } from 'node:module'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
const distDir = process.env.DEMO_DIST ?? path.join(repoRoot, 'demo/build/dist/js/productionExecutable')
const { chromium } = createRequire(path.join(repoRoot, 'build/js/package.json'))('playwright')

const mimeTypes = {
    '.css': 'text/css; charset=utf-8',
    '.html': 'text/html; charset=utf-8',
    '.js': 'text/javascript; charset=utf-8',
    '.png': 'image/png',
    '.svg': 'image/svg+xml',
}

const server = createServer(async (request, response) => {
    const pathname = decodeURIComponent(new URL(request.url, 'http://localhost').pathname)
    const relative = pathname === '/' ? 'index.html' : pathname.replace(/^\/+/, '')
    const filePath = path.join(distDir, relative)
    if (!filePath.startsWith(distDir + path.sep)) {
        response.writeHead(403).end()
        return
    }
    try {
        const body = await readFile(filePath)
        response.writeHead(200, { 'content-type': mimeTypes[path.extname(filePath)] ?? 'application/octet-stream' })
        response.end(body)
    } catch {
        response.writeHead(404).end('not found')
    }
})

await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
const base = `http://127.0.0.1:${server.address().port}/`

const failures = []
const check = (label, condition, detail = '') => {
    if (!condition) failures.push(`${label}${detail ? ` (${detail})` : ''}`)
    console.log(`${condition ? 'ok  ' : 'FAIL'} ${label}${detail ? `: ${detail}` : ''}`)
}

const browser = await chromium.launch()
try {
    const context = await browser.newContext({ viewport: { width: 1280, height: 800 } })
    const page = await context.newPage()

    const consoleErrors = []
    const failedRequests = []
    page.on('console', message => {
        if (message.type() === 'error') consoleErrors.push(message.text())
    })
    page.on('requestfailed', request => failedRequests.push(`${request.url()} ${request.failure()?.errorText}`))

    await page.goto(base, { waitUntil: 'networkidle' })

    check('page title names the project', (await page.title()).includes('__PROJECT__'))
    check('heading is rendered', (await page.locator('h1').textContent()) === '__PROJECT__ demo')
    check('stylesheet is applied', await page.locator('.page').evaluate(element =>
        getComputedStyle(element).display !== 'contents'))
    check('no console errors', consoleErrors.length === 0, consoleErrors.join(' | '))
    check('no failed requests', failedRequests.length === 0, failedRequests.join(' | '))
} finally {
    await browser.close()
    server.close()
}

if (failures.length > 0) {
    console.error(`\n${failures.length} check(s) failed`)
    process.exit(1)
}
console.log('\nall checks passed')
