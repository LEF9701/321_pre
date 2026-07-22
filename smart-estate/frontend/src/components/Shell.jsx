import { useEffect, useState } from 'react';
import Icon from '../lib/icons.jsx';
import { cx, csrf, user, activeEmergency, initials } from '../lib/page.js';
import { enumLabel } from '../lib/enums.js';
import { t, getLocale, toggleLocale, LOCALE_SHORT, LOCALE_LABEL } from '../lib/i18n.js';
import { C } from '../lib/strings.js';

/* ---- role-aware navigation (mirrors the old fragments/common.html) ---- */
const NAV = {
  RESIDENT: [
    { group: { en: 'Resident', zh: '住戶' }, items: [
      { label: { en: 'Dashboard', zh: '概覽' }, href: '/resident', icon: 'dashboard' },
      { label: { en: 'Inbox', zh: '收件匣' }, href: '/resident/inbox', icon: 'inbox' },
      { label: { en: 'Preferences', zh: '通訊偏好' }, href: '/resident/preferences', icon: 'sliders' },
      { label: { en: 'Report / Track Issue', zh: '報告／追蹤問題' }, href: '/resident/issues', icon: 'wrench' },
      { label: { en: 'Access card', zh: '門禁卡' }, href: '/resident/access', icon: 'shield' },
      { label: { en: 'Fees', zh: '費用' }, href: '/resident/fees', icon: 'dollar' },
      { label: { en: 'Maintenance', zh: '維修保養' }, href: '/resident/maintenance', icon: 'calendar' },
      { label: { en: 'AI Assistant', zh: 'AI 助理' }, href: '/ai/chat', icon: 'sparkles' },
    ]},
  ],
  MANAGEMENT: [
    { group: { en: 'Management', zh: '管理處' }, items: [
      { label: { en: 'Overview', zh: '總覽' }, href: '/mgmt', icon: 'dashboard' },
      { label: { en: 'Residents', zh: '住戶' }, href: '/mgmt/residents', icon: 'users' },
      { label: { en: 'Notifications', zh: '通知' }, href: '/mgmt/notifications', icon: 'bell' },
      { label: { en: 'Analytics', zh: '數據分析' }, href: '/mgmt/analytics', icon: 'chart' },
      { label: { en: 'Issues', zh: '問題個案' }, href: '/mgmt/issues', icon: 'wrench' },
      { label: { en: 'Sensors', zh: '感應器' }, href: '/mgmt/iot', icon: 'activity' },
      { label: { en: 'Access control', zh: '門禁管理' }, href: '/mgmt/access', icon: 'shield' },
      { label: { en: 'Fees', zh: '費用' }, href: '/mgmt/fees', icon: 'dollar' },
      { label: { en: 'AI Assistant', zh: 'AI 助理' }, href: '/ai/chat', icon: 'sparkles' },
      { label: { en: 'AI Evaluation', zh: 'AI 評估' }, href: '/ai/evaluation', icon: 'check' },
    ]},
  ],
  ADMIN_EXTRA: [
    { group: { en: 'Administration', zh: '系統管理' }, items: [
      { label: { en: 'Admin Home', zh: '管理主頁' }, href: '/admin', icon: 'home' },
      { label: { en: 'Buildings', zh: '大廈管理' }, href: '/admin/buildings', icon: 'building',
        paths: ['/admin/buildings', '/admin/blocks', '/admin/units', '/admin/floors'] },
      { label: { en: 'Floor Map', zh: '樓層平面圖' }, href: '/admin/floormap', icon: 'map' },
      { label: { en: 'Users & Roles', zh: '使用者及角色' }, href: '/admin/users', icon: 'users' },
      { label: { en: 'Emergency', zh: '緊急事故' }, href: '/admin/emergency', icon: 'alert' },
      { label: { en: 'Audit Log', zh: '稽核紀錄' }, href: '/admin/audit', icon: 'history' },
      { label: { en: 'AI Documents', zh: 'AI 文件' }, href: '/ai/documents', icon: 'file' },
    ]},
  ],
};

const BRAND = {
  name: { en: 'Smart Estate', zh: 'Smart Estate' },
  sub:  { en: 'Evergreen Bay Estate · BMS', zh: '常青灣屋苑 · 樓宇管理系統' },
  foot: { en: 'CSIT321 prototype · external channels mocked', zh: 'CSIT321 原型 · 外部渠道為模擬' },
};

const UI = {
  menu:        { en: 'Menu',                   zh: '選單' },
  toggleTheme: { en: 'Toggle theme',           zh: '切換主題' },
  emergency:   { en: 'EMERGENCY ACTIVE',       zh: '緊急事故生效中' },
  declaredBy:  { en: 'declared {at} by {by}.', zh: '由 {by} 於 {at} 發佈。' },
  manage:      { en: 'Manage ›',               zh: '管理 ›' },
  switchTo:    { en: 'Switch to {lang}',       zh: '切換至 {lang}' },
};

function navForRole(role) {
  if (role === 'RESIDENT') return NAV.RESIDENT;
  if (role === 'MANAGEMENT') return NAV.MANAGEMENT;
  if (role === 'ADMIN') return [...NAV.MANAGEMENT, ...NAV.ADMIN_EXTRA];
  return [];
}

// On logout, drop any per-user AI conversation held in this tab so the next
// account that signs in here starts clean. The form still submits normally.
function clearPrivateState() {
  try {
    for (let i = sessionStorage.length - 1; i >= 0; i--) {
      const k = sessionStorage.key(i);
      if (k && k.startsWith('se.ai.thread')) sessionStorage.removeItem(k);
    }
  } catch { /* storage unavailable, nothing to clear */ }
}

// Longest-prefix wins so /resident matches Dashboard but /resident/inbox matches Inbox.
function activeHref(groups, path) {
  let best = null, bestLen = -1;
  for (const g of groups) for (const it of g.items) {
    for (const p of (it.paths || [it.href])) {
      if ((path === p || path.startsWith(p + '/') || (p === path)) && p.length > bestLen) {
        best = it.href; bestLen = p.length;
      }
    }
  }
  return best;
}

function useTheme() {
  const [theme, setTheme] = useState(() => {
    const saved = typeof localStorage !== 'undefined' && localStorage.getItem('se-theme');
    if (saved) return saved;
    return (typeof matchMedia !== 'undefined' && matchMedia('(prefers-color-scheme: dark)').matches) ? 'dark' : 'light';
  });
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    try { localStorage.setItem('se-theme', theme); } catch {}
  }, [theme]);
  return [theme, () => setTheme((t) => (t === 'dark' ? 'light' : 'dark'))];
}

/* Shows the language you are reading in; clicking swaps to the other one. */
function LanguageButton() {
  const locale = getLocale();
  const other = locale === 'zh' ? 'en' : 'zh';
  const hint = t(UI.switchTo, { lang: LOCALE_LABEL[other] });
  return (
    <button className="iconbtn langbtn" onClick={toggleLocale} title={hint} aria-label={hint}>
      <Icon name="globe" size={17} />
      <span>{LOCALE_SHORT[locale]}</span>
    </button>
  );
}

function Sidebar({ groups, path, onNavigate }) {
  const active = activeHref(groups, path);
  return (
    <aside className="sidebar">
      <a className="sidebar__brand" href="/">
        <span className="logo">🏢</span>
        <span>{t(BRAND.name)}<small>{t(BRAND.sub)}</small></span>
      </a>
      <nav className="sidebar__nav">
        {groups.map((g) => (
          <div className="nav-group" key={g.group.en}>
            <div className="nav-group__label">{t(g.group)}</div>
            {g.items.map((it) => (
              <a key={it.href} href={it.href} onClick={onNavigate}
                 className={cx('nav-link', active === it.href && 'is-active')}>
                <Icon name={it.icon} size={18} className="ico" />
                <span>{t(it.label)}</span>
              </a>
            ))}
          </div>
        ))}
      </nav>
      <div className="sidebar__foot">{t(BRAND.foot)}</div>
    </aside>
  );
}

function EmergencyBanner() {
  if (!activeEmergency) return null;
  return (
    <div className="emergency-banner">
      <span className="pulse" />
      <span>
        <strong>{t(UI.emergency)} — {enumLabel('EmergencyType', activeEmergency.type)}</strong>
        {' '}{t(UI.declaredBy, { at: activeEmergency.declaredAt, by: activeEmergency.declaredBy })}
      </span>
      {user?.role === 'ADMIN' && <a href="/admin/emergency" style={{ marginLeft: 'auto' }}>{t(UI.manage)}</a>}
    </div>
  );
}

export default function Shell({ title, sub, children }) {
  const [theme, toggleTheme] = useTheme();
  const [navOpen, setNavOpen] = useState(false);
  const path = typeof location !== 'undefined' ? location.pathname : '/';
  const groups = navForRole(user?.role);

  return (
    <div className={cx('app', navOpen && 'nav-open')}>
      <div className="scrim" onClick={() => setNavOpen(false)} />
      <Sidebar groups={groups} path={path} onNavigate={() => setNavOpen(false)} />
      <div className="main">
        <header className="topbar">
          <button className="iconbtn topbar__hamburger" onClick={() => setNavOpen((v) => !v)} aria-label={t(UI.menu)}>
            <Icon name="menu" size={20} />
          </button>
          <div className="topbar__title">{title}{sub && <small>{sub}</small>}</div>
          <div className="topbar__spacer" />
          <LanguageButton />
          <button className="iconbtn" onClick={toggleTheme} aria-label={t(UI.toggleTheme)} title={t(UI.toggleTheme)}>
            <Icon name={theme === 'dark' ? 'sun' : 'moon'} size={18} />
          </button>
          {user && (
            <div className="user-chip">
              <div className="avatar">{initials(user.name)}</div>
              <div className="user-chip__meta">
                <div className="name">{user.name}</div>
                <div className="muted small">{enumLabel('Role', user.role)}</div>
              </div>
              <form method="post" action="/logout" style={{ display: 'inline' }} onSubmit={clearPrivateState}>
                <input type="hidden" name={csrf.parameterName} value={csrf.token} />
                <button className="iconbtn" type="submit" aria-label={t(C.logOut)} title={t(C.logOut)}>
                  <Icon name="logout" size={18} />
                </button>
              </form>
            </div>
          )}
        </header>
        <EmergencyBanner />
        <main className="content">{children}</main>
      </div>
    </div>
  );
}
