import test from 'node:test';
import assert from 'node:assert/strict';
import { adminAction, submitAdmin } from './adminBridge.js';
// KI-Log Nr. 18
test('SSO uses a POST body and never puts the token into a URL', () => {
  let submitted = false;
  let appended;
  const form = { appendChild(input) { this.input = input; }, submit() { submitted = true; } };
  const doc = {
    createElement(tag) { return tag === 'form' ? form : {}; },
    body: { appendChild(element) { appended = element; } },
  };
  submitAdmin('test-token', 'https://backend.example.org/', doc);
  assert.equal(form.action, 'https://backend.example.org/admin/sso');
  assert.equal(form.method, 'POST');
  assert.equal(form.input.name, 'token');
  assert.equal(form.input.value, 'test-token');
  assert.equal(form.hidden, true);
  assert.equal(appended, form);
  assert.equal(submitted, true);
});

test('backend config cannot supply credentials, query, hash or other protocols', () => {
  for (const base of ['javascript:alert(1)', 'https://user:pass@example.org',
    'https://example.org/?token=x', 'https://example.org/#x', 'https://example.org/api'])
    assert.throws(() => adminAction(base));
});