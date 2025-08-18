// src/utils/authToken.js
export function getRawToken() {
  return (
    localStorage.getItem('token') ||
    localStorage.getItem('jwt') ||
    localStorage.getItem('authToken') ||
    localStorage.getItem('accessToken') ||
    ''
  );
}

export function sanitizeToken(t) {
  if (!t) return '';
  let x = t.trim();
  // "eyJ..." 같이 감싸진 경우 제거
  x = x.replace(/^"|"$/g, '');
  // Bearer가 저장돼 있으면 제거해서 순수 토큰만 남기기
  if (/^Bearer\s+/i.test(x)) x = x.replace(/^Bearer\s+/i, '');
  return x;
}

export function decodeJwtWithoutVerify(token) {
  try {
    const payload = token.split('.')[1];
    return JSON.parse(atob(payload));
  } catch {
    return null;
  }
}

export function isExpired(token) {
  const p = decodeJwtWithoutVerify(token);
  if (!p || !p.exp) return false; // exp 없으면 판단 불가 → 일단 통과
  const nowSec = Math.floor(Date.now() / 1000);
  return p.exp <= nowSec;
}
