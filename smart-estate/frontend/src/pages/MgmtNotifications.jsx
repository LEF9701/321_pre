import { data } from '../lib/page.js';
import { t, field } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Card, Badge, EnumBadge, Button, EmptyState, TableWrap, Bar } from '../components/ui.jsx';

export const name = 'mgmt-notifications';
export const title = { en: 'Notifications', zh: '通知' };

const S = {
  head:       { en: 'Notifications',                        zh: '通知' },
  compose:    { en: 'Compose',                              zh: '撰寫通知' },
  empty:      { en: 'No notifications yet',                 zh: '暫未有通知' },
  emptyHint:  { en: 'Compose your first notice to residents.', zh: '撰寫第一則通知發送給住戶。' },
  scope:      { en: 'Scope',                                zh: '對象範圍' },
  recipients: { en: 'Recipients',                           zh: '收件者' },
  delivered:  { en: 'Delivered',                            zh: '送達率' },
  dispatched: { en: 'Dispatched',                           zh: '已發送' },
  scheduled:  { en: 'Scheduled {at}',                       zh: '預定 {at}' },
};

export default function MgmtNotifications() {
  const rows = data.rows || [];

  return (
    <>
      <PageHead
        title={t(S.head)}
        actions={<Button href="/mgmt/notifications/new" variant="primary" icon="plus">{t(S.compose)}</Button>}
      />

      <Card flush>
        {rows.length === 0
          ? <EmptyState icon="bell" title={t(S.empty)}>{t(S.emptyHint)}</EmptyState>
          : (
            <TableWrap>
              <thead>
                <tr>
                  <th>{t(C.createdAt)}</th>
                  <th>{t(C.title)}</th>
                  <th>{t(C.category)}</th>
                  <th>{t(S.scope)}</th>
                  <th>{t(C.status)}</th>
                  <th>{t(S.recipients)}</th>
                  <th>{t(S.delivered)}</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.id}>
                    <td className="nowrap muted small">{r.createdAt}</td>
                    <td><a className="strong" href={`/mgmt/notifications/${r.id}`}>{field(r, 'title')}</a></td>
                    <td><EnumBadge type="NoticeCategory" value={r.category} dot={false} /></td>
                    <td className="muted">{r.recipientScope}</td>
                    <td>
                      {r.dispatched
                        ? <Badge tone="ok">{t(S.dispatched)}</Badge>
                        : <Badge tone="warn">{t(S.scheduled, { at: r.scheduledAt })}</Badge>}
                    </td>
                    <td className="num">{r.recipients}</td>
                    <td><Bar pct={r.deliveryRatePct} showLabel /></td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>
    </>
  );
}
