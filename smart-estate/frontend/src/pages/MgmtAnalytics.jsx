import { data } from '../lib/page.js';
import { t, field } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Stat, Card, EnumBadge, Button, EmptyState, TableWrap, Bar } from '../components/ui.jsx';

export const name = 'mgmt-analytics';
export const title = { en: 'Delivery Analytics', zh: '發送數據分析' };

const S = {
  head:         { en: 'Delivery Analytics',            zh: '發送數據分析' },
  attempts:     { en: 'Total attempts',                zh: '發送次數總計' },
  delivered:    { en: 'Delivered',                     zh: '已送達' },
  read:         { en: 'Read',                          zh: '已閱讀' },
  failed:       { en: 'Failed',                        zh: '發送失敗' },
  deliveryRate: { en: 'Delivery rate',                 zh: '送達率' },
  readRate:     { en: 'Read rate',                     zh: '閱讀率' },
  byChannel:    { en: 'By channel',                    zh: '按渠道劃分' },
  noData:       { en: 'No delivery data yet',          zh: '暫未有發送數據' },
  attemptsCol:  { en: 'Attempts',                      zh: '發送次數' },
  perNotif:     { en: 'Per notification',              zh: '按通知劃分' },
  noNotifs:     { en: 'No dispatched notifications yet', zh: '暫未有已發送的通知' },
  recipients:   { en: 'Recipients',                    zh: '收件者' },
};

export default function MgmtAnalytics() {
  const d = data;
  const overall = d.overall || {};
  const channels = overall.perChannel || [];
  const rows = d.rows || [];

  return (
    <>
      <PageHead title={t(S.head)} />

      <div className="grid grid--4" style={{ marginBottom: 16 }}>
        <Stat label={t(S.attempts)} value={overall.attempts ?? 0} icon="send" />
        <Stat label={t(S.delivered)} value={overall.delivered ?? 0} icon="check" tone="ok" />
        <Stat label={t(S.read)} value={overall.read ?? 0} icon="inbox" tone="brand" />
        <Stat label={t(S.failed)} value={overall.failed ?? 0} icon="alert"
              tone={overall.failed > 0 ? 'danger' : undefined} />
      </div>

      <div className="stack">
        <Card title={t(S.deliveryRate)} icon="chart">
          <div className="stack">
            <div>
              <div className="field__label">{t(S.deliveryRate)}</div>
              <Bar pct={overall.deliveryRatePct ?? 0} showLabel />
            </div>
            <div>
              <div className="field__label">{t(S.readRate)}</div>
              <Bar pct={overall.readRatePct ?? 0} showLabel />
            </div>
          </div>
        </Card>

        <Card title={t(S.byChannel)} icon="sliders" flush>
          {channels.length === 0
            ? <EmptyState icon="inbox" title={t(S.noData)} />
            : (
              <TableWrap>
                <thead><tr><th>{t(C.channel)}</th><th>{t(S.attemptsCol)}</th><th>{t(S.delivered)}</th><th>{t(S.read)}</th><th>{t(S.failed)}</th></tr></thead>
                <tbody>
                  {channels.map((c, idx) => (
                    <tr key={idx}>
                      <td><EnumBadge type="Channel" value={c.channel} /></td>
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

        <Card title={t(S.perNotif)} icon="bell" flush>
          {rows.length === 0
            ? <EmptyState icon="inbox" title={t(S.noNotifs)} />
            : (
              <TableWrap>
                <thead><tr><th>{t(C.createdAt)}</th><th>{t(C.title)}</th><th>{t(C.category)}</th><th>{t(S.recipients)}</th><th>{t(S.deliveryRate)}</th><th>{t(S.readRate)}</th></tr></thead>
                <tbody>
                  {rows.map((r) => (
                    <tr key={r.id}>
                      <td className="nowrap muted small">{r.createdAt}</td>
                      <td><a className="strong" href={`/mgmt/notifications/${r.id}`}>{field(r, 'title')}</a></td>
                      <td><EnumBadge type="NoticeCategory" value={r.category} dot={false} /></td>
                      <td className="num">{r.recipients}</td>
                      <td style={{ minWidth: 140 }}><Bar pct={r.deliveryRatePct ?? 0} showLabel /></td>
                      <td className="num">{r.readRatePct ?? 0}%</td>
                    </tr>
                  ))}
                </tbody>
              </TableWrap>
            )}
        </Card>
      </div>
    </>
  );
}
