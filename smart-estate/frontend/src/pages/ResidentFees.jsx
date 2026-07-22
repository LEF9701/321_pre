import { data, hkd } from '../lib/page.js';
import { PageHead, Card, EnumBadge, EmptyState, TableWrap, DL } from '../components/ui.jsx';
import { t, field } from '../lib/i18n.js';
import { C } from '../lib/strings.js';

export const name = 'resident-fees';
export const title = { en: 'Fees', zh: '費用' };

const S = {
  head:        { en: 'Fees', zh: '費用' },
  sub:         { en: 'Current balance, due date and reminder history for your unit.',
                 zh: '您單位的目前結餘、到期日及提醒紀錄。' },
  feeStatus:   { en: 'Management fee status', zh: '管理費狀況' },
  outstanding: { en: 'Outstanding balance', zh: '未繳結餘' },
  noUnit:      { en: 'No unit assigned', zh: '未編配單位' },
  reminders:   { en: 'Reminder history', zh: '提醒紀錄' },
  noReminders: { en: 'No fee reminders received', zh: '未收到任何費用提醒' },
  received:    { en: 'Received', zh: '收到時間' },
  notice:      { en: 'Notice', zh: '通告' },
  mockNote:    { en: 'This prototype mocks payment; no online payment gateway is wired up.',
                 zh: '此原型的付款功能為模擬，並未接駁任何網上支付閘道。' },
};

export default function ResidentFees() {
  const d = data;
  const unit = d.unit || null;
  const feeHistory = d.feeHistory || [];

  return (
    <>
      <PageHead title={t(S.head)} sub={t(S.sub)} />

      <div className="stack">
        <Card title={t(S.feeStatus)} icon="dollar">
          {unit
            ? (
              <DL items={[
                [t(C.unit), unit.fullCode],
                [t(C.status), <EnumBadge type="FeeStatus" value={unit.feeStatus} />],
                [t(S.outstanding), hkd(unit.balance)],
                [t(C.dueDate), unit.dueDate],
              ]} />
            )
            : <EmptyState icon="home" title={t(S.noUnit)} />}
        </Card>

        <Card title={t(S.reminders)} icon="bell" flush>
          {feeHistory.length === 0
            ? <EmptyState icon="bell" title={t(S.noReminders)} />
            : (
              <TableWrap>
                <thead>
                  <tr>
                    <th>{t(S.received)}</th>
                    <th>{t(S.notice)}</th>
                    <th>{t(C.channels)}</th>
                    <th>{t(C.status)}</th>
                  </tr>
                </thead>
                <tbody>
                  {feeHistory.map((it) => (
                    <tr key={it.id}>
                      <td className="nowrap muted small">{it.dispatchedAt}</td>
                      <td><a className="strong" href={`/resident/inbox/${it.id}`}>{field(it, 'title')}</a></td>
                      <td className="muted">{it.channels}</td>
                      <td><EnumBadge type="DeliveryStatus" value={it.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </TableWrap>
            )}
        </Card>

        <p className="muted small">{t(S.mockNote)}</p>
      </div>
    </>
  );
}
