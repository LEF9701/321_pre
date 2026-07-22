import { data, flash } from '../lib/page.js';
import { enumLabel, enumOptions } from '../lib/enums.js';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Card, Button, Field, Input, Select, Csrf, Flash, EmptyState, EnumBadge, TableWrap, DL } from '../components/ui.jsx';

export const name = 'admin-unit-detail';

const S = {
  back:        { en: 'Block',            zh: '座別' },
  occupants:   { en: 'Occupants',        zh: '住戶' },
  occupantsN:  { en: 'Occupants ({n})',  zh: '住戶（{n}）' },
  noOccupant:  { en: '— vacant / no resident account —', zh: '— 空置／沒有住戶帳戶 —' },
  editUnit:    { en: 'Edit unit',        zh: '編輯單位' },
  unitCode:    { en: 'Unit code',        zh: '單位編號' },
  feeStatus:   { en: 'Fee status',       zh: '費用狀況' },
  balance:     { en: 'Balance (HK$)',    zh: '結餘（港幣）' },
  dangerZone:  { en: 'Danger zone',      zh: '危險操作' },
  confirmDel:  { en: 'Delete this unit?', zh: '刪除此單位？' },
  deleteUnit:  { en: 'Delete unit',      zh: '刪除單位' },
  issues:      { en: 'Issues at this unit', zh: '此單位的問題個案' },
  noIssues:    { en: 'No issues',        zh: '沒有問題個案' },
  ref:         { en: 'Ref',              zh: '編號' },
};

export default function AdminUnitDetail() {
  const d = data;
  const unit = d.unit || {};
  const occupants = d.occupants || [];
  const issues = d.issues || [];

  return (
    <>
      <PageHead title={unit.unitCode} back={{ href: '/admin/blocks/' + unit.blockId, label: S.back }} />

      <Flash flash={flash} />

      <Card title={occupants.length ? t(S.occupantsN, { n: occupants.length }) : t(S.occupants)} icon="users" flush={occupants.length > 0}>
        {occupants.length === 0
          ? <div style={{ padding: 16 }}><span className="muted">{t(S.noOccupant)}</span></div>
          : (
            <TableWrap>
              <thead>
                <tr><th>{t(C.name)}</th><th>{t(C.email)}</th><th>{t(C.phone)}</th></tr>
              </thead>
              <tbody>
                {occupants.map((o, i) => (
                  <tr key={i}>
                    <td className="strong">{o.name}</td>
                    <td className="muted small">{o.email}</td>
                    <td className="mono small">{o.phone}</td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>

      <Card title={t(S.editUnit)} icon="edit">
        <form method="post" action={'/admin/units/' + unit.id} className="stack">
          <Csrf />
          <Field label={t(S.unitCode)} htmlFor="unitCode">
            <Input id="unitCode" name="unitCode" required defaultValue={unit.unitCode || ''} />
          </Field>
          <div className="grid grid--2">
            <Field label={t(C.status)} htmlFor="status">
              <Select id="status" name="status" options={enumOptions('UnitStatus')} defaultValue={unit.status ?? ''} />
            </Field>
            <Field label={t(S.feeStatus)} htmlFor="feeStatus">
              <Select id="feeStatus" name="feeStatus" options={enumOptions('FeeStatus')} defaultValue={unit.feeStatus ?? ''} />
            </Field>
          </div>
          <div className="grid grid--2">
            <Field label={t(S.balance)} htmlFor="balance">
              <Input id="balance" name="balance" type="number" step="0.01" defaultValue={unit.balance ?? ''} />
            </Field>
            <Field label={t(C.dueDate)} htmlFor="dueDate">
              <Input id="dueDate" name="dueDate" type="date" defaultValue={unit.dueDate || ''} />
            </Field>
          </div>
          <div>
            <Button type="submit" variant="primary" icon="check">{t(C.save)}</Button>
          </div>
        </form>
      </Card>

      <Card title={t(S.dangerZone)}>
        <form method="post" action={'/admin/units/' + unit.id + '/delete'} style={{ display: 'inline' }}
              onSubmit={(e) => { if (!confirm(t(S.confirmDel))) e.preventDefault(); }}>
          <Csrf />
          <Button type="submit" variant="danger" icon="trash">{t(S.deleteUnit)}</Button>
        </form>
      </Card>

      <Card title={t(S.issues)} icon="wrench" flush>
        {issues.length === 0
          ? <EmptyState icon="check" title={t(S.noIssues)} />
          : (
            <TableWrap>
              <thead>
                <tr><th>{t(S.ref)}</th><th>{t(C.category)}</th><th>{t(C.status)}</th><th>{t(C.priority)}</th></tr>
              </thead>
              <tbody>
                {issues.map((i) => (
                  <tr key={i.id}>
                    <td><a className="mono strong" href={`/mgmt/issues/${i.id}`}>{i.refNumber}</a></td>
                    <td>{enumLabel('IssueCategory', i.category)}</td>
                    <td><EnumBadge type="IssueStatus" value={i.status} /></td>
                    <td><EnumBadge type="IssuePriority" value={i.priority} /></td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>
    </>
  );
}
