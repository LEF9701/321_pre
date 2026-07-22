import { data } from '../lib/page.js';
import { t, field } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Card, Stat, Badge, EnumBadge, EmptyState, TableWrap, Bar, Button } from '../components/ui.jsx';

export const name = 'mgmt-notification-detail';

const S = {
  editNotice:  { en: 'Edit notice',           zh: '編輯通知' },
  backLabel:   { en: 'Notifications',         zh: '通知' },
  scope:       { en: 'Scope: {scope}',        zh: '對象範圍：{scope}' },
  forced:      { en: 'FORCED all channels',   zh: '強制使用所有渠道' },
  dispatched:  { en: 'Dispatched {at}',       zh: '已於 {at} 發送' },
  analytics:   { en: 'Delivery analytics',    zh: '發送數據分析' },
  recipients:  { en: 'Recipients',            zh: '收件者' },
  attempts:    { en: 'Total attempts',        zh: '發送次數總計' },
  delivered:   { en: 'Delivered',             zh: '已送達' },
  read:        { en: 'Read',                  zh: '已閱讀' },
  failed:      { en: 'Failed',                zh: '發送失敗' },
  pending:     { en: 'Pending',               zh: '待發送' },
  byChannel:   { en: 'By channel',            zh: '按渠道劃分' },
  noChannel:   { en: 'No channel data',       zh: '沒有渠道數據' },
  attemptsCol: { en: 'Attempts',              zh: '發送次數' },
  logTitle:    { en: 'Per-recipient delivery log', zh: '個別收件者發送紀錄' },
  logSub:      { en: 'Each row is a mocked send: channel, recipient address and simulated status. No real provider is contacted.',
                 zh: '每一行均為模擬發送：渠道、收件地址及模擬狀態。系統不會連接真實服務供應商。' },
  noLogs:      { en: 'No delivery records',   zh: '沒有發送紀錄' },
  recipient:   { en: 'Recipient',             zh: '收件者' },
  sentAt:      { en: 'Sent',                  zh: '發送時間' },
  deliveredAt: { en: 'Delivered',             zh: '送達時間' },
  readAt:      { en: 'Read',                  zh: '閱讀時間' },
  escalation:  { en: 'Escalation',            zh: '升級處理' },
};

export default function MgmtNotificationDetail() {
  const d = data;
  const n = d.n || {};
  const stats = d.stats || {};
  const perChannel = stats.perChannel || [];
  const logs = d.logs || [];

  return (
    <>
      <PageHead title={field(n, 'title')} back={{ href: '/mgmt/notifications', label: S.backLabel }}
                actions={<Button href={`/mgmt/notifications/${n.id}/edit`} icon="edit">{t(S.editNotice)}</Button>} />

      <div className="btn-row" style={{ flexWrap: 'wrap', gap: 8, marginBottom: 16, alignItems: 'center' }}>
        <EnumBadge type="NoticeCategory" value={n.category} dot={false} />
        <EnumBadge type="Severity" value={n.severity} />
        <span className="muted small">{t(S.scope, { scope: n.recipientScope })}</span>
        {n.forcedAllChannels && <Badge tone="danger">{t(S.forced)}</Badge>}
        {n.dispatchedAt && <span className="muted small">{t(S.dispatched, { at: n.dispatchedAt })}</span>}
      </div>

      <Card title={t(C.content)} icon="file">
        <p style={{ whiteSpace: 'pre-wrap', margin: 0 }}>{field(n, 'content')}</p>
      </Card>

      <Card title={t(S.analytics)} icon="chart">
        <div className="grid grid--3">
          <Stat label={t(S.recipients)} value={stats.recipients ?? 0} icon="users" />
          <Stat label={t(S.attempts)} value={stats.attempts ?? 0} icon="send" />
          <Stat label={t(S.delivered)} value={stats.delivered ?? 0} icon="check"
                hint={<Bar pct={stats.deliveryRatePct} showLabel />} />
          <Stat label={t(S.read)} value={stats.read ?? 0} icon="inbox"
                hint={<Bar pct={stats.readRatePct} showLabel />} />
          <Stat label={t(S.failed)} value={stats.failed ?? 0} icon="alert" tone={stats.failed > 0 ? 'danger' : undefined} />
          <Stat label={t(S.pending)} value={stats.pending ?? 0} icon="history" />
        </div>
      </Card>

      <Card title={t(S.byChannel)} icon="sliders" flush>
        {perChannel.length === 0
          ? <EmptyState icon="sliders" title={t(S.noChannel)} />
          : (
            <TableWrap>
              <thead>
                <tr>
                  <th>{t(C.channel)}</th>
                  <th>{t(S.attemptsCol)}</th>
                  <th>{t(S.delivered)}</th>
                  <th>{t(S.read)}</th>
                  <th>{t(S.failed)}</th>
                </tr>
              </thead>
              <tbody>
                {perChannel.map((c, i) => (
                  <tr key={i}>
                    <td><EnumBadge type="Channel" value={c.channel} dot={false} /></td>
                    <td className="num">{c.attempts}</td>
                    <td className="num">{c.delivered}</td>
                    <td className="num">{c.read}</td>
                    <td className="num">{c.failed}</td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>

      <Card title={t(S.logTitle)} icon="clipboard"
            sub={t(S.logSub)}
            flush>
        {logs.length === 0
          ? <EmptyState icon="inbox" title={t(S.noLogs)} />
          : (
            <TableWrap>
              <thead>
                <tr>
                  <th>{t(S.recipient)}</th>
                  <th>{t(C.channel)}</th>
                  <th>{t(C.address)}</th>
                  <th>{t(C.status)}</th>
                  <th>{t(S.sentAt)}</th>
                  <th>{t(S.deliveredAt)}</th>
                  <th>{t(S.readAt)}</th>
                  <th>{t(S.escalation)}</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((l, i) => (
                  <tr key={i}>
                    <td className="strong">{l.recipientName ?? <span className="muted">—</span>}</td>
                    <td><EnumBadge type="Channel" value={l.channel} dot={false} /></td>
                    <td className="mono small">{l.recipientAddress}</td>
                    <td><EnumBadge type="DeliveryStatus" value={l.status} /></td>
                    <td className="nowrap muted small">{l.sentAt ?? '—'}</td>
                    <td className="nowrap muted small">{l.deliveredAt ?? '—'}</td>
                    <td className="nowrap muted small">{l.readAt ?? '—'}</td>
                    <td>{l.escalation && <Badge tone="warn">{t(S.escalation)}</Badge>}</td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>
    </>
  );
}
