import { data, flash } from '../lib/page.js';
import { enumOptions } from '../lib/enums.js';
import { PageHead, Card, Button, Field, Input, Select, Csrf, Flash, EmptyState, EnumBadge, TableWrap, DL } from '../components/ui.jsx';
import { t, field } from '../lib/i18n.js';
import { C } from '../lib/strings.js';

export const name = 'mgmt-fee-detail';

const S = {
  head:          { en: '{unit} — Fee',   zh: '{unit} — 費用' },
  backToFees:    { en: 'Fee Collection', zh: '費用收繳' },

  occupant:      { en: 'Occupant',       zh: '住戶' },
  noOccupant:    { en: '— vacant / no resident assigned —', zh: '— 空置／未有指派住戶 —' },

  defineFee:     { en: 'Define fee',     zh: '設定費用' },
  balance:       { en: 'Outstanding balance (HK$)', zh: '欠繳結餘（港幣）' },
  saveFee:       { en: 'Save fee',       zh: '儲存費用' },

  sendReminder:  { en: 'Send a reminder', zh: '發送提醒' },
  currentStatus: { en: 'Current status:', zh: '目前狀態：' },
  sendStandard:  { en: 'Send standard reminder',        zh: '發送標準提醒' },
  sendFinal:     { en: 'Send final (overdue) notice',   zh: '發送最後通知（逾期未繳）' },
  noResident:    { en: 'No resident is assigned to this unit.', zh: '此單位未有指派住戶。' },

  history:       { en: 'Reminder history',      zh: '提醒紀錄' },
  noReminders:   { en: 'No reminders sent yet.', zh: '暫時未有發送提醒。' },
  sent:          { en: 'Sent',                  zh: '發送時間' },
  notice:        { en: 'Notice',                zh: '通知' },
};

export default function MgmtFeeDetail() {
  const d = data;
  const unit = d.unit || {};
  const occupant = d.occupant || null;
  const reminders = d.reminders || [];

  return (
    <>
      <PageHead title={t(S.head, { unit: unit.fullCode })} back={{ href: '/mgmt/fees', label: S.backToFees }} />

      <Flash flash={flash} />

      <Card title={t(S.occupant)} icon="users">
        {occupant
          ? <DL items={[[t(C.name), occupant.name], [t(C.email), occupant.email], [t(C.phone), occupant.phone]]} />
          : <span className="muted">{t(S.noOccupant)}</span>}
      </Card>

      <Card title={t(S.defineFee)} icon="dollar">
        <form method="post" action={'/mgmt/fees/units/' + unit.id} className="stack">
          <Csrf />
          <div className="grid grid--2">
            <Field label={t(C.status)} htmlFor="feeStatus">
              <Select id="feeStatus" name="feeStatus" options={enumOptions('FeeStatus')} defaultValue={unit.feeStatus} />
            </Field>
            <Field label={t(S.balance)} htmlFor="balance">
              <Input id="balance" name="balance" type="number" step="0.01" defaultValue={unit.balance} />
            </Field>
            <Field label={t(C.dueDate)} htmlFor="dueDate">
              <Input id="dueDate" name="dueDate" type="date" defaultValue={unit.dueDate || ''} />
            </Field>
          </div>
          <div>
            <Button type="submit" variant="primary" icon="check">{t(S.saveFee)}</Button>
          </div>
        </form>
      </Card>

      <Card title={t(S.sendReminder)} icon="send">
        <div className="row" style={{ alignItems: 'center', gap: 8, marginBottom: 12 }}>
          <span className="muted small">{t(S.currentStatus)}</span>
          <EnumBadge type="FeeStatus" value={unit.feeStatus} />
        </div>
        <div className="btn-row" style={{ flexWrap: 'wrap', gap: 12 }}>
          <form method="post" action={'/mgmt/fees/units/' + unit.id + '/remind'} style={{ display: 'inline' }}>
            <Csrf />
            <input type="hidden" name="escalate" value="false" />
            <Button type="submit" variant="primary" icon="send">{t(S.sendStandard)}</Button>
          </form>
          <form method="post" action={'/mgmt/fees/units/' + unit.id + '/remind'} style={{ display: 'inline' }}>
            <Csrf />
            <input type="hidden" name="escalate" value="true" />
            <Button type="submit" variant="danger" icon="alert">{t(S.sendFinal)}</Button>
          </form>
        </div>
        {!occupant && <div className="muted small" style={{ marginTop: 12 }}>{t(S.noResident)}</div>}
      </Card>

      <Card title={t(S.history)} icon="history" flush>
        {reminders.length === 0
          ? <EmptyState icon="inbox" title={t(S.noReminders)} />
          : (
            <TableWrap>
              <thead>
                <tr><th>{t(S.sent)}</th><th>{t(S.notice)}</th><th>{t(C.channels)}</th><th>{t(C.status)}</th></tr>
              </thead>
              <tbody>
                {reminders.map((r) => (
                  <tr key={r.id}>
                    <td className="nowrap">{r.dispatchedAt || <span className="muted">—</span>}</td>
                    <td>{field(r, 'title')}</td>
                    <td>{r.channels || <span className="muted">—</span>}</td>
                    <td><EnumBadge type="DeliveryStatus" value={r.status} /></td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>
    </>
  );
}
