import { spawn } from 'node:child_process';
import { createServer } from 'node:net';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

const root = fileURLToPath(new URL('..', import.meta.url));
const pkg = JSON.parse(await readFile(resolve(root, 'package.json'), 'utf8'));
const procfile = await readFile(resolve(root, 'Procfile'), 'utf8');

if (!pkg.dependencies?.serve) throw new Error('serve must be a production dependency.');
if (!procfile.includes('serve -s dist')) throw new Error('Procfile must start "serve -s dist".');

// Freien Port suchen
const probe = createServer();
await new Promise((ready, reject) => { probe.once('error', reject); probe.listen(0, '127.0.0.1', ready); });
const port = probe.address().port;
await new Promise(closed => probe.close(closed));

const child = spawn(process.execPath,
    [resolve(root, 'node_modules/serve/build/main.js'), '-s', 'dist',
        '-l', `tcp://127.0.0.1:${port}`, '--no-clipboard'],
    { cwd: root, stdio: ['ignore', 'pipe', 'pipe'] });
let logs = '';
let ended = false;
child.stdout.on('data', part => { logs += part.toString(); });
child.stderr.on('data', part => { logs += part.toString(); });
child.on('exit', () => { ended = true; });

const base = `http://127.0.0.1:${port}`;
try {
    let response;
    for (let attempt = 0; attempt < 40; attempt++) {
        if (ended) throw new Error('Static server stopped: ' + logs.slice(-1000));
        try {
            response = await fetch(`${base}/`, { signal: AbortSignal.timeout(1500) });
            if (response.ok) break;
        } catch { /* Server startet noch */ }
        await new Promise(wait => setTimeout(wait, 100));
    }
    if (!response?.ok) throw new Error('Frontend does not start on the given port.');

    const html = await response.text();
    if (!html.includes('<div id="root">')) throw new Error('React entry point is missing.');
    const asset = html.match(/src="([^"]+\.js)"/);
    if (!asset) throw new Error('Built JavaScript is missing.');
    const js = await fetch(new URL(asset[1], base));
    if (!js.ok || !(js.headers.get('content-type') || '').includes('javascript'))
        throw new Error('JavaScript asset is not served correctly.');

    const deepLink = await fetch(`${base}/training`);
    if (!deepLink.ok || !(await deepLink.text()).includes('<div id="root">'))
        throw new Error('Deep link /training does not fall back to index.html.');

    console.log('Static frontend: HTML, JavaScript, deep link and port checked successfully.');
} finally {
    if (!ended) {
        child.kill('SIGTERM');
        await new Promise(done => child.once('exit', done));
    }
}