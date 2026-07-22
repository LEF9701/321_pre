import { data, flash } from '../lib/page.js';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Card, Button, Field, Input, Csrf, Flash, EmptyState, TableWrap } from '../components/ui.jsx';

export const name = 'admin-buildings';
export const title = { en: 'Buildings', zh: '大廈管理' };

const S = {
  head:        { en: 'Buildings',          zh: '大廈管理' },
  register:    { en: 'Register a building', zh: '登記大廈' },
  registerBtn: { en: 'Register building',  zh: '登記大廈' },
  buildingName: { en: 'Name',              zh: '名稱' },
  district:    { en: 'District',           zh: '地區' },
  configure:   { en: 'Configure',          zh: '設定' },
  empty:       { en: 'No buildings yet',   zh: '暫未有大廈' },
  emptyHint:   { en: 'Register one above to get started.', zh: '請在上方登記大廈以開始。' },
  confirmDel:  { en: 'Delete this building and ALL its blocks/floors/units?',
                 zh: '刪除此大廈及其所有座別、樓層及單位？' },
};

export default function AdminBuildings() {
  const d = data;
  const rows = d.rows || [];

  return (
    <>
      <PageHead title={t(S.head)} />

      <Flash flash={flash} />

      <Card title={t(S.register)} icon="building">
        <form method="post" action="/admin/buildings" className="stack">
          <Csrf />
          <Field label={t(S.buildingName)} htmlFor="name">
            <Input id="name" name="name" required />
          </Field>
          <Field label={t(C.address)} htmlFor="address">
            <Input id="address" name="address" />
          </Field>
          <Field label={t(S.district)} htmlFor="district">
            <Input id="district" name="district" />
          </Field>
          <div>
            <Button type="submit" variant="primary" icon="plus">{t(S.registerBtn)}</Button>
          </div>
        </form>
      </Card>

      <Card flush>
        {rows.length === 0
          ? <EmptyState icon="building" title={t(S.empty)}>{t(S.emptyHint)}</EmptyState>
          : (
            <TableWrap>
              <thead>
                <tr><th>{t(S.buildingName)}</th><th>{t(C.address)}</th><th>{t(S.district)}</th><th className="num">{t(C.blocks)}</th><th className="num">{t(C.units)}</th><th /></tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.id}>
                    <td><a className="strong" href={`/admin/buildings/${r.id}`}>{r.name}</a></td>
                    <td>{r.address || <span className="muted">—</span>}</td>
                    <td>{r.district || <span className="muted">—</span>}</td>
                    <td className="num">{r.blocks}</td>
                    <td className="num">{r.units}</td>
                    <td className="right nowrap">
                      <Button href={`/admin/buildings/${r.id}`} size="sm" variant="ghost">{t(S.configure)}</Button>
                      {' '}
                      <form method="post" action={`/admin/buildings/${r.id}/delete`} style={{ display: 'inline' }}
                            onSubmit={(e) => { if (!confirm(t(S.confirmDel))) e.preventDefault(); }}>
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
