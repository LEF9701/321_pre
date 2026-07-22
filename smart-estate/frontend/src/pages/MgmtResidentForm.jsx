import { data, flash } from '../lib/page.js';
import { enumOptions } from '../lib/enums.js';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Card, Button, Field, Input, Select, Csrf, Alert, Flash } from '../components/ui.jsx';

export const name = 'mgmt-resident-form';

const S = {
  add:         { en: 'Add resident',    zh: '新增住戶' },
  edit:        { en: 'Edit resident',   zh: '編輯住戶' },
  emailLogin:  { en: 'Email (login)',   zh: '電郵（登入帳號）' },
  unassigned:  { en: '— unassigned —',  zh: '— 未分配 —' },
  defaultsPre: { en: 'New residents get the default password ',
                 zh: '新住戶會使用預設密碼 ' },
  defaultsPost:{ en: ' and default Email / preferred-language preferences.',
                 zh: '，並採用預設的電郵及偏好語言通訊設定。' },
};

export default function MgmtResidentForm() {
  const d = data;
  const action = d.isNew ? '/mgmt/residents' : '/mgmt/residents/' + d.residentId;
  const unitOptions = (d.units || []).map((u) => ({ value: u.id, label: u.fullCode }));

  return (
    <>
      <PageHead
        title={d.isNew ? t(S.add) : t(S.edit)}
        back={{ href: '/mgmt/residents', label: C.residents }}
      />

      <Flash flash={flash} />

      <Card>
        <form method="post" action={action} className="stack">
          <Csrf />
          <Field label={t(C.name)} htmlFor="name">
            <Input id="name" name="name" required defaultValue={d.name || ''} />
          </Field>
          <Field label={t(S.emailLogin)} htmlFor="email">
            <Input id="email" name="email" type="email" required defaultValue={d.email || ''} />
          </Field>
          <Field label={t(C.phone)} htmlFor="phone">
            <Input id="phone" name="phone" defaultValue={d.phone || ''} />
          </Field>
          <Field label={t(C.language)} htmlFor="language">
            <Select id="language" name="language" options={enumOptions('Language')} defaultValue={d.language ?? ''} />
          </Field>
          <Field label={t(C.unit)} htmlFor="unitId">
            <Select id="unitId" name="unitId" options={unitOptions} includeEmpty emptyLabel={t(S.unassigned)}
                    defaultValue={d.unitId ?? ''} />
          </Field>

          {d.isNew && (
            <Alert tone="info">
              {t(S.defaultsPre)}<b>resident123</b>{t(S.defaultsPost)}
            </Alert>
          )}

          <div>
            <Button type="submit" variant="primary" icon="check">{t(C.save)}</Button>
          </div>
        </form>
      </Card>
    </>
  );
}
