const { chromium } = require('playwright');
const path = require('path');

(async () => {
  const context = await chromium.launchPersistentContext(path.join(process.cwd(), '.local-browser-profile'), { headless: false, viewport: { width: 1280, height: 900 } });
  const page = context.pages()[0] || await context.newPage();
  page.on('response', async response => {
    const type = response.headers()['content-type'] || '';
    if (!response.url().includes('khodro45.com') || !type.includes('json')) return;
    try { const body = await response.text(); if (/"(id|last_price|start_price|end_time|trimyear)"/.test(body)) console.log('VEHICLE_CANDIDATE', response.status(), response.url().replace(/[?&](token|session|csrf|authorization)=[^&]*/ig, '$1=[REDACTED]')); } catch {}
  });
  await page.goto('https://dealer.khodro45.com/cars', { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(1000);
  const inputs = await page.locator('input').evaluateAll(nodes => nodes.map(n => ({ type:n.type, name:n.name, placeholder:n.placeholder, autocomplete:n.autocomplete })));
  console.log(JSON.stringify({ url: page.url(), inputs }));
  await new Promise(() => {});
})();
