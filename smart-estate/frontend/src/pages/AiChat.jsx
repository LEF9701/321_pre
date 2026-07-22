import { useCallback, useEffect, useRef, useState } from 'react';
import { data, csrf, cx, user } from '../lib/page.js';
import Icon from '../lib/icons.jsx';
import { Button } from '../components/ui.jsx';
import { t, plural } from '../lib/i18n.js';
import { C } from '../lib/strings.js';

export const name = 'ai-chat';
export const title = { en: 'AI Assistant', zh: 'AI 助理' };

// The thread is namespaced per user so a different account signing in on the same
// browser never sees the previous resident's conversation (answers can carry their
// account figures). It lives in sessionStorage, so it survives page navigation and
// reloads within the tab, and clears when the tab closes, on "New chat", or on logout.
const STORE_KEY = `se.ai.thread:${user?.id ?? 'anon'}`;
const HISTORY_TURNS = 6;

const S = {
  placeholder:  { en: 'Ask about estate rules, fees, facilities… (中文亦可)',
                  zh: '查詢屋苑守則、管理費、設施等⋯（亦可用英文提問）' },
  assistant:    { en: 'Estate Assistant',  zh: '屋苑 AI 助理' },
  online:       { en: 'Online · cited from estate bylaws',    zh: '線上・引用屋苑守則' },
  offline:      { en: 'Offline mode · extractive search',     zh: '離線模式・條文檢索' },
  newChat:      { en: 'New chat',          zh: '開新對話' },
  welcome:      { en: 'How can I help with the estate?',      zh: '有甚麼可以幫到您？' },
  welcomeBody:  { en: 'Ask about rules, fees, renovation, noise, fire safety or meetings. I answer from '
                    + 'the indexed bylaw and FAQ documents and show you the passages I used.',
                  zh: '歡迎查詢屋苑守則、管理費、裝修、噪音、消防安全及業主大會等事宜。'
                    + '所有回答均引用屋苑守則及常見問題文件，並會列出所引用的條文。' },
  citedSources: { one:   '{n} cited source',
                  other: '{n} cited sources',
                  zh:    '引用了 {n} 段條文' },
  similarity:   { en: 'Similarity {v}',    zh: '相似度 {v}' },
  composeLabel: { en: 'Message the estate assistant', zh: '向屋苑 AI 助理發送訊息' },
  hint:         { en: 'Enter to send · Shift + Enter for a new line. Please contact the management '
                    + 'office for formal matters.',
                  zh: '按 Enter 傳送，Shift + Enter 換行。正式事宜請聯絡管理處。' },
  unreachable:  { en: "Sorry — I couldn't reach the assistant just now. Please try again, or contact the "
                    + 'management office (Evergreen Property Management Ltd., 3000 0000).',
                  zh: '抱歉，暫時無法連接 AI 助理。請稍後再試，或聯絡管理處'
                    + '（常青物業管理有限公司，3000 0000）。' },
};

// Situational, cross-document prompts. Written in both languages: the RAG answers in
// the language it was asked in, so a Chinese reader needs Chinese prompts or the demo
// silently drops to the English corpus.
const SUGGESTIONS = [
  { icon: 'dollar', text: { en: 'Is my management fee overdue, and what happens if I keep not paying?',
                            zh: '我的管理費是否已逾期？如果一直不繳交會有甚麼後果？' } },
  { icon: 'wrench', text: { en: 'I want to renovate my flat — what approvals do I need, and when is the deposit forfeited?',
                            zh: '我想裝修，需要甚麼批准？押金在甚麼情況下會被沒收？' } },
  { icon: 'users',  text: { en: "I'm letting my flat out — who pays the management fee, me or my tenant?",
                            zh: '我將單位出租，管理費應由我還是租客繳付？' } },
  { icon: 'alert',  text: { en: 'The flat upstairs drills late at night and water is seeping into mine — what are my rights?',
                            zh: '樓上單位深夜鑽牆，又滲水到我的單位，我有甚麼權利？' } },
];

let seq = 0;
const nextId = () => `m${Date.now().toString(36)}-${seq++}`;

