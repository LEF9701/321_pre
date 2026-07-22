import { data, flash } from '../lib/page.js';
import { enumLabel, enumOptions } from '../lib/enums.js';
import { t, field } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Card, Alert, Field, Input, Select, Textarea, Button, EmptyState, TableWrap, Csrf, Flash } from '../components/ui.jsx';

export const name = 'admin-emergency';
export const title = { en: 'Emergency Control', zh: '緊急事故控制' };

const S = {
  head:          { en: 'Emergency Control', zh: '緊急事故控制' },
  sub:           { en: 'Declare → forced multi-channel broadcast → voice escalation → close. {n} residents in scope.',
                   zh: '發佈 → 強制多渠道廣播 → 語音升級 → 結束。涵蓋 {n} 位住戶。' },

  declareCard:   { en: 'Declare an emergency', zh: '發佈緊急事故' },
  declareConfirm:{ en: 'Declare an emergency? This puts the system into elevated state.',
                   zh: '確定發佈緊急事故？系統將進入緊急狀態。' },
  type:          { en: 'Type',                zh: '類型' },
  note:          { en: 'Note / location',     zh: '備註／位置' },
  notePlaceholder: { en: 'e.g. Smoke reported, Azalea Tower 12/F',
                     zh: '例如：Azalea Tower 12 樓發現濃煙' },
  declareBtn:    { en: 'Declare emergency',   zh: '發佈緊急事故' },

  declaredBy:    { en: 'declared {at} by {by}.', zh: '由 {by} 於 {at} 發佈。' },
  noteSuffix:    { en: ' ({note})',            zh: '（{note}）' },

  broadcast:     { en: 'Broadcast',            zh: '廣播' },
  broadcastSub:  { en: 'Overrides preferences — sends on SMS + WhatsApp + Email.',
                   zh: '覆蓋通訊偏好 — 以短訊、WhatsApp 及電郵發送。' },
  titleEn:       { en: 'Title (English)',      zh: '標題（英文）' },
  titleZh:       { en: 'Title (中文)',         zh: '標題（中文）' },
  contentEn:     { en: 'Content (English)',    zh: '內容（英文）' },
  contentZh:     { en: 'Content (中文)',       zh: '內容（中文）' },
  broadcastBtn:  { en: 'Broadcast to all (forced channels)', zh: '向全部住戶廣播（強制渠道）' },

  closeCard:     { en: 'Close',                zh: '結束' },
  closeConfirm:  { en: 'Close the emergency and return to normal routing?',
                   zh: '確定結束緊急事故並回復正常通知發送？' },
  closeBtn:      { en: 'Close emergency',      zh: '結束緊急事故' },

  history:       { en: 'Broadcast history',    zh: '廣播紀錄' },
  noHistory:     { en: 'No emergency broadcasts yet.', zh: '暫未有緊急廣播。' },
  time:          { en: 'Time',                 zh: '時間' },
  recipients:    { en: 'Recipients',           zh: '收件人' },
  deliveredAttempts: { en: 'Delivered/Attempts', zh: '已送達／嘗試次數' },
  voiceEscalation: { en: 'Voice escalation',   zh: '語音升級' },
  detail:        { en: 'Detail',               zh: '詳情' },
  runEscalation: { en: 'Run voice escalation', zh: '執行語音升級' },
  viewLog:       { en: 'View log',             zh: '檢視紀錄' },
  viewAudit:     { en: 'View emergency audit log', zh: '檢視緊急事故稽核紀錄' },
};

