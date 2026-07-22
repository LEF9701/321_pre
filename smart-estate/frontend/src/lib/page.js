// Reads the server-injected payloads and exposes helpers shared across pages.
//   window.__APP__  = common context (csrf, user, activeEmergency, flash)
//   window.__PAGE__ = per page (name, title, data)
// The dev harness puts everything on __PAGE__, so we merge with __APP__ winning.

const APP = (typeof window !== 'undefined' && window.__APP__) || {};
export const PAGE = (typeof window !== 'undefined' && window.__PAGE__) || {};
export const data = PAGE.data || {};

const rawUser = APP.user ?? PAGE.user ?? null;
export const user = rawUser && rawUser.role ? rawUser : null;

const rawEmg = APP.activeEmergency ?? PAGE.activeEmergency ?? null;
export const activeEmergency = rawEmg && rawEmg.type ? rawEmg : null;

const rawFlash = APP.flash ?? PAGE.flash ?? {};
export const flash = (rawFlash && (rawFlash.msg || rawFlash.error || rawFlash.msgCode || rawFlash.errorCode))
  ? rawFlash : null;

export const csrf = APP.csrf ?? PAGE.csrf ?? { parameterName: '_csrf', token: '', headerName: 'X-CSRF-TOKEN' };

export function cx(...parts) {
  return parts.filter(Boolean).join(' ');
}

const money = new Intl.NumberFormat('en-HK', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
export function hkd(n) {
  if (n == null || n === '') return '—';
  const v = typeof n === 'number' ? n : Number(n);
  if (Number.isNaN(v)) return String(n);
  return 'HK$ ' + money.format(v);
}

export function initials(name) {
  if (!name) return '?';
  return name.trim().split(/\s+/).slice(0, 2).map((s) => s[0]).join('').toUpperCase();
}

export function pct(n) {
  if (n == null) return '0%';
  return Math.round(n) + '%';
}
