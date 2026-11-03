import test from 'node:test';
import assert from 'node:assert/strict';
import { apiResponse } from './apiResponse.js';
// KI-Log Nr. 19
test('valid success data and false values remain unchanged', () => {
  assert.deepEqual(apiResponse('{"done":false,"reps":0}', 200, true), { done: false, reps: 0 });
});

test('backend validation message and field errors are preserved', () => {
  assert.throws(
    () => apiResponse('{"message":"Invalid input.","errors":{"age":"Check age"}}', 400, false),
    e => e.message === 'Invalid input.' && e.fields.age === 'Check age');
});

test('HTML error from a proxy has a useful HTTP message', () => {
  assert.throws(() => apiResponse('<html>Application error</html>', 503, false), /HTTP 503/);
});

test('success without the expected object is rejected', () => {
  for (const body of ['', '<html>frontend</html>', 'null', '42', '[]'])
    assert.throws(() => apiResponse(body, 200, true), /JSON response/);
});