import { useState } from 'react';
import Icon from '../lib/icons.jsx';
import { data, csrf } from '../lib/page.js';
import { Alert } from '../components/ui.jsx';
import { t, getLocale, toggleLocale, LOCALE_SHORT, LOCALE_LABEL } from '../lib/i18n.js';
import { C } from '../lib/strings.js';

export const name = 'login';
export const bare = true;

const S = {
  brandLead:   { en: 'Building management for Hong Kong residential estates.',
                 zh: '為香港住宅屋苑而設的樓宇管理系統。' },
  brandBlurb:  { en: 'Web-based management for Evergreen Bay Estate — six Trident blocks, multi-channel notifications, maintenance tracking and an AI bylaw assistant.',
                 zh: '常青灣屋苑的網上管理平台，涵蓋六座三叉型大廈、多渠道通知、維修保養追蹤及 AI 公契助理。' },
  signIn:      { en: 'Sign in', zh: '登入' },
  switchTo:    { en: 'Switch to {lang}', zh: '切換至 {lang}' },
  welcome:     { en: 'Welcome back. Use a demo account below to explore a role.',
                 zh: '歡迎回來。可使用下方的示範帳戶體驗不同角色。' },
  badLogin:    { en: 'Invalid email or password.', zh: '電郵或密碼不正確。' },
  loggedOut:   { en: 'You have been logged out.', zh: '您已登出。' },
  demoHint:    { en: 'Demo accounts — click to fill', zh: '示範帳戶 — 按一下自動填入' },
};

const DEMO = [
  { who: { en: 'Admin', zh: '系統管理員' }, email: 'admin@smartestate.hk', pw: 'admin123' },
  { who: { en: 'Management', zh: '管理處' }, email: 'manager@smartestate.hk', pw: 'manager123' },
  { who: { en: 'Resident', zh: '住戶' }, email: 'resident@smartestate.hk', pw: 'resident123' },
  { who: { en: 'Resident · overdue fee', zh: '住戶 · 逾期未繳費' }, email: 'wong@smartestate.hk', pw: 'resident123' },
  { who: { en: 'Resident · elderly, voice', zh: '住戶 · 長者，語音' }, email: 'cheung@smartestate.hk', pw: 'resident123' },
];

const FEATURES = [
  { en: 'Multi-channel dispatch with per-resident routing', zh: '多渠道發送，按個別住戶路由' },
  { en: 'Issue tracking with AI auto-categorisation', zh: '問題追蹤，配備 AI 自動分類' },
  { en: 'Colour-coded floor map & emergency broadcast', zh: '顏色標示樓層平面圖及緊急廣播' },
];

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const locale = getLocale();
  const other = locale === 'zh' ? 'en' : 'zh';
  const switchHint = t(S.switchTo, { lang: LOCALE_LABEL[other] });

  return (
    <div className="auth">
      <button type="button" className="auth__lang" onClick={toggleLocale}
              title={switchHint} aria-label={switchHint}>
        <Icon name="globe" size={16} />
        <span>{LOCALE_SHORT[locale]}</span>
      </button>
      <aside className="auth__aside">
        <div className="blob b1" /><div className="blob b2" />
        <div className="brandline"><span className="logo">🏢</span> Smart Estate</div>
        <div>
          <h2>{t(S.brandLead)}</h2>
          <p>{t(S.brandBlurb)}</p>
        </div>
        <div className="auth__feats">
          {FEATURES.map((f) => (
            <div className="f" key={f.en}><Icon name="check" size={16} /> {t(f)}</div>
          ))}
        </div>
      </aside>

      <div className="auth__main">
        <div className="auth__card">
          <h1>{t(S.signIn)}</h1>
          <div className="sub">{t(S.welcome)}</div>

          {data.error && <Alert tone="danger">{t(S.badLogin)}</Alert>}
          {data.logout && <Alert tone="ok">{t(S.loggedOut)}</Alert>}

          <form method="post" action="/login">
            <input type="hidden" name={csrf.parameterName} value={csrf.token} />
            <div className="field">
              <label className="field__label" htmlFor="username">{t(C.email)}</label>
              <input className="input" id="username" name="username" type="text" autoFocus required
                     value={username} onChange={(e) => setUsername(e.target.value)}
                     placeholder="you@smartestate.hk" />
            </div>
            <div className="field">
              <label className="field__label" htmlFor="password">{t(C.password)}</label>
              <input className="input" id="password" name="password" type="password" required
                     value={password} onChange={(e) => setPassword(e.target.value)}
                     placeholder="••••••••" />
            </div>
            <button className="btn btn--primary btn--block" type="submit">
              <Icon name="logout" size={16} /> {t(S.signIn)}
            </button>
          </form>

          <div className="demo-accounts">
            <div className="tag-key" style={{ marginBottom: 8 }}>{t(S.demoHint)}</div>
            {DEMO.map((d) => (
              <div className="da-row" key={d.email}
                   onClick={() => { setUsername(d.email); setPassword(d.pw); }}>
                <span className="who">{t(d.who)}</span>
                <span className="cred">{d.email} · {d.pw}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
