import { data } from '../lib/page.js';
import { enumLabel } from '../lib/enums.js';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Card, Button, EmptyState, TableWrap, Field, Input, Select, Csrf } from '../components/ui.jsx';

export const name = 'mgmt-residents';
export const title = { en: 'Residents', zh: '住戶' };

const S = {
  count:        { en: '{n} resident(s)',              zh: '{n} 位住戶' },
  addResident:  { en: 'Add resident',                 zh: '新增住戶' },
  searchPh:     { en: 'Search name / email / phone',  zh: '搜尋姓名／電郵／電話' },
  block:        { en: 'Block',                        zh: '座別' },
  allBlocks:    { en: 'All blocks',                   zh: '所有座別' },
  empty:        { en: 'No residents found',           zh: '找不到住戶' },
  emptyHint:    { en: 'Try a different search or ',   zh: '請嘗試其他搜尋條件，或' },
  emptyLink:    { en: 'add a resident »',             zh: '新增住戶 »' },
  unassigned:   { en: '— unassigned —',               zh: '— 未分配 —' },
  confirmRemove:{ en: 'Remove this resident?',        zh: '確定移除此住戶？' },
};

export default function MgmtResidents() {
  const d = data;
  const residents = d.residents || [];
  const blockOptions = (d.blocks || []).map((b) => ({ value: b.id, label: b.blockName }));

  return (
    <>
      <PageHead
        title={t(C.residents)}
        sub={t(S.count, { n: residents.length })}
        actions={<Button href="/mgmt/residents/new" variant="primary" icon="plus">{t(S.addResident)}</Button>}
      />

      <Card>
        <form method="get" action="/mgmt/residents" className="row" style={{ gap: 12, alignItems: 'flex-end', flexWrap: 'wrap' }}>
          <Field label={t(C.search)}>
            <Input name="q" defaultValue={d.q || ''} placeholder={t(S.searchPh)} />
          </Field>
          <Field label={t(S.block)}>
            <Select name="block" options={blockOptions} includeEmpty emptyLabel={t(S.allBlocks)}
                    defaultValue={d.blockFilter ?? ''} />
          </Field>
          <Button type="submit" variant="primary" icon="search">{t(C.search)}</Button>
          <Button href="/mgmt/residents" variant="ghost">{t(C.reset)}</Button>
        </form>
      </Card>

      <Card flush>
        {residents.length === 0
          ? <EmptyState icon="users" title={t(S.empty)}>{t(S.emptyHint)}<a href="/mgmt/residents/new">{t(S.emptyLink)}</a></EmptyState>
          : (
            <TableWrap>
              <thead><tr><th>{t(C.name)}</th><th>{t(C.email)}</th><th>{t(C.phone)}</th><th>{t(C.unit)}</th><th>{t(C.language)}</th><th /></tr></thead>
              <tbody>
                {residents.map((r) => (
                  <tr key={r.id}>
                    <td className="strong">{r.name}</td>
                    <td className="muted">{r.email}</td>
                    <td className="muted">{r.phone}</td>
                    <td>{r.unitFullCode || <span className="muted">{t(S.unassigned)}</span>}</td>
                    <td>{enumLabel('Language', r.language)}</td>
                    <td className="nowrap">
                      <Button href={`/mgmt/residents/${r.id}/edit`} size="sm" variant="ghost" icon="edit" />
                      <form method="post" action={`/mgmt/residents/${r.id}/delete`} style={{ display: 'inline' }}
                            onSubmit={(e) => { if (!confirm(t(S.confirmRemove))) e.preventDefault(); }}>
                        <Csrf />
                        <Button type="submit" size="sm" variant="danger" icon="trash" />
                      </form>
                    </td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>
    </>
  );
}
