// Bilingual EN + Traditional Chinese (繁體中文, HK). One primitive t(); { en, zh }
// pairs written inline next to markup, shared ones in ./strings.js as C. One React
// root: App.jsx subscribes via useLocale(), so a language change re-renders below.

import { useEffect, useReducer } from 'react';
import { user } from './page.js';

export const LOCALES = ['en', 'zh'];
const STORAGE_KEY = 'se-lang';

// BCP-47 tag for <html lang>. Traditional Chinese as used in Hong Kong.
const HTML_LANG = { en: 'en', zh: 'zh-Hant-HK' };

// What each locale calls itself, never translated.
export const LOCALE_LABEL = { en: 'English', zh: '繁體中文' };
export const LOCALE_SHORT = { en: 'EN', zh: '繁中' };

function detect() {
  // 1. an explicit choice made in this browser wins over everything
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (LOCALES.includes(saved)) return saved;
  } catch { /* private mode / storage disabled */ }

  // 2. otherwise seed from the account's language, so a resident whose profile
  //    says ZH lands on a Chinese UI the first time without touching anything.
  //    This only seeds the initial value; switching the UI afterwards never
  //    writes back to the account (notification routing stays in Preferences).
  const accountLang = user?.language;
  if (accountLang === 'ZH') return 'zh';
  if (accountLang === 'EN') return 'en';

  // 3. fall back to the browser's preference
  try {
    const nav = (navigator.languages || [navigator.language || '']).join(',').toLowerCase();
    if (/\bzh\b|zh-/.test(nav)) return 'zh';
  } catch { /* no navigator (SSR / tests) */ }

  return 'en';
}

let current = detect();
const subscribers = new Set();

export function getLocale() {
  return current;
}

/** The Language enum name the server uses, for form fields that post a language. */
export function localeAsEnum() {
  return current === 'zh' ? 'ZH' : 'EN';
}

export function setLocale(next) {
  if (!LOCALES.includes(next) || next === current) return;
  current = next;
  try { localStorage.setItem(STORAGE_KEY, next); } catch { /* ignore */ }
  applyDocumentLang();
  subscribers.forEach((fn) => fn());
}

export function toggleLocale() {
  setLocale(current === 'zh' ? 'en' : 'zh');
}

export function applyDocumentLang() {
  if (typeof document === 'undefined') return;
  document.documentElement.setAttribute('lang', HTML_LANG[current] || 'en');
}

/** Re-render on language change. Called once at the root; the tree follows. */
export function useLocale() {
  const [, bump] = useReducer((n) => n + 1, 0);
  useEffect(() => {
    subscribers.add(bump);
    return () => subscribers.delete(bump);
  }, []);
  return current;
}

/**
 * Translate. Accepts an { en, zh } pair, or a plain string (returned as-is, for
 * values that are the same in both languages or already come from the server).
 * `vars` fills {placeholders}.
 */
export function t(s, vars) {
  if (s == null) return '';
  let out;
  if (typeof s === 'string') {
    out = s;
  } else {
    // fall back to English when a Chinese string has not been written yet, so a
    // missing translation degrades to readable text rather than blank space
    out = s[current] ?? s.en ?? s.zh ?? '';
  }
  if (!vars) return out;
  return out.replace(/\{(\w+)\}/g, (m, k) => (vars[k] == null ? m : String(vars[k])));
}

/**
 * Pick a bilingual pair off a server object, falling back to the other side when
 * one is blank (management may only fill in one). Handles two field shapes:
 *   - symmetric contentEn / contentZh: field(notice, 'content')
 *   - asymmetric title / titleZh: field(notice, 'title')
 *     (the plain `title` is the English/default side; there is no `titleEn`)
 */
export function field(obj, base) {
  if (!obj) return '';
  const cap = base.charAt(0).toUpperCase() + base.slice(1);
  const zh = obj[base + 'Zh'] ?? obj[cap + 'Zh'];
  const en = obj[base + 'En'] ?? obj[cap + 'En'] ?? obj[base];
  const primary = current === 'zh' ? zh : en;
  const secondary = current === 'zh' ? en : zh;
  const val = (primary != null && String(primary).trim() !== '') ? primary : secondary;
  return val == null ? '' : val;
}

/**
 * English needs singular/plural agreement, Chinese does not. Pass the two
 * English forms and the single Chinese form.
 */
export function plural(n, forms) {
  if (current === 'zh') return t(forms.zh ?? '');
  return n === 1 ? t(forms.one ?? '') : t(forms.other ?? '');
}
