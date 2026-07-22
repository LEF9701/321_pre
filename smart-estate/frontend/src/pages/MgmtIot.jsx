import { useEffect, useMemo, useRef, useState } from 'react';
import { data, flash, csrf, cx } from '../lib/page.js';
import Icon from '../lib/icons.jsx';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { enumLabel } from '../lib/enums.js';
import { useTableControls } from '../lib/tablecontrols.js';
import { PageHead, Card, Stat, EnumBadge, EmptyState, TableWrap, Flash, Button, Csrf, SortTh, SearchBox } from '../components/ui.jsx';

export const name = 'mgmt-iot';
export const title = { en: 'Sensors', zh: '感應器' };

const S = {
  head:      { en: 'Sensor monitoring', zh: '感應器監控' },
  sub:       { en: 'Live status of the estate fire, water and door sensors. Updates in real time as devices report.',
               zh: '屋苑火警、漏水及門磁感應器的即時狀態，會隨裝置回報即時更新。' },
  live:      { en: 'Live',       zh: '即時' },
  offline:   { en: 'Reconnecting…', zh: '重新連線中…' },
  total:     { en: 'Sensors',    zh: '感應器總數' },
  alerts:    { en: 'Active alerts', zh: '生效中警報' },
  fireAlerts:{ en: 'Fire alerts', zh: '火警警報' },
  activeCard:{ en: 'Active alerts', zh: '生效中警報' },
  noAlerts:  { en: 'All sensors normal', zh: '所有感應器正常' },
  allCard:   { en: 'All sensors', zh: '全部感應器' },
  searchSensors: { en: 'Search device, location, type…', zh: '搜尋裝置、位置、類型⋯' },
  device:    { en: 'Device',     zh: '裝置' },
  type:      { en: 'Type',       zh: '類型' },
  location:  { en: 'Location',   zh: '位置' },
  lastEvent: { en: 'Last event', zh: '最近事件' },
  declare:   { en: 'Declare fire emergency', zh: '發佈火警緊急事故' },
  declareConfirm: { en: 'Declare a FIRE emergency for this location? This puts the system into elevated state and broadcasts to residents.',
                    zh: '確定就此位置發佈火警緊急事故？系統將進入戒備狀態並向住戶廣播。' },
};

function fmtTime(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

export default function MgmtIot() {
  // Keyed by id so live SSE updates replace the matching sensor in place.
  const [byId, setById] = useState(() => {
    const m = new Map();
    (data.sensors || []).forEach((s) => m.set(s.id, s));
    return m;
  });
  const [connected, setConnected] = useState(false);
  const [flashId, setFlashId] = useState(null);
  const flashTimer = useRef(null);

  useEffect(() => {
    const es = new EventSource('/mgmt/iot/stream');
    es.addEventListener('ready', () => setConnected(true));
    es.addEventListener('sensor', (e) => {
      let s;
      try { s = JSON.parse(e.data); } catch { return; }
      if (s && s.id != null) {
        setById((prev) => {
          const next = new Map(prev);
          next.set(s.id, { ...next.get(s.id), ...s });
          return next;
        });
        // briefly highlight the row that just changed
        setFlashId(s.id);
        clearTimeout(flashTimer.current);
        flashTimer.current = setTimeout(() => setFlashId(null), 1600);
      }
    });
    es.onerror = () => setConnected(false);
    return () => { es.close(); clearTimeout(flashTimer.current); };
  }, []);

  const sensors = useMemo(
    () => [...byId.values()].sort((a, b) => (a.code || '').localeCompare(b.code || '')),
    [byId],
  );
  const alerts = sensors.filter((s) => s.status === 'ALERT');
  const fireAlerts = alerts.filter((s) => s.fireType);

  // search + sort for the full sensor list (matches localised type/status labels too)
  const ctl = useTableControls(sensors, {
    searchText: (s) => `${s.code} ${s.location} ${enumLabel('SensorType', s.type)} ${enumLabel('SensorStatus', s.status)}`,
    initialSort: 'code', initialDir: 'asc',
  });

  return (
    <>
      <PageHead title={t(S.head)} sub={t(S.sub)}
                actions={
                  <span className={cx('badge', connected ? 'badge--ok' : 'badge--muted')}>
                    <span className="dot" style={connected ? { animation: 'pulse 1.4s infinite' } : undefined} />
                    {connected ? t(S.live) : t(S.offline)}
                  </span>
                } />

      <Flash flash={flash} />

      <div className="grid grid--3" style={{ marginBottom: 16 }}>
        <Stat label={t(S.total)} value={sensors.length} icon="activity" />
        <Stat label={t(S.alerts)} value={alerts.length} icon="alert" tone={alerts.length ? 'danger' : undefined} />
        <Stat label={t(S.fireAlerts)} value={fireAlerts.length} icon="flame" tone={fireAlerts.length ? 'danger' : undefined} />
      </div>

      <Card title={t(S.activeCard)} icon="alert" flush>
        {alerts.length === 0
          ? <EmptyState icon="check" title={t(S.noAlerts)} />
          : (
            <TableWrap>
              <thead>
                <tr>
                  <th>{t(S.device)}</th>
                  <th>{t(S.type)}</th>
                  <th>{t(S.location)}</th>
                  <th>{t(C.severity)}</th>
                  <th>{t(S.lastEvent)}</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {alerts.map((s) => (
                  <tr key={s.id} className={cx(flashId === s.id && 'row-flash')}>
                    <td className="mono small">{s.code}</td>
                    <td><EnumBadge type="SensorType" value={s.type} dot={false} /></td>
                    <td className="strong">{s.location}</td>
                    <td><EnumBadge type="Severity" value={s.severity} /></td>
                    <td className="nowrap muted small">{fmtTime(s.at)}</td>
                    <td className="nowrap">
                      {s.fireType && (
                        <form method="post" action={`/mgmt/iot/sensors/${s.id}/declare`}
                              style={{ display: 'inline' }}
                              onSubmit={(e) => { if (!confirm(t(S.declareConfirm))) e.preventDefault(); }}>
                          <Csrf />
                          <Button type="submit" variant="danger" size="sm" icon="flame">{t(S.declare)}</Button>
                        </form>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>

      <Card title={t(S.allCard)} icon="activity" flush
            actions={<SearchBox value={ctl.search} onChange={ctl.setSearch} placeholder={t(S.searchSensors)} />}>
        {sensors.length === 0
          ? <EmptyState icon="activity" title={t(C.nothingYet)} />
          : (
            <TableWrap>
              <thead>
                <tr>
                  <SortTh label={t(S.device)} k="code" ctl={ctl} />
                  <SortTh label={t(S.type)} k="type" ctl={ctl} />
                  <SortTh label={t(S.location)} k="location" ctl={ctl} />
                  <SortTh label={t(C.status)} k="status" ctl={ctl} />
                  <SortTh label={t(S.lastEvent)} k="at" ctl={ctl} />
                </tr>
              </thead>
              <tbody>
                {ctl.view.map((s) => (
                  <tr key={s.id} className={cx(flashId === s.id && 'row-flash')}>
                    <td className="mono small">{s.code}</td>
                    <td><EnumBadge type="SensorType" value={s.type} dot={false} /></td>
                    <td>{s.location}</td>
                    <td><EnumBadge type="SensorStatus" value={s.status} /></td>
                    <td className="nowrap muted small">{fmtTime(s.at)}</td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>
    </>
  );
}
