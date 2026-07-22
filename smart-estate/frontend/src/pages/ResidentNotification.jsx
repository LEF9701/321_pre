import { data } from '../lib/page.js';
import { PageHead, Card, EnumBadge, TableWrap, DL } from '../components/ui.jsx';
import { t, field } from '../lib/i18n.js';
import { C } from '../lib/strings.js';

export const name = 'resident-notification';

const S = {
  inbox:       { en: 'Inbox',     zh: '收件匣' },
  sent:        { en: 'Sent',      zh: '發送時間' },
  from:        { en: 'From',      zh: '發送者' },
  notice:      { en: 'Notice',    zh: '通告' },
  delivery:    { en: 'Delivery to you', zh: '送遞至您的紀錄' },
  deliverySub: { en: 'How this notice was routed to you and the delivery outcome.',
                 zh: '此通告如何路由至您，以及送遞的結果。' },
  address:     { en: 'Address',   zh: '收件地址' },
  sentAt:      { en: 'Sent',      zh: '發送時間' },
  deliveredAt: { en: 'Delivered', zh: '送達時間' },
  readAt:      { en: 'Read',      zh: '閱讀時間' },
};

export default function ResidentNotification() {
  const d = data;
  const n = d.n || {};
  const logs = d.logs || [];

  return (
    <>
      <PageHead title={field(n, 'title')} back={{ href: '/resident/inbox', label: S.inbox }} />

      <div className="stack">
        <Card>
          <DL items={[
            [t(C.category), <EnumBadge type="NoticeCategory" value={n.category} />],
            [t(C.severity), <EnumBadge type="Severity" value={n.severity} />],
            [t(S.sent), n.dispatchedAt],
            [t(S.from), n.createdByName],
          ]} />
        </Card>

        <Card title={t(S.notice)} icon="bell">
          <p style={{ whiteSpace: 'pre-wrap' }}>{field(n, 'content')}</p>
        </Card>

        <Card title={t(S.delivery)} icon="send"
              sub={t(S.deliverySub)} flush>
          <TableWrap>
            <thead>
              <tr>
                <th>{t(C.channel)}</th>
                <th>{t(S.address)}</th>
                <th>{t(C.status)}</th>
                <th>{t(S.sentAt)}</th>
                <th>{t(S.deliveredAt)}</th>
                <th>{t(S.readAt)}</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((l, i) => (
                <tr key={i}>
                  <td><EnumBadge type="Channel" value={l.channel} dot={false} /></td>
                  <td className="mono small">{l.recipientAddress}</td>
                  <td><EnumBadge type="DeliveryStatus" value={l.status} /></td>
                  <td className="nowrap muted">{l.sentAt || '—'}</td>
                  <td className="nowrap muted">{l.deliveredAt || '—'}</td>
                  <td className="nowrap muted">{l.readAt || '—'}</td>
                </tr>
              ))}
            </tbody>
          </TableWrap>
        </Card>
      </div>
    </>
  );
}
