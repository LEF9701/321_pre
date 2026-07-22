import { data, flash } from '../lib/page.js';
import { enumOptions } from '../lib/enums.js';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Card, Field, Input, Select, Button, Csrf, Flash } from '../components/ui.jsx';

export const name = 'admin-user-form';

const S = {
  addUser:     { en: 'Add user',   zh: '新增使用者' },
  editUser:    { en: 'Edit user',  zh: '編輯使用者' },
  back:        { en: 'Users',      zh: '使用者' },
  emailLogin:  { en: 'Email (login)', zh: '電郵（登入帳號）' },
  unitResidents: { en: 'Unit (residents only)', zh: '單位（只適用於住戶）' },
  noneOption:  { en: '— none —',   zh: '— 無 —' },
  newPassword: { en: 'New password (blank = keep)', zh: '新密碼（留空則不變）' },
};

export default function AdminUserForm() {
  const d = data;
  const action = d.isNew ? '/admin/users' : '/admin/users/' + d.userId;

  return (
    <>
      <PageHead title={d.isNew ? t(S.addUser) : t(S.editUser)} back={{ href: '/admin/users', label: S.back }} />

      <Flash flash={flash} />

      <Card>
        <form method="post" action={action}>
          <Csrf />
          <div className="form-grid">
            <Field label={t(C.name)} htmlFor="name">
              <Input id="name" name="name" required defaultValue={d.name} />
            </Field>
            <Field label={t(S.emailLogin)} htmlFor="email">
              <Input id="email" name="email" type="email" required defaultValue={d.email} />
            </Field>
          </div>

          <div className="form-grid">
            <Field label={t(C.phone)} htmlFor="phone">
              <Input id="phone" name="phone" defaultValue={d.phone} />
            </Field>
            <Field label={t(C.role)} htmlFor="role">
              <Select id="role" name="role" options={enumOptions('Role')} defaultValue={d.role} />
            </Field>
          </div>

          <div className="form-grid">
            <Field label={t(C.language)} htmlFor="language">
              <Select id="language" name="language" options={enumOptions('Language')} defaultValue={d.language} />
            </Field>
            <Field label={t(S.unitResidents)} htmlFor="unitId">
              <Select
                id="unitId"
                name="unitId"
                options={(d.units || []).map((u) => ({ value: u.id, label: u.fullCode }))}
                includeEmpty
                emptyLabel={t(S.noneOption)}
                defaultValue={d.unitId ?? ''}
              />
            </Field>
          </div>

          <Field label={d.isNew ? t(C.password) : t(S.newPassword)} htmlFor="password" full>
            <Input id="password" name="password" type="text" required={d.isNew} />
          </Field>

          <Button type="submit" variant="primary">{t(C.save)}</Button>
        </form>
      </Card>
    </>
  );
}
