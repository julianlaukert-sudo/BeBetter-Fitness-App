// Baut die SSO-Adresse nur aus einer reinen Backend-Origin
export function adminAction(base) {
  const url = new URL(base);
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password
      || url.search || url.hash || !['', '/'].includes(url.pathname))
    throw new Error('Please check the backend address for admin login.');
  return `${url.origin}/admin/sso`;
}

// Schickt das Token einmal im POST-Body und nie in der URL
export function submitAdmin(token, base, doc = document) {
  const form = doc.createElement('form');
  form.method = 'POST';
  form.action = adminAction(base);
  form.hidden = true;
  const input = doc.createElement('input');
  input.type = 'hidden';
  input.name = 'token';
  input.value = token;
  form.appendChild(input);
  doc.body.appendChild(form);
  form.submit();
}