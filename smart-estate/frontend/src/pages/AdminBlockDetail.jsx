import { data, flash, cx } from '../lib/page.js';
import { enumLabel } from '../lib/enums.js';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Card, Button, Field, Input, Csrf, Flash, EmptyState } from '../components/ui.jsx';

export const name = 'admin-block-detail';

const S = {
  addFloor:     { en: 'Add a floor',      zh: '新增樓層' },
  addFloorBtn:  { en: 'Add floor',        zh: '新增樓層' },
  floorNumber:  { en: 'Floor number',     zh: '樓層號碼' },
  unitsToGen:   { en: 'Units to generate', zh: '生成單位數目' },
  legend:       { en: 'Legend',           zh: '圖例' },
  legendVacant: { en: 'V — Vacant',       zh: 'V — 空置' },
  legendOccupied: { en: 'O — Occupied',   zh: 'O — 已入住' },
  floorLabel:   { en: '{n}/F',            zh: '{n} 樓' },
  unitCode:     { en: 'unit code',        zh: '單位編號' },
  addUnit:      { en: 'add unit',         zh: '新增單位' },
  confirmDelFloor: { en: 'Delete this floor and its units?', zh: '刪除此樓層及其單位？' },
  noUnits:      { en: 'No units on this floor.', zh: '此樓層暫未有單位。' },
  empty:        { en: 'No floors yet',    zh: '暫未有樓層' },
  emptyHint:    { en: 'Add a floor above.', zh: '請在上方新增樓層。' },
};

export default function AdminBlockDetail() {
  const d = data;
  const block = d.block || {};
  const floors = d.floors || [];

  return (
    <>
      <PageHead
        title={block.blockName}
        sub={block.blockType}
        back={{ href: '/admin/buildings/' + block.buildingId, label: C.building }}
      />

      <Flash flash={flash} />

      <Card title={t(S.addFloor)} icon="building">
        <form method="post" action={'/admin/blocks/' + block.id + '/floors'} className="stack">
          <Csrf />
          <div className="grid grid--2">
            <Field label={t(S.floorNumber)} htmlFor="floorNumber">
              <Input id="floorNumber" name="floorNumber" type="number" required />
            </Field>
            <Field label={t(S.unitsToGen)} htmlFor="unitsPerFloor">
              <Input id="unitsPerFloor" name="unitsPerFloor" type="number" defaultValue={6} min={0} max={40} />
            </Field>
          </div>
          <div>
            <Button type="submit" variant="primary" icon="plus">{t(S.addFloorBtn)}</Button>
          </div>
        </form>
      </Card>

      <Card title={t(S.legend)}>
        <div className="legend">
          <span className="item"><span className="swatch swatch--vacant" /> {t(S.legendVacant)}</span>
          <span className="item"><span className="swatch swatch--occupied" /> {t(S.legendOccupied)}</span>
        </div>
      </Card>

      {floors.length === 0
        ? <Card><EmptyState icon="building" title={t(S.empty)}>{t(S.emptyHint)}</EmptyState></Card>
        : floors.map((floor) => (
          <Card
            key={floor.id}
            title={t(S.floorLabel, { n: floor.floorNumber })}
            actions={
              <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                <form method="post" action={'/admin/floors/' + floor.id + '/units'} style={{ display: 'inline-flex', gap: 6 }}>
                  <Csrf />
                  <Input name="unitCode" size={6} placeholder={t(S.unitCode)} required />
                  <Button type="submit" size="sm" icon="plus">{t(S.addUnit)}</Button>
                </form>
                <form method="post" action={'/admin/floors/' + floor.id + '/delete'} style={{ display: 'inline' }}
                      onSubmit={(e) => { if (!confirm(t(S.confirmDelFloor))) e.preventDefault(); }}>
                  <Csrf />
                  <Button type="submit" size="sm" variant="danger" icon="trash" />
                </form>
              </div>
            }
          >
            {(floor.units || []).length === 0
              ? <span className="muted small">{t(S.noUnits)}</span>
              : (
                <div className="floor-row" style={{ flexWrap: 'wrap' }}>
                  {floor.units.map((unit) => (
                    <a
                      key={unit.id}
                      className={cx('cell', 'cell--' + (unit.status === 'OCCUPIED' ? 'occupied' : 'vacant'))}
                      href={'/admin/units/' + unit.id}
                      title={unit.unitCode + ' — ' + enumLabel('UnitStatus', unit.status)}
                    >
                      <span className="letter">{unit.status === 'OCCUPIED' ? 'O' : 'V'}</span>
                      <span className="code">{unit.unitCode}</span>
                    </a>
                  ))}
                </div>
              )}
          </Card>
        ))}
    </>
  );
}
