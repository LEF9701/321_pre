import { useEffect } from 'react';
import { PAGE, user } from './lib/page.js';
import Shell from './components/Shell.jsx';
import { t, useLocale, applyDocumentLang } from './lib/i18n.js';

// Pages self-register: each module under pages/ exports `name` (matching the
// server-injected PAGE.name) and a default component. No central registry to
// edit, so pages can be added in parallel without conflicts.
const modules = import.meta.glob('./pages/**/*.jsx', { eager: true });
const registry = {};
for (const m of Object.values(modules)) {
  if (m && m.name && m.default) registry[m.name] = m;
}

const NOT_FOUND = {
  title: { en: 'Page not found', zh: '找不到頁面' },
  body: {
    en: 'No React page is registered for',
    zh: '沒有為此頁面註冊 React 元件：',
  },
};

const BRAND = { en: 'Smart Estate', zh: 'Smart Estate' };

export default function App() {
  // The app is a single React root, so subscribing here re-renders the whole
  // tree (Shell and page alike) the moment the language changes.
  useLocale();

  const mod = registry[PAGE.name];
  // A page's own `title` is a translatable { en, zh } pair; the server-rendered
  // <title> stays as the English fallback for pages that don't declare one.
  const title = mod?.title ? t(mod.title) : (PAGE.title || '');

  useEffect(() => {
    applyDocumentLang();
    if (title) document.title = `${title} — ${t(BRAND)}`;
  }, [title]);

  if (!mod) {
    return (
      <Shell title={t(NOT_FOUND.title)}>
        <div className="card"><div className="card__body">
          {t(NOT_FOUND.body)} <code>{String(PAGE.name)}</code>.
        </div></div>
      </Shell>
    );
  }

  const Page = mod.default;
  // `bare` pages (login) render their own full-screen layout, no app chrome.
  if (mod.bare || !user) return <Page />;
  return (
    <Shell title={title} sub={PAGE.sub}>
      <Page />
    </Shell>
  );
}
