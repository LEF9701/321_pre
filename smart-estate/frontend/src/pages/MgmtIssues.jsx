import { data } from '../lib/page.js';
import { enumLabel, enumOptions } from '../lib/enums.js';
import { PageHead, Card, EnumBadge, EmptyState, TableWrap, Select, Field } from '../components/ui.jsx';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';

export const name = 'mgmt-issues';
export const title = { en: 'Issue Tracking', zh: '問題個案追蹤' };

const S = {
  head:        { en: 'Issue Tracking', zh: '問題個案追蹤' },
  headSub:     { en: 'View, assign and update issue tickets. (FR-2.4 / FR-5.x)',
                 zh: '檢視、指派及更新問題個案。（FR-2.4 / FR-5.x）' },
  filterLabel: { en: 'Filter by status', zh: '按狀態篩選' },
  allStatuses: { en: 'All statuses',     zh: '所有狀態' },
  empty:       { en: 'No issues found',  zh: '找不到問題個案' },
  ref:         { en: 'Ref',              zh: '個案編號' },
  reported:    { en: 'Reported',         zh: '報告時間' },
  assigned:    { en: 'Assigned',         zh: '指派對象' },
};

export default function MgmtIssues() {
  const d = data;
  const issues = d.issues || [];

  return (
    <>
      <PageHead title={t(S.head)} sub={t(S.headSub)} />

      <Card>
        <form method="get">
          <Field label={t(S.filterLabel)} htmlFor="status">
            <Select
              id="status"
              name="status"
              options={enumOptions('IssueStatus')}
              includeEmpty
              emptyLabel={t(S.allStatuses)}
              defaultValue={d.statusFilter || ''}
              onChange={(e) => e.target.form.submit()}
            />
          </Field>
        </form>
      </Card>

      <Card flush>
        {issues.length === 0
          ? <EmptyState icon="wrench" title={t(S.empty)} />
          : (
            <TableWrap>
              <thead>
                <tr>
                  <th>{t(S.ref)}</th>
                  <th>{t(S.reported)}</th>
                  <th>{t(C.unit)}</th>
                  <th>{t(C.category)}</th>
                  <th>{t(C.priority)}</th>
                  <th>{t(C.status)}</th>
                  <th>{t(S.assigned)}</th>
                </tr>
              </thead>
              <tbody>
                {issues.map((i) => (
                  <tr key={i.id}>
                    <td><a className="mono strong" href={`/mgmt/issues/${i.id}`}>{i.refNumber}</a></td>
                    <td className="nowrap muted small">{i.createdAt}</td>
                    <td>{i.unitFullCode || <span className="muted">—</span>}</td>
                    <td>{enumLabel('IssueCategory', i.category)}</td>
                    <td><EnumBadge type="IssuePriority" value={i.priority} /></td>
                    <td><EnumBadge type="IssueStatus" value={i.status} /></td>
                    <td>{i.assignedTo || <span className="muted">—</span>}</td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>
    </>
  );
}
