import { data, cx } from '../lib/page.js';
import { t } from '../lib/i18n.js';
import { PageHead, Card, Badge, EmptyState, TableWrap, Icon } from '../components/ui.jsx';

export const name = 'admin-audit';
export const title = { en: 'Audit Log', zh: '稽核紀錄' };

const S = {
  head:      { en: 'Audit Log',     zh: '稽核紀錄' },
  sub:       { en: 'Record of sensitive actions for compliance.', zh: '敏感操作紀錄，供合規審查之用。' },
  all:       { en: 'All actions',   zh: '所有操作' },
  emergOnly: { en: 'Emergency only', zh: '只顯示緊急事故' },
  empty:     { en: 'No audit entries.', zh: '暫未有稽核紀錄。' },
  time:      { en: 'Time',          zh: '時間' },
  actor:     { en: 'Actor',         zh: '操作者' },
  action:    { en: 'Action',        zh: '操作' },
  detail:    { en: 'Detail',        zh: '詳情' },
  emergency: { en: 'Emergency',     zh: '緊急事故' },
};

export default function AdminAudit() {
  const logs = data.logs || [];
  const emergencyOnly = !!data.emergencyOnly;

  return (
    <>
      <PageHead title={t(S.head)} sub={t(S.sub)} />

      <div className="pills" style={{ marginBottom: 16 }}>
        <a className={cx('pill', !emergencyOnly && 'is-active')} href="/admin/audit">{t(S.all)}</a>
        <a className={cx('pill', emergencyOnly && 'is-active')} href="/admin/audit?filter=emergency">{t(S.emergOnly)}</a>
      </div>

      <Card flush>
        {logs.length === 0 ? (
          <EmptyState icon="clipboard" title={t(S.empty)} />
        ) : (
          <TableWrap>
            <thead>
              <tr><th>{t(S.time)}</th><th>{t(S.actor)}</th><th>{t(S.action)}</th><th>{t(S.detail)}</th><th>{t(S.emergency)}</th></tr>
            </thead>
            <tbody>
              {logs.map((l, i) => (
                <tr key={i}>
                  <td className="nowrap mono small">{l.timestamp}</td>
                  <td>{l.actor}</td>
                  <td><Badge>{l.action}</Badge></td>
                  <td>{l.detail}</td>
                  <td className="center">
                    {l.emergencyRelated ? <Badge tone="danger" dot={false}><Icon name="alert" size={13} /></Badge> : null}
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
