import { data, user } from '../lib/page.js';
import { enumLabel } from '../lib/enums.js';
import { PageHead, Stat, Card, Badge, EnumBadge, Button, EmptyState, TableWrap, Icon } from '../components/ui.jsx';
import { t, field } from '../lib/i18n.js';
import { C } from '../lib/strings.js';

export const name = 'resident-dashboard';
export const title = { en: 'Dashboard', zh: '概覽' };

const S = {
  welcome:      { en: 'Welcome, {name}',        zh: '{name}，歡迎' },
  yourUnit:     { en: 'Your unit · {code}',     zh: '您的單位 · {code}' },
  noUnit:       { en: 'No unit assigned',       zh: '未編配單位' },
  reportIssue:  { en: 'Report an issue',        zh: '報告問題' },
  unreadNotices:{ en: 'Unread notices',         zh: '未讀通告' },
  openIssues:   { en: 'Open issues',            zh: '待處理問題' },
  feeStatus:    { en: 'Fee status',             zh: '費用狀況' },
  recentNotifs: { en: 'Recent notifications',   zh: '最近通知' },
  fullInbox:    { en: 'Full inbox',             zh: '完整收件匣' },
  noNotifs:     { en: 'No notifications yet',   zh: '暫時未有通知' },
  received:     { en: 'Received',               zh: '收到時間' },
  myOpenIssues: { en: 'My open issues',         zh: '我的待處理問題' },
  allIssues:    { en: 'All issues',             zh: '所有問題' },
  noOpenIssues: { en: 'No open issues',         zh: '沒有待處理問題' },
  allGood:      { en: 'Everything looks good.', zh: '一切正常。' },
  reportOne:    { en: 'Report one »',           zh: '報告問題 »' },
  ref:          { en: 'Ref',                    zh: '編號' },
  upcoming:     { en: 'Upcoming maintenance',   zh: '即將進行的維修保養' },
  estateWide:   { en: 'Affecting the estate',   zh: '影響屋苑範圍' },
  nothingSched: { en: 'Nothing scheduled',      zh: '暫無安排' },
  work:         { en: 'Work',                   zh: '工程' },
  affected:     { en: 'Affected',               zh: '影響範圍' },
};

export default function ResidentDashboard() {
  const d = data;
  const openIssues = (d.issues || []).filter((i) => i.status !== 'RESOLVED' && i.status !== 'WITHDRAWN');

  return (
    <>
      <PageHead
        title={t(S.welcome, { name: user?.name || t(C.resident) })}
        sub={d.unitFullCode ? t(S.yourUnit, { code: d.unitFullCode }) : t(S.noUnit)}
        actions={<Button href="/resident/issues/new" variant="primary" icon="plus">{t(S.reportIssue)}</Button>}
      />

      <div className="grid grid--3" style={{ marginBottom: 16 }}>
        <Stat label={t(S.unreadNotices)} value={d.unreadCount ?? 0} icon="inbox"
              tone={d.unreadCount > 0 ? 'warn' : undefined} />
        <Stat label={t(S.openIssues)} value={d.openIssueCount ?? 0} icon="wrench"
              tone={d.openIssueCount > 0 ? 'warn' : undefined} />
        <Stat label={t(S.feeStatus)} icon="dollar"
              tone={d.unitFeeStatus ? { OVERDUE: 'danger', DUE: 'warn', PAID: 'ok' }[d.unitFeeStatus] : undefined}
              value={d.unitFeeStatus ? enumLabel('FeeStatus', d.unitFeeStatus) : '—'} />
      </div>

      <div className="stack">
        <Card title={t(S.recentNotifs)} icon="bell"
              actions={<Button href="/resident/inbox" variant="ghost" size="sm" iconRight="chevronRight">{t(S.fullInbox)}</Button>}
              flush>
          {(!d.inbox || d.inbox.length === 0)
            ? <EmptyState icon="inbox" title={t(S.noNotifs)} />
            : (
              <TableWrap>
                <thead><tr><th>{t(S.received)}</th><th>{t(C.title)}</th><th>{t(C.category)}</th><th>{t(C.status)}</th><th /></tr></thead>
                <tbody>
                  {d.inbox.map((it) => (
                    <tr key={it.id}>
                      <td className="nowrap muted small">{it.dispatchedAt}</td>
                      <td>
                        {!it.read && <span className="unread-dot" />}
                        <a className="strong" href={`/resident/inbox/${it.id}`}>{field(it, 'title')}</a>
                      </td>
                      <td><EnumBadge type="NoticeCategory" value={it.category} dot={false} /></td>
                      <td><EnumBadge type="DeliveryStatus" value={it.status} /></td>
                      <td className="right"><a href={`/resident/inbox/${it.id}`}>{t(C.view)}</a></td>
                    </tr>
                  ))}
                </tbody>
              </TableWrap>
            )}
        </Card>

        <Card title={t(S.myOpenIssues)} icon="wrench"
              actions={<Button href="/resident/issues" variant="ghost" size="sm" iconRight="chevronRight">{t(S.allIssues)}</Button>}
              flush>
          {openIssues.length === 0
            ? <EmptyState icon="check" title={t(S.noOpenIssues)}>{t(S.allGood)} <a href="/resident/issues/new">{t(S.reportOne)}</a></EmptyState>
            : (
              <TableWrap>
                <thead><tr><th>{t(S.ref)}</th><th>{t(C.category)}</th><th>{t(C.status)}</th><th>{t(C.priority)}</th></tr></thead>
                <tbody>
                  {openIssues.map((i) => (
                    <tr key={i.id}>
                      <td><a className="mono strong" href={`/resident/issues/${i.id}`}>{i.refNumber}</a></td>
                      <td>{enumLabel('IssueCategory', i.category)}</td>
                      <td><EnumBadge type="IssueStatus" value={i.status} /></td>
                      <td><EnumBadge type="IssuePriority" value={i.priority} /></td>
                    </tr>
                  ))}
                </tbody>
              </TableWrap>
            )}
        </Card>

        <Card title={t(S.upcoming)} icon="calendar" sub={t(S.estateWide)} flush>
          {(!d.schedules || d.schedules.length === 0)
            ? <EmptyState icon="calendar" title={t(S.nothingSched)} />
            : (
              <TableWrap>
                <thead><tr><th>{t(C.date)}</th><th>{t(S.work)}</th><th>{t(S.affected)}</th></tr></thead>
                <tbody>
                  {d.schedules.map((s, i) => (
                    <tr key={i}>
                      <td className="nowrap">{s.date}</td>
                      <td className="strong">{s.title}</td>
                      <td className="muted">{s.affectedScope}</td>
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
