import { data, flash } from '../lib/page.js';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Card, Button, Field, Input, Csrf, Flash, EmptyState, TableWrap } from '../components/ui.jsx';

export const name = 'admin-building-detail';

const S = {
  addBlock:     { en: 'Add a block',        zh: '新增座別' },
  addBlockBtn:  { en: 'Add block',          zh: '新增座別' },
  blockName:    { en: 'Block name',         zh: '座別名稱' },
  blockType:    { en: 'Block type',         zh: '座別類型' },
  blockTypeHint:{ en: 'e.g. Trident 1',     zh: '例如：Trident 1' },
  floorsToGen:  { en: 'Floors to generate', zh: '生成樓層數目' },
  unitsPerFloor:{ en: 'Units per floor',    zh: '每層單位數目' },
  blockCol:     { en: 'Block',              zh: '座別' },
  type:         { en: 'Type',               zh: '類型' },
  configure:    { en: 'Configure',          zh: '設定' },
  empty:        { en: 'No blocks yet',      zh: '暫未有座別' },
  emptyHint:    { en: 'Add a block above.', zh: '請在上方新增座別。' },
  confirmDel:   { en: 'Delete this block and all its floors/units?',
                  zh: '刪除此座別及其所有樓層及單位？' },
};

export default function AdminBuildingDetail() {
  const d = data;
  const building = d.building || {};
  const rows = d.rows || [];

  return (
    <>
      <PageHead
        title={building.name}
        sub={[building.address, building.district].filter(Boolean).join(' · ')}
        back={{ href: '/admin/buildings', label: C.buildings }}
      />

      <Flash flash={flash} />

      <Card title={t(S.addBlock)} icon="building">
        <form method="post" action={'/admin/buildings/' + building.id + '/blocks'} className="stack">
          <Csrf />
          <Field label={t(S.blockName)} htmlFor="blockName">
            <Input id="blockName" name="blockName" required />
          </Field>
          <Field label={t(S.blockType)} htmlFor="blockType">
            <Input id="blockType" name="blockType" placeholder={t(S.blockTypeHint)} />
          </Field>
          <div className="grid grid--2">
            <Field label={t(S.floorsToGen)} htmlFor="floors">
              <Input id="floors" name="floors" type="number" defaultValue={0} min={0} max={60} />
            </Field>
            <Field label={t(S.unitsPerFloor)} htmlFor="unitsPerFloor">
              <Input id="unitsPerFloor" name="unitsPerFloor" type="number" defaultValue={0} min={0} max={40} />
            </Field>
          </div>
          <div>
            <Button type="submit" variant="primary" icon="plus">{t(S.addBlockBtn)}</Button>
          </div>
        </form>
      </Card>

      <Card flush>
        {rows.length === 0
          ? <EmptyState icon="building" title={t(S.empty)}>{t(S.emptyHint)}</EmptyState>
          : (
            <TableWrap>
              <thead>
                <tr><th>{t(S.blockCol)}</th><th>{t(S.type)}</th><th className="num">{t(C.floors)}</th><th className="num">{t(C.units)}</th><th /></tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.id}>
                    <td><a className="strong" href={`/admin/blocks/${r.id}`}>{r.blockName}</a></td>
                    <td>{r.blockType || <span className="muted">—</span>}</td>
                    <td className="num">{r.floors}</td>
                    <td className="num">{r.units}</td>
                    <td className="right nowrap">
                      <Button href={`/admin/blocks/${r.id}`} size="sm" variant="ghost">{t(S.configure)}</Button>
                      {' '}
                      <form method="post" action={'/admin/blocks/' + r.id + '/delete'} style={{ display: 'inline' }}
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