export default function AdminEmergency() {
  const d = data;
  const active = d.active;
  const pre = d.pre || {};
  const templates = d.emergencyTemplates || [];
  const broadcasts = d.broadcasts || [];

  return (
    <>
      <PageHead
        title={t(S.head)}
        sub={t(S.sub, { n: d.residentCount ?? 0 })}
      />

      <Flash flash={flash} />

      {!active ? (
        <Card title={t(S.declareCard)}>
          <form
            method="post"
            action="/admin/emergency/declare"
            onSubmit={(e) => { if (!confirm(t(S.declareConfirm))) e.preventDefault(); }}
          >
            <Csrf />
            <Field label={t(S.type)} htmlFor="type">
              <Select id="type" name="type" options={enumOptions('EmergencyType')} />
            </Field>
            <Field label={t(S.note)} htmlFor="note">
              <Input id="note" name="note" size={50} placeholder={t(S.notePlaceholder)} />
            </Field>
            <Button type="submit" variant="danger">{t(S.declareBtn)}</Button>
          </form>
        </Card>
      ) : (
        <>
          <Alert tone="danger">
            <strong>{enumLabel('EmergencyType', active.type)}</strong>{' '}
            {t(S.declaredBy, { at: active.declaredAt, by: active.declaredBy })}
            {active.note ? t(S.noteSuffix, { note: active.note }) : ''}
          </Alert>

          <Card title={t(S.broadcast)} sub={t(S.broadcastSub)}>
            {templates.length > 0 && (
              <div className="btn-row" style={{ marginBottom: 12 }}>
                {templates.map((tpl) => (
                  <Button key={tpl.code} href={'/admin/emergency?template=' + tpl.code} variant="ghost" size="sm">{tpl.name}</Button>
                ))}
              </div>
            )}
            <form method="post" action="/admin/emergency/broadcast">
              <Csrf />
              <Field label={t(S.titleEn)} htmlFor="title">
                <Input id="title" name="title" required defaultValue={pre.title} />
              </Field>
              <Field label={t(S.titleZh)} htmlFor="titleZh">
                <Input id="titleZh" name="titleZh" defaultValue={pre.titleZh} />
              </Field>
              <Field label={t(C.severity)} htmlFor="severity">
                <Select id="severity" name="severity" options={enumOptions('Severity')} defaultValue={pre.severity} />
              </Field>
              <Field label={t(S.contentEn)} htmlFor="contentEn" full>
                <Textarea id="contentEn" name="contentEn" rows={3} required defaultValue={pre.contentEn} />
              </Field>
              <Field label={t(S.contentZh)} htmlFor="contentZh" full>
                <Textarea id="contentZh" name="contentZh" rows={3} defaultValue={pre.contentZh} />
              </Field>
              <Button type="submit" variant="danger" icon="send">{t(S.broadcastBtn)}</Button>
            </form>
          </Card>

          <Card title={t(S.closeCard)}>
            <form
              method="post"
              action="/admin/emergency/close"
              onSubmit={(e) => { if (!confirm(t(S.closeConfirm))) e.preventDefault(); }}
            >
              <Csrf />
              <Button type="submit">{t(S.closeBtn)}</Button>
            </form>
          </Card>
        </>
      )}

      <Card title={t(S.history)} flush>
        {broadcasts.length === 0 ? (
          <EmptyState icon="bell" title={t(S.noHistory)} />
        ) : (
          <TableWrap>
            <thead>
              <tr><th>{t(S.time)}</th><th>{t(C.title)}</th><th>{t(S.recipients)}</th><th>{t(S.deliveredAttempts)}</th><th>{t(S.voiceEscalation)}</th><th>{t(S.detail)}</th></tr>
            </thead>
            <tbody>
              {broadcasts.map((b) => (
                <tr key={b.id}>
                  <td className="nowrap muted small">{b.dispatchedAt}</td>
                  <td className="strong">{field(b, 'title')}</td>
                  <td className="center">{b.recipients}</td>
                  <td className="center">{b.delivered + '/' + b.attempts}</td>
                  <td>
                    <form method="post" action="/admin/emergency/escalate" style={{ display: 'inline' }}>
                      <Csrf />
                      <input type="hidden" name="notificationId" value={b.id} />
                      <Button type="submit" size="sm">{t(S.runEscalation)}</Button>
                    </form>
                  </td>
                  <td><a href={'/mgmt/notifications/' + b.id}>{t(S.viewLog)}</a></td>
                </tr>
              ))}
            </tbody>
          </TableWrap>
        )}
      </Card>

      <div className="btn-row" style={{ marginTop: 16 }}>
        <Button href="/admin/audit?filter=emergency" variant="ghost">{t(S.viewAudit)}</Button>
      </div>
    </>
  );
}
