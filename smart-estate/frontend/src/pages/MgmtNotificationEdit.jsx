import { data, flash } from '../lib/page.js';
import { enumOptions } from '../lib/enums.js';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Card, Flash, Field, Input, Select, Textarea, Button, Csrf } from '../components/ui.jsx';

export const name = 'mgmt-notification-edit';
export const title = { en: 'Edit notice', zh: '編輯通知' };

const S = {
  head:      { en: 'Edit notice',    zh: '編輯通知' },
  sub:       { en: 'Update the notice content, category or severity.',
               zh: '更新通知的內容、類別或嚴重程度。' },
  backLabel: { en: 'Notice',         zh: '通知' },
  titleEn:   { en: 'Title (English)', zh: '標題（英文）' },
  titleZh:   { en: 'Title (中文)',     zh: '標題（中文）' },
  contentEn: { en: 'English content', zh: '內容（英文）' },
  contentZh: { en: 'Chinese content (中文)', zh: '內容（中文）' },
};

export default function MgmtNotificationEdit() {
  const n = data.n || {};
  return (
    <>
      <PageHead title={t(S.head)} sub={t(S.sub)}
                back={{ href: `/mgmt/notifications/${n.id}`, label: S.backLabel }} />
      <Flash flash={flash} />
      <Card>
        <form method="post" action={`/mgmt/notifications/${n.id}`}>
          <Csrf />
          <Field label={t(S.titleEn)} htmlFor="title">
            <Input id="title" name="title" required defaultValue={n.title} />
          </Field>
          <Field label={t(S.titleZh)} htmlFor="titleZh">
            <Input id="titleZh" name="titleZh" defaultValue={n.titleZh} />
          </Field>
          <div className="form-grid">
            <Field label={t(C.category)} htmlFor="category">
              <Select id="category" name="category" options={enumOptions('NoticeCategory')} defaultValue={n.category} />
            </Field>
            <Field label={t(C.severity)} htmlFor="severity">
              <Select id="severity" name="severity" options={enumOptions('Severity')} defaultValue={n.severity} />
            </Field>
          </div>
          <Field label={t(S.contentEn)} htmlFor="contentEn">
            <Textarea id="contentEn" name="contentEn" rows={4} required defaultValue={n.contentEn} />
          </Field>
          <Field label={t(S.contentZh)} htmlFor="contentZh">
            <Textarea id="contentZh" name="contentZh" rows={4} defaultValue={n.contentZh} />
          </Field>
          <div className="btn-row">
            <Button type="submit" variant="primary" icon="check">{t(C.saveChanges)}</Button>
            <Button href={`/mgmt/notifications/${n.id}`} variant="ghost">{t(C.cancel)}</Button>
          </div>
        </form>
      </Card>
    </>
  );
}
