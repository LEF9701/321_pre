import { data } from '../lib/page.js';
import { PageHead, Card, EnumBadge, EmptyState, TableWrap } from '../components/ui.jsx';
import { t, field } from '../lib/i18n.js';
import { C } from '../lib/strings.js';

export const name = 'resident-inbox';
export const title = { en: 'Inbox', zh: '收件匣' };

const S = {
  head:     { en: 'Inbox', zh: '收件匣' },
  sub:      { en: 'Timestamped notices delivered to you, with the delivery status on your chosen channel(s).',
              zh: '已送達給您的通告及其時間紀錄，並顯示所選渠道的送遞狀態。' },
  noNotifs: { en: 'No notifications yet', zh: '暫時未有通知' },
  received: { en: 'Received', zh: '收到時間' },
};

export default function ResidentInbox() {
  const d = data;
  const inbox = d.inbox || [];

  return (
    <>
      <PageHead title={t(S.head)} sub={t(S.sub)} />

      <Card flush>
        {inbox.length === 0
          ? <EmptyState icon="inbox" title={t(S.noNotifs)} />
          : (
            <TableWrap>
              <thead>
                <tr>
                  <th>{t(S.received)}</th>
                  <th>{t(C.title)}</th>
                  <th>{t(C.category)}</th>
                  <th>{t(C.severity)}</th>
                  <th>{t(C.channels)}</th>
                  <th>{t(C.status)}</th>
                </tr>
              </thead>
              <tbody>
                {inbox.map((it) => (
                  <tr key={it.id}>
                    <td className="nowrap muted small">{it.dispatchedAt}</td>
                    <td>
                      {!it.read && <span className="unread-dot" />}
                      <a className="strong" href={`/resident/inbox/${it.id}`}>{field(it, 'title')}</a>
                    </td>
                    <td><EnumBadge type="NoticeCategory" value={it.category} dot={false} /></td>
                    <td><EnumBadge type="Severity" value={it.severity} /></td>
                    <td className="muted">{it.channels}</td>
                    <td><EnumBadge type="DeliveryStatus" value={it.status} /></td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>
    </>
  );
}