function loadThread() {
  try {
    const raw = sessionStorage.getItem(STORE_KEY);
    const parsed = raw ? JSON.parse(raw) : null;
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function saveThread(msgs) {
  try {
    sessionStorage.setItem(STORE_KEY, JSON.stringify(msgs.filter((m) => !m.pending)));
  } catch {
    /* storage unavailable; thread won't survive a reload */
  }
}

function clockTime(ts) {
  return new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

/** Renders the answer, turning inline [doc-k] markers into clickable citation chips. */
function AnswerText({ text, onCite }) {
  const parts = String(text || '').split(/(\[doc-\d+\])/g);
  return (
    <>
      {parts.map((part, i) => {
        const m = /^\[doc-(\d+)\]$/.exec(part);
        if (!m) return <span key={i}>{part}</span>;
        return (
          <button key={i} type="button" className="cite" onClick={() => onCite(part.slice(1, -1))}>
            {m[1]}
          </button>
        );
      })}
    </>
  );
}

function SourcePanel({ sources, openLabel, onToggle }) {
  const [expanded, setExpanded] = useState(false);
  const open = expanded || !!openLabel;
  const hlRef = useRef(null);

  // Clicking an inline [doc-k] chip should bring that passage into view, not just reveal it.
  useEffect(() => {
    if (openLabel && hlRef.current) {
      hlRef.current.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    }
  }, [openLabel]);

  return (
    <div className="msg__sources">
      <button
        type="button"
        className="msg__sources-toggle"
        onClick={() => { setExpanded(!open); onToggle(); }}
      >
        <Icon name="file" size={13} />
        {t(plural(sources.length, S.citedSources), { n: sources.length })}
        <Icon name="chevronRight" size={13} className={cx('caret', open && 'caret--open')} />
      </button>

      {open && (
        <ul className="src-list">
          {sources.map((s) => (
            <li
              key={s.label}
              ref={openLabel === s.label ? hlRef : null}
              className={cx('src', openLabel === s.label && 'src--hl')}
            >
              <div className="src__head">
                <span className="src__num">{String(s.label).replace('doc-', '')}</span>
                <span className="src__title">{s.title}</span>
                <span className="src__code mono">{s.docCode}</span>
                <span className="src__score" title={t(S.similarity, { v: Number(s.score).toFixed(2) })}>
                  <span className="src__score-bar" style={{ width: `${Math.min(100, Number(s.score) * 100)}%` }} />
                </span>
              </div>
              <p className="src__snippet">{s.snippet}</p>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function Message({ msg, onCite, openLabel }) {
  if (msg.role === 'user') {
    return (
      <div className="msg msg--me">
        <div className="msg__bubble">{msg.text}</div>
        <div className="msg__meta">{clockTime(msg.at)}</div>
      </div>
    );
  }

  return (
    <div className="msg msg--ai">
      <div className="msg__avatar"><Icon name="sparkles" size={17} /></div>
      <div className="msg__body">
        <div className={cx('msg__bubble', msg.error && 'msg__bubble--error')}>
          {msg.pending
            ? <span className="typing"><i /><i /><i /></span>
            : <AnswerText text={msg.text} onCite={onCite} />}
        </div>

        {!msg.pending && msg.sources && msg.sources.length > 0 && (
          <SourcePanel sources={msg.sources} openLabel={openLabel} onToggle={() => onCite(null)} />
        )}

        {!msg.pending && (
          <div className="msg__meta">
            {clockTime(msg.at)}
            {msg.mode && <> · <span className="msg__mode">{msg.mode}</span></>}
          </div>
        )}
      </div>
    </div>
  );
}

export default function AiChat() {
  const d = data;
  const [messages, setMessages] = useState(() => {
    const stored = loadThread();
    if (stored.length) return stored;
    // A no-JS form post left a server-rendered answer behind, so start the thread with it.
    if (d.result) {
      const at = Date.now();
      return [
        { id: nextId(), role: 'user', text: d.question, at },
        {
          id: nextId(), role: 'ai', text: d.result.answer, at,
          mode: d.result.mode, sources: d.result.sources || [], error: d.result.refused,
        },
      ];
    }
    return [];
  });
  const [draft, setDraft] = useState('');
  const [busy, setBusy] = useState(false);
  const [openCite, setOpenCite] = useState(null);

  const threadRef = useRef(null);
  const inputRef = useRef(null);

  useEffect(() => { saveThread(messages); }, [messages]);

  useEffect(() => {
    const el = threadRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [messages]);

  const send = useCallback(async (question) => {
    const text = question.trim();
    if (!text || busy) return;

    const at = Date.now();
    const pendingId = nextId();
    setMessages((prev) => [
      ...prev,
      { id: nextId(), role: 'user', text, at },
      { id: pendingId, role: 'ai', pending: true, at },
    ]);
    setDraft('');
    setBusy(true);

    // Send the recent thread so the assistant can resolve follow-ups ("what about on Sundays?").
    // Pending and errored turns are excluded; neither is something the model said.
    const history = messages
      .filter((m) => !m.pending && !m.error && m.text)
      .slice(-HISTORY_TURNS)
      .map((m) => ({ role: m.role === 'user' ? 'USER' : 'ASSISTANT', text: m.text }));

    let reply;
    try {
      const res = await fetch('/ai/chat/ask', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify({ question: text, history }),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const r = await res.json();
      reply = {
        text: r.answer, mode: r.mode, sources: r.sources || [], error: r.refused,
      };
    } catch {
      reply = {
        text: t(S.unreachable),
        error: true, sources: [],
      };
    }

    setMessages((prev) => prev.map((m) => (
      m.id === pendingId ? { ...m, ...reply, pending: false, at: Date.now() } : m
    )));
    setBusy(false);
    inputRef.current?.focus();
  }, [busy, messages]);

  function onKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      send(draft);
    }
  }

  function reset() {
    setMessages([]);
    setOpenCite(null);
    try { sessionStorage.removeItem(STORE_KEY); } catch { /* ignore */ }
    inputRef.current?.focus();
  }

  const empty = messages.length === 0;

  return (
    <div className="chat">
      <header className="chat__head">
        <div className="chat__id">
          <div className="chat__avatar"><Icon name="sparkles" size={19} /></div>
          <div>
            <div className="chat__name">{t(S.assistant)}</div>
            <div className={cx('chat__status', d.claudeAvailable && 'is-live')}>
              <span className="dot" />
              {d.claudeAvailable ? t(S.online) : t(S.offline)}
            </div>
          </div>
        </div>
        {!empty && (
          <Button variant="ghost" size="sm" icon="plus" onClick={reset}>{t(S.newChat)}</Button>
        )}
      </header>

      <div className="chat__thread" ref={threadRef}>
        {empty ? (
          <div className="chat__welcome">
            <div className="chat__welcome-mark"><Icon name="sparkles" size={26} /></div>
            <h2>{t(S.welcome)}</h2>
            <p>{t(S.welcomeBody)}</p>
            <div className="chat__chips">
              {SUGGESTIONS.map((s) => (
                <button key={s.text.en} type="button" className="chip" onClick={() => send(t(s.text))}>
                  <Icon name={s.icon} size={15} />
                  {t(s.text)}
                </button>
              ))}
            </div>
          </div>
        ) : (
          <div className="chat__list">
            {messages.map((m) => (
              <Message
                key={m.id}
                msg={m}
                openLabel={openCite && openCite.id === m.id ? openCite.label : null}
                onCite={(label) => setOpenCite(label ? { id: m.id, label } : null)}
              />
            ))}
          </div>
        )}
      </div>

      <div className="chat__composer">
        <div className="composer">
          {/* The wrapper mirrors the draft in an invisible ::after so the textarea
              auto-grows in pure CSS. Fall back to placeholder so it isn't clipped
              on narrow screens. */}
          <div className="composer__grow" data-value={draft || t(S.placeholder)}>
            <textarea
              ref={inputRef}
              className="composer__input"
              rows={1}
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              onKeyDown={onKeyDown}
              placeholder={t(S.placeholder)}
              aria-label={t(S.composeLabel)}
            />
          </div>
          <button
            type="button"
            className="composer__send"
            disabled={!draft.trim() || busy}
            onClick={() => send(draft)}
            aria-label={t(C.send)}
          >
            <Icon name="send" size={17} />
          </button>
        </div>
        <div className="composer__hint">{t(S.hint)}</div>
      </div>
    </div>
  );
}
