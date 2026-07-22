import { data } from '../lib/page.js';
import { cx } from '../lib/page.js';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Card, Stat, Bar, Select, EmptyState } from '../components/ui.jsx';

export const name = 'admin-floormap';
export const title = { en: 'Floor Map', zh: '樓層平面圖' };

const S = {
  head:       { en: 'Floor Map',  zh: '樓層平面圖' },
  sub:        { en: 'Colour-coded grid per block. Click any unit to drill down.',
                zh: '各座別的顏色網格，按任何單位可查看詳情。' },
  totalUnits: { en: 'Total units', zh: '單位總數' },
  occupied:   { en: 'Occupied',    zh: '已入住' },
  vacant:     { en: 'Vacant',      zh: '空置' },
  openIssues: { en: 'Open issues', zh: '待處理個案' },
  overdue:    { en: 'Fee overdue', zh: '逾期未繳' },
  noBlocks:   { en: 'No blocks to display.', zh: '沒有可顯示的座別。' },
  floorLabel: { en: '{n}/F',       zh: '{n} 樓' },
};

const LEGEND = [
  { state: 'occupied', letter: 'O', label: { en: 'Occupied',    zh: '已入住' } },
  { state: 'vacant', letter: 'V', label: { en: 'Vacant',        zh: '空置' } },
  { state: 'issue', letter: 'I', label: { en: 'Open issue',     zh: '待處理個案' } },
  { state: 'overdue', letter: '$', label: { en: 'Fee overdue',  zh: '逾期未繳' } },
];

export default function AdminFloormap() {
  const d = data;
  const buildings = d.buildings || [];
  const maps = d.maps || [];

  return (
    <>
      <PageHead title={t(S.head)} sub={t(S.sub)} />

      <Card>
        <form method="get" action="/admin/floormap">
          <Select
            name="building"
            options={buildings.map((b) => ({ value: b.id, label: b.name }))}
            defaultValue={d.selectedBuilding ?? ''}
            onChange={(e) => e.target.form.submit()}
          />
        </form>
        <p className="legend" style={{ marginTop: 12 }}>
          {LEGEND.map((l) => (
            <span key={l.state} className="legend">
              <span className={cx('swatch', 'swatch--' + l.state)}>{l.letter}</span> {t(l.label)}
            </span>
          ))}
        </p>
      </Card>

      {maps.length === 0 ? (
        <EmptyState icon="building" title={t(S.noBlocks)} />
      ) : (
        maps.map((bm) => (
          <Card key={bm.blockId} title={bm.blockName} sub={bm.blockType}>
            <div className="grid grid--auto" style={{ marginBottom: 16 }}>
              <Stat label={t(S.totalUnits)} value={bm.totalUnits} />
              <Stat label={t(S.occupied)} value={bm.occupied} />
              <Stat label={t(S.vacant)} value={bm.vacant} />
              <Stat label={t(S.openIssues)} value={bm.openIssues} tone={bm.openIssues > 0 ? 'warn' : undefined} />
              <Stat label={t(S.overdue)} value={bm.overdue} tone={bm.overdue > 0 ? 'danger' : undefined} />
              <Stat label={t(C.occupancy)} value={<Bar pct={bm.occupancyPct} showLabel />} />
            </div>

            <div className="floormap">
              {(bm.rows || []).map((row) => (
                <div className="floor-row" key={row.floorNumber}>
                  <div className="floor-label">{t(S.floorLabel, { n: row.floorNumber })}</div>
                  {(row.cells || []).map((cell) => (
                    <a
                      key={cell.unitId}
                      className={cx('cell', 'cell--' + cell.state)}
                      href={'/admin/units/' + cell.unitId}
                      title={cell.stateLabel + (cell.occupant ? ' — ' + cell.occupant : '')}
                    >
                      <span className="letter">{cell.letter}</span>
                      <span className="code">{cell.unitCode}</span>
                    </a>
                  ))}
                </div>
              ))}
            </div>
          </Card>
        ))
      )}
    </>
  );
}
