import { data, flash, hkd } from '../lib/page.js';
import { PageHead, Stat, Card, EnumBadge, Button, EmptyState, TableWrap, Bar, Csrf, Flash } from '../components/ui.jsx';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';

export const name = 'mgmt-fees';
export const title = { en: 'Fee Collection', zh: '費用收繳' };

const S = {
  head:           { en: 'Fee Collection',  zh: '費用收繳' },
  occupiedUnits:  { en: 'Occupied units',  zh: '已入住單位' },
  paid:           { en: 'Paid',            zh: '已繳費' },
  due:            { en: 'Due',             zh: '待繳費' },
  overdue:        { en: 'Overdue',         zh: '逾期未繳' },

  collectionRate: { en: 'Collection rate', zh: '收繳率' },

  runReminders:   { en: 'Run fee reminders',     zh: '發送費用提醒' },
  runNow:         { en: 'Run fee reminders now', zh: '立即發送費用提醒' },
  runHint:        { en: 'Normally runs on the 1st of each month (FR-6.3).',
                    zh: '一般於每月一號自動執行（FR-6.3）。' },

  outstanding:    { en: 'Units with outstanding fees', zh: '有欠繳費用的單位' },
  allPaid:        { en: 'All occupied units are paid up.', zh: '所有已入住單位均已繳費。' },
  occupant:       { en: 'Occupant',        zh: '住戶' },
  manage:         { en: 'Manage',          zh: '管理' },
};

export default function MgmtFees() {
  const d = data;
  const rows = d.rows || [];
  const rate = d.collectionRate ?? 0;
  const rateTone = rate >= 80 ? 'ok' : rate >= 50 ? 'warn' : 'danger';

  return (
    <>
      <PageHead title={t(S.head)} />

      <Flash flash={flash} />

      <div className="grid grid--4" style={{ marginBottom: 16 }}>
        <Stat label={t(S.occupiedUnits)} value={d.totalOccupied ?? 0} icon="building" />
        <Stat label={t(S.paid)} value={d.paid ?? 0} icon="check" tone="ok" />
        <Stat label={t(S.due)} value={d.due ?? 0} icon="dollar" tone="warn" />
        <Stat label={t(S.overdue)} value={d.overdue ?? 0} icon="alert" tone="danger" />
      </div>

      <div className="stack">
        <Card title={t(S.collectionRate)} icon="chart">
          <Bar pct={rate} tone={rateTone} showLabel />
        </Card>

        <Card title={t(S.runReminders)} icon="send">
          <form method="post" action="/mgmt/fees/run-reminders" className="row" style={{ gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
            <Csrf />
            <Button type="submit" variant="primary" icon="send">{t(S.runNow)}</Button>
            <span className="muted small">{t(S.runHint)}</span>
          </form>
        </Card>

        <Card title={t(S.outstanding)} icon="dollar" flush>
          {rows.length === 0
            ? <EmptyState icon="check" title={t(S.allPaid)} />
            : (
              <TableWrap>
                <thead><tr><th>{t(C.unit)}</th><th>{t(S.occupant)}</th><th>{t(C.status)}</th><th>{t(C.balance)}</th><th>{t(C.dueDate)}</th><th></th></tr></thead>
                <tbody>
                  {rows.map((u) => (
                    <tr key={u.id}>
                      <td className="strong">{u.fullCode}</td>
                      <td>{u.occupant || <span className="muted">—</span>}</td>
                      <td><EnumBadge type="FeeStatus" value={u.feeStatus} /></td>
                      <td className="num">{hkd(u.balance)}</td>
                      <td className="nowrap">{u.dueDate || <span className="muted">—</span>}</td>
                      <td className="num"><Button href={'/mgmt/fees/units/' + u.id} variant="ghost" size="sm" iconRight="chevronRight">{t(S.manage)}</Button></td>
                    </tr>
                  ))}
                </tbody>
              </TableWrap>
            )}
        </Card>
      </div>
    </>
  );
}
