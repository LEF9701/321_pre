import { useEffect, useRef, useState } from 'react';
import { data, flash, cx } from '../lib/page.js';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { enumLabel } from '../lib/enums.js';
import { useTableControls } from '../lib/tablecontrols.js';
import { PageHead, Card, Flash, Alert, EnumBadge, EmptyState, TableWrap, Button, Csrf, SortTh, SearchBox } from '../components/ui.jsx';

export const name = 'mgmt-access';
export const title = { en: 'Access control', zh: '門禁管理' };

const S = {
  head:      { en: 'Access control', zh: '門禁管理' },
  sub:       { en: 'Approve resident card registrations and monitor door access in real time.',
               zh: '審批住戶門禁卡登記，並即時監察門禁進出。' },
  live:      { en: 'Live',       zh: '即時' },
  offline:   { en: 'Reconnecting…', zh: '重新連線中…' },
  queue:     { en: 'Card registration approvals', zh: '門禁卡登記審批' },
  noPending: { en: 'No pending requests', zh: '沒有待審批申請' },
  resident:  { en: 'Resident', zh: '住戶' },
  action:    { en: 'Request', zh: '申請' },
  cardNumber:{ en: 'Card number', zh: '卡號' },
  submitted: { en: 'Submitted', zh: '提交時間' },
  approve:   { en: 'Approve', zh: '批准' },
  reject:    { en: 'Reject',  zh: '拒絕' },
  log:       { en: 'Access log', zh: '門禁紀錄' },
  logSub:    { en: 'Live card taps at building entrances and lifts. Card numbers are masked; movement data is management-only.',
               zh: '大廈入口及升降機的即時拍卡紀錄。卡號經遮蔽，出入紀錄僅供管理處查看。' },
  searchLog: { en: 'Search resident, card, door…', zh: '搜尋住戶、卡號、門口⋯' },
  noEvents:  { en: 'No access events yet', zh: '尚未有門禁紀錄' },
  time:      { en: 'Time', zh: '時間' },
  card:      { en: 'Card', zh: '卡片' },
  door:      { en: 'Door', zh: '門口' },
  type:      { en: 'Type', zh: '類型' },
  direction: { en: 'Direction', zh: '方向' },
  result:    { en: 'Result', zh: '結果' },
  privacy:   { en: 'Movement records are retained only for a limited period for estate security, and are never shared with other residents.',
               zh: '出入紀錄僅為屋苑保安而保留一段有限時間，絕不會向其他住戶披露。' },
};

function fmtTime(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

export default function MgmtAccess() {
  const pending = data.pending || [];
  const [events, setEvents] = useState(() => data.events || []);
  const [connected, setConnected] = useState(false);
  const [flashKey, setFlashKey] = useState(null);
  const flashTimer = useRef(null);

  const ctl = useTableControls(events, {
    searchText: (e) => `${e.resident || ''} ${e.card || ''} ${e.point || ''} ${enumLabel('AccessDecision', e.decision)} ${e.reason || ''}`,
    initialSort: 'at', initialDir: 'desc',
  });

  useEffect(() => {
    const es = new EventSource('/mgmt/iot/stream');
    es.addEventListener('ready', () => setConnected(true));
    es.addEventListener('access', (e) => {
      let ev;
      try { ev = JSON.parse(e.data); } catch { return; }
      setEvents((prev) => [ev, ...prev].slice(0, 100));
      setFlashKey(ev.id);
      clearTimeout(flashTimer.current);
      flashTimer.current = setTimeout(() => setFlashKey(null), 1600);
    });
    es.onerror = () => setConnected(false);
    return () => { es.close(); clearTimeout(flashTimer.current); };
  }, []);

  return (
    <>
      <PageHead title={t(S.head)} sub={t(S.sub)}
                actions={
                  <span className={cx('badge', connected ? 'badge--ok' : 'badge--muted')}>
                    <span className="dot" /> {connected ? t(S.live) : t(S.offline)}
                  </span>
                } />
      <Flash flash={flash} />

      <Card title={t(S.queue)} icon="check" flush>
        {pending.length === 0
          ? <EmptyState icon="check" title={t(S.noPending)} />
          : (
            <TableWrap>
              <thead>
                <tr>
                  <th>{t(S.submitted)}</th>
                  <th>{t(S.resident)}</th>
                  <th>{t(S.action)}</th>
                  <th>{t(S.cardNumber)}</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {pending.map((r) => (
                  <tr key={r.id}>
                    <td className="nowrap muted small">{r.createdAt}</td>
                    <td className="strong">{r.resident}<div className="muted small">{r.unit}</div></td>
                    <td><EnumBadge type="CardRequestAction" value={r.action} dot={false} /></td>
                    <td className="mono small">{r.cardNumber || '—'}</td>
                    <td className="nowrap">
                      <div className="btn-row">
                        <form method="post" action={`/mgmt/access/requests/${r.id}/approve`} style={{ display: 'inline' }}>
                          <Csrf />
                          <Button type="submit" variant="primary" size="sm" icon="check">{t(S.approve)}</Button>
                        </form>
                        <form method="post" action={`/mgmt/access/requests/${r.id}/reject`} style={{ display: 'inline' }}>
                          <Csrf />
                          <Button type="submit" variant="ghost" size="sm" icon="x">{t(S.reject)}</Button>
                        </form>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>

      <Card title={t(S.log)} icon="history" sub={t(S.logSub)} flush style={{ marginTop: 16 }}
            actions={<SearchBox value={ctl.search} onChange={ctl.setSearch} placeholder={t(S.searchLog)} />}>
        {events.length === 0
          ? <EmptyState icon="history" title={t(S.noEvents)} />
          : (
            <TableWrap>
              <thead>
                <tr>
                  <SortTh label={t(S.time)} k="at" ctl={ctl} />
                  <SortTh label={t(S.card)} k="card" ctl={ctl} />
                  <SortTh label={t(S.resident)} k="resident" ctl={ctl} />
                  <SortTh label={t(S.door)} k="point" ctl={ctl} />
                  <th>{t(S.type)}</th>
                  <th>{t(S.direction)}</th>
                  <SortTh label={t(S.result)} k="decision" ctl={ctl} />
                </tr>
              </thead>
              <tbody>
                {ctl.view.map((ev) => (
                  <tr key={ev.id} className={cx(flashKey === ev.id && 'row-flash')}>
                    <td className="nowrap muted small">{fmtTime(ev.at)}</td>
                    <td className="mono small">{ev.card}</td>
                    <td>{ev.resident || <span className="muted">—</span>}</td>
                    <td className="strong">{ev.point}</td>
                    <td><EnumBadge type="AccessPointType" value={ev.pointType} dot={false} /></td>
                    <td><EnumBadge type="AccessDirection" value={ev.direction} dot={false} /></td>
                    <td>
                      <EnumBadge type="AccessDecision" value={ev.decision} />
                      {ev.reason && <div className="muted small">{ev.reason}</div>}
                    </td>
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
