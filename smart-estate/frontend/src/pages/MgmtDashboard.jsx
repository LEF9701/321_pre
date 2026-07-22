import { data } from '../lib/page.js';
import { enumLabel } from '../lib/enums.js';
import { t, field } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Stat, Card, Badge, EnumBadge, Button, EmptyState, TableWrap } from '../components/ui.jsx';

export const name = 'mgmt-dashboard';
export const title = { en: 'Overview', zh: '總覽' };

const S = {
  head:         { en: 'Management Overview',    zh: '管理處總覽' },
  compose:      { en: 'Compose',                zh: '撰寫通知' },
  openIssues:   { en: 'Open issues',            zh: '待處理個案' },
  overdueUnits: { en: 'Overdue units',          zh: '逾期未繳單位' },
  recentIssues: { en: 'Recent issues',          zh: '最近問題個案' },
  noIssues:     { en: 'No issues yet',          zh: '暫未有問題個案' },
  ref:          { en: 'Ref',                    zh: '編號' },
  reporter:     { en: 'Reporter',               zh: '報告人' },
  recentNotifs: { en: 'Recent notifications',   zh: '最近通知' },
  noNotifs:     { en: 'No notifications yet',   zh: '暫未有通知' },
  dispatched:   { en: 'Dispatched',             zh: '已發送' },
  scheduled:    { en: 'Scheduled {at}',         zh: '預定 {at}' },
};

export default function MgmtDashboard() {
  const d = data;
  const issues = d.recentIssues || [];
  const notifs = d.recentNotifications || [];

  return (
    <>
      <PageHead
        title={t(S.head)}
        actions={<Button href="/mgmt/notifications/new" variant="primary" icon="plus">{t(S.compose)}</Button>}
      />

      <div className="grid grid--3" style={{ marginBottom: 16 }}>
        <Stat label={t(C.residents)} value={d.residentCount ?? 0} icon="users" />
        <Stat label={t(S.openIssues)} value={d.openIssues ?? 0} icon="wrench"
              tone={d.openIssues > 0 ? 'warn' : undefined} />
        <Stat label={t(S.overdueUnits)} value={d.overdueUnits ?? 0} icon="dollar"
              tone={d.overdueUnits > 0 ? 'danger' : undefined} />
      </div>

      <div className="stack">
        <Card title={t(S.recentIssues)} icon="wrench" flush>
          {issues.length === 0
            ? <EmptyState icon="check" title={t(S.noIssues)} />
            : (
              <TableWrap>
                <thead><tr><th>{t(S.ref)}</th><th>{t(C.category)}</th><th>{t(C.status)}</th><th>{t(C.priority)}</th><th>{t(S.reporter)}</th></tr></thead>
                <tbody>
                  {issues.map((i) => (
                    <tr key={i.id}>
                      <td><a className="mono strong" href={`/mgmt/issues/${i.id}`}>{i.refNumber}</a></td>
                      <td>{enumLabel('IssueCategory', i.category)}</td>
                      <td><EnumBadge type="IssueStatus" value={i.status} /></td>
                      <td><EnumBadge type="IssuePriority" value={i.priority} /></td>
                      <td className="muted">{i.reporterName || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </TableWrap>
            )}
        </Card>

        <Card title={t(S.recentNotifs)} icon="bell" flush>
          {notifs.length === 0
            ? <EmptyState icon="inbox" title={t(S.noNotifs)} />
            : (
              <TableWrap>
                <thead><tr><th>{t(C.createdAt)}</th><th>{t(C.title)}</th><th>{t(C.category)}</th><th>{t(C.status)}</th></tr></thead>
                <tbody>
                  {notifs.map((n) => (
                    <tr key={n.id}>
                      <td className="nowrap muted small">{n.createdAt}</td>
                      <td><a className="strong" href={`/mgmt/notifications/${n.id}`}>{field(n, 'title')}</a></td>
                      <td><EnumBadge type="NoticeCategory" value={n.category} dot={false} /></td>
                      <td>
                        {n.dispatched
                          ? <Badge tone="ok">{t(S.dispatched)}</Badge>
                          : <Badge tone="warn">{t(S.scheduled, { at: n.scheduledAt })}</Badge>}
                      </td>
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
