import { useState } from 'react';
import { data, flash } from '../lib/page.js';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { enumOptions } from '../lib/enums.js';
import { PageHead, Card, Flash, Alert, EnumBadge, Field, Input, Select, Button, Csrf, EmptyState, TableWrap, DL } from '../components/ui.jsx';

export const name = 'resident-access';
export const title = { en: 'Access card', zh: '門禁卡' };

const S = {
  head:    { en: 'My access card', zh: '我的門禁卡' },
  sub:     { en: 'Your Octopus card for building-entrance and lift access. Changes need management approval.',
             zh: '你用於大廈入口及升降機的八達通卡。更改須經管理處批准。' },
  cardTitle:{ en: 'Registered card', zh: '已登記卡片' },
  cardNumber:{ en: 'Card number', zh: '卡號' },
  registered:{ en: 'Registered', zh: '登記日期' },
  noCard:  { en: 'No card registered', zh: '未登記卡片' },
  noCardHint:{ en: 'Submit a request below to register your Octopus card.', zh: '請於下方提交申請以登記你的八達通卡。' },
  request: { en: 'Request a change', zh: '申請更改' },
  requestHint:{ en: 'Enter the number printed on the back of your Octopus card. A committee member will review it.',
                zh: '請輸入八達通卡背面的號碼，委員會成員將會審核。' },
  action:  { en: 'Request type', zh: '申請類型' },
  submit:  { en: 'Submit for approval', zh: '提交審批' },
  history: { en: 'My requests', zh: '我的申請' },
  noReq:   { en: 'No requests yet', zh: '未有申請' },
  when:    { en: 'Submitted', zh: '提交時間' },
  privacy: { en: 'Access records are used only for estate security and are visible to management, not to other residents.',
             zh: '門禁紀錄僅用於屋苑保安，只有管理處可查看，其他住戶無法查看。' },
};

export default function ResidentAccess() {
  const card = data.card;
  const requests = data.requests || [];
  const [action, setAction] = useState('ADD');
  const needsNumber = action !== 'REMOVE';

  return (
    <>
      <PageHead title={t(S.head)} sub={t(S.sub)} />
      <Flash flash={flash} />

      <div className="grid grid--2" style={{ alignItems: 'start' }}>
        <Card title={t(S.cardTitle)} icon="shield">
          {card
            ? <DL items={[
                [t(S.cardNumber), <span className="mono">{card.cardNumber}</span>],
                [t(C.status), <EnumBadge type="CardStatus" value={card.status} />],
                [t(S.registered), card.registeredAt],
              ]} />
            : <EmptyState icon="shield" title={t(S.noCard)}>{t(S.noCardHint)}</EmptyState>}
        </Card>

        <Card title={t(S.request)} icon="edit">
          <form method="post" action="/resident/access/request">
            <Csrf />
            <Field label={t(S.action)} htmlFor="action">
              <Select id="action" name="action" value={action}
                      onChange={(e) => setAction(e.target.value)}
                      options={enumOptions('CardRequestAction')} />
            </Field>
            {needsNumber && (
              <Field label={t(S.cardNumber)} hint={t(S.requestHint)} htmlFor="cardNumber">
                <Input id="cardNumber" name="cardNumber" className="mono" placeholder="e.g. 88220011" />
              </Field>
            )}
            <Button type="submit" variant="primary" icon="check">{t(S.submit)}</Button>
          </form>
        </Card>
      </div>

      <Card title={t(S.history)} icon="history" flush style={{ marginTop: 16 }}>
        {requests.length === 0
          ? <EmptyState icon="history" title={t(S.noReq)} />
          : (
            <TableWrap>
              <thead>
                <tr>
                  <th>{t(S.when)}</th>
                  <th>{t(S.action)}</th>
                  <th>{t(S.cardNumber)}</th>
                  <th>{t(C.status)}</th>
                </tr>
              </thead>
              <tbody>
                {requests.map((r) => (
                  <tr key={r.id}>
                    <td className="nowrap muted small">{r.createdAt}</td>
                    <td><EnumBadge type="CardRequestAction" value={r.action} dot={false} /></td>
                    <td className="mono small">{r.cardNumber || '—'}</td>
                    <td><EnumBadge type="CardRequestStatus" value={r.status} /></td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>

      <Alert tone="info" icon="shield">{t(S.privacy)}</Alert>
    </>
  );
}
