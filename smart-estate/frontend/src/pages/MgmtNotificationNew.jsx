import { data } from '../lib/page.js';
import { enumOptions, enumLabel } from '../lib/enums.js';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Card, Button, Field, Input, Textarea, Select, Csrf } from '../components/ui.jsx';

export const name = 'mgmt-notification-new';
export const title = { en: 'Compose Notification', zh: '撰寫通知' };

const S = {
  head:          { en: 'Compose Notification',            zh: '撰寫通知' },
  backLabel:     { en: 'Notifications',                   zh: '通知' },
  templateCard:  { en: 'Start from a HK template (FR-6)', zh: '由香港常用範本開始（FR-6）' },
  noTemplates:   { en: 'No templates available.',         zh: '沒有可用範本。' },
  notification:  { en: 'Notification',                    zh: '通知' },
  titleEn:       { en: 'Title (English)',                 zh: '標題（英文）' },
  titleZh:       { en: 'Title (中文)',                     zh: '標題（中文）' },
  contentEn:     { en: 'Content (English)',               zh: '內容（英文）' },
  contentZh:     { en: 'Content (中文)',                   zh: '內容（中文）' },
  recipients:    { en: 'Recipients',                      zh: '收件對象' },
  allResidents:  { en: 'All residents',                   zh: '全體住戶' },
  specificBlock: { en: 'Specific block (batch)',          zh: '指定座別（批次）' },
  blockIf:       { en: 'Block (if specific)',             zh: '座別（如選指定座別）' },
  when:          { en: 'When',                            zh: '發送時間' },
  sendNow:       { en: 'Send now',                        zh: '立即發送' },
  sendLater:     { en: 'Schedule for later',              zh: '預約稍後發送' },
  scheduledAt:   { en: 'Scheduled time',                  zh: '預約時間' },
  scheduledHint: { en: '(used only with Schedule for later — FR-1.4)',
                   zh: '（只在選擇「預約稍後發送」時適用 — FR-1.4）' },
  dispatch:      { en: 'Dispatch',                        zh: '發送' },
};

export default function MgmtNotificationNew() {
  const d = data;
  const templates = d.templates || [];
  // Keep the server's filtered category set (it excludes EMERGENCY), but show each
  // in the reader's language rather than the server's English label.
  const categories = (d.categories || []).map((c) => ({
    value: c.value,
    label: enumLabel('NoticeCategory', c.value),
  }));
  const blocks = d.blocks || [];
  const pre = d.pre || {};

  return (
    <>
      <PageHead
        title={t(S.head)}
        back={{ href: '/mgmt/notifications', label: S.backLabel }}
      />

      <Card title={t(S.templateCard)} icon="sparkles">
        {templates.length === 0
          ? <span className="muted">{t(S.noTemplates)}</span>
          : (
            <div className="btn-row" style={{ flexWrap: 'wrap', gap: 8 }}>
              {templates.map((tpl) => (
                <Button key={tpl.code} href={'/mgmt/notifications/new?template=' + tpl.code} variant="ghost" size="sm">{tpl.name}</Button>
              ))}
            </div>
          )}
      </Card>

      <Card title={t(S.notification)} icon="bell">
        <form method="post" action="/mgmt/notifications">
          <Csrf />

          <Field label={t(S.titleEn)} htmlFor="title" full>
            <Input id="title" name="title" required defaultValue={pre.title} />
          </Field>
          <Field label={t(S.titleZh)} htmlFor="titleZh" full>
            <Input id="titleZh" name="titleZh" defaultValue={pre.titleZh} />
          </Field>

          <div className="form-grid">
            <Field label={t(C.category)} htmlFor="category">
              <Select id="category" name="category" options={categories} defaultValue={pre.category} />
            </Field>
            <Field label={t(C.severity)} htmlFor="severity">
              <Select id="severity" name="severity" options={enumOptions('Severity')} defaultValue={pre.severity} />
            </Field>
          </div>

          <Field label={t(S.contentEn)} htmlFor="contentEn" full>
            <Textarea id="contentEn" name="contentEn" rows={4} required defaultValue={pre.contentEn} />
          </Field>
          <Field label={t(S.contentZh)} htmlFor="contentZh" full>
            <Textarea id="contentZh" name="contentZh" rows={4} defaultValue={pre.contentZh} />
          </Field>

          <Field label={t(S.recipients)} htmlFor="scopeType">
            <Select id="scopeType" name="scopeType" options={[
              { value: 'ALL', label: t(S.allResidents) },
              { value: 'BLOCK', label: t(S.specificBlock) },
            ]} />
          </Field>

          <Field label={t(S.blockIf)} htmlFor="scopeId">
            <Select id="scopeId" name="scopeId" includeEmpty options={blocks.map((b) => ({ value: b.id, label: b.blockName }))} />
          </Field>

          <Field label={t(S.when)} htmlFor="when">
            <Select id="when" name="when" options={[
              { value: 'now', label: t(S.sendNow) },
              { value: 'later', label: t(S.sendLater) },
            ]} />
          </Field>

          <Field label={t(S.scheduledAt)} htmlFor="scheduledAt" hint={t(S.scheduledHint)}>
            <Input id="scheduledAt" type="datetime-local" name="scheduledAt" />
          </Field>

          <Button type="submit" variant="primary" icon="send">{t(S.dispatch)}</Button>
        </form>
      </Card>
    </>
  );
}
