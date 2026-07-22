import { data } from '../lib/page.js';
import { enumLabel } from '../lib/enums.js';
import { PageHead, Card, EnumBadge, Button, EmptyState, TableWrap } from '../components/ui.jsx';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';

export const name = 'resident-issues';
export const title = { en: 'My Issues', zh: '我的問題個案' };

const S = {
  head:        { en: 'My Issues', zh: '我的問題個案' },
  sub:         { en: 'Maintenance issues you have reported and their current status.',
                 zh: '您已報告的維修問題及其目前狀態。' },
  reportIssue: { en: 'Report an issue', zh: '報告問題' },
  none:        { en: 'You have not reported any issues yet', zh: '您尚未報告任何問題' },
  reportOne:   { en: 'Report one »', zh: '報告問題 »' },
  ref:         { en: 'Ref', zh: '編號' },
  reported:    { en: 'Reported', zh: '報告時間' },
  assignedTo:  { en: 'Assigned to', zh: '負責人' },
};

export default function ResidentIssues() {
  const d = data;
  const issues = d.issues || [];

  return (
    <>
      <PageHead title={t(S.head)}
                sub={t(S.sub)}
                actions={<Button href="/resident/issues/new" variant="primary" icon="plus">{t(S.reportIssue)}</Button>} />

      <Card flush>
        {issues.length === 0
          ? <EmptyState icon="wrench" title={t(S.none)}>
              <a href="/resident/issues/new">{t(S.reportOne)}</a>
            </EmptyState>
          : (
            <TableWrap>
              <thead>
                <tr>
                  <th>{t(S.ref)}</th>
                  <th>{t(S.reported)}</th>
                  <th>{t(C.category)}</th>
                  <th>{t(C.status)}</th>
                  <th>{t(C.priority)}</th>
                  <th>{t(S.assignedTo)}</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {issues.map((i) => (
                  <tr key={i.id}>
                    <td><a className="mono strong" href={`/resident/issues/${i.id}`}>{i.refNumber}</a></td>
                    <td className="nowrap muted small">{i.createdAt}</td>
                    <td>{enumLabel('IssueCategory', i.category)}</td>
                    <td><EnumBadge type="IssueStatus" value={i.status} /></td>
                    <td><EnumBadge type="IssuePriority" value={i.priority} /></td>
                    <td>{i.assignedTo || <span className="muted">—</span>}</td>
                    <td className="right">
                      <Button href={`/resident/issues/${i.id}`} variant="ghost" size="sm" iconRight="chevronRight">{t(C.view)}</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>
    </>
  );
}
