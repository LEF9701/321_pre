import { data, flash } from '../lib/page.js';
import { cx, initials } from '../lib/page.js';
import { enumLabel, ISSUE_STATUS_ORDER } from '../lib/enums.js';
import { PageHead, Card, Badge, EnumBadge, Button, Textarea, Csrf, EmptyState, Flash, DL } from '../components/ui.jsx';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';

export const name = 'resident-issue-detail';

const S = {
  head:          { en: 'Issue {ref}', zh: '問題 {ref}' },
  myIssues:      { en: 'My Issues', zh: '我的問題個案' },
  withdrawAsk:   { en: 'Withdraw this issue? This cannot be undone.',
                   zh: '確定撤回此問題？此操作無法復原。' },
  withdraw:      { en: 'Withdraw this issue', zh: '撤回此問題' },
  withdrawnSub:  { en: 'This issue was withdrawn and is no longer being processed.',
                   zh: '此問題已撤回，不會再作處理。' },
  withdrawn:     { en: 'Withdrawn', zh: '已撤回' },
  workflow:      { en: 'Status workflow', zh: '狀態流程' },
  workflowSub:   { en: 'Submitted → Under Review → In Progress → Resolved. You are notified whenever the status changes.',
                   zh: '已提交 → 審核中 → 處理中 → 已解決。狀態每次更新時，您都會收到通知。' },
  details:       { en: 'Issue details', zh: '問題詳情' },
  trackingNo:    { en: 'Tracking number', zh: '追蹤編號' },
  assignedTo:    { en: 'Assigned to', zh: '負責人' },
  notAssigned:   { en: 'Not yet assigned', zh: '尚未編配' },
  reported:      { en: 'Reported', zh: '報告時間' },
  lastUpdated:   { en: 'Last updated', zh: '最後更新' },
  mgmtNote:      { en: 'Management note', zh: '管理處備註' },
  photoAlt:      { en: 'issue photo', zh: '問題相片' },
  conversation:  { en: 'Conversation', zh: '對話紀錄' },
  conversationSub: { en: 'Updates and replies between you and management.',
                     zh: '您與管理處之間的更新及回覆。' },
  noComments:    { en: 'No comments yet.', zh: '暫時未有留言。' },
  commentHint:   { en: 'Add a comment or update for management…', zh: '向管理處新增留言或更新…' },
  postComment:   { en: 'Post comment', zh: '發表留言' },
};

export default function ResidentIssueDetail() {
  const d = data;
  const issue = d.issue || {};
  const issueId = d.issueId ?? issue.id;
  const comments = d.comments || [];
  const withdrawn = issue.status === 'WITHDRAWN';
  const currentIdx = ISSUE_STATUS_ORDER.indexOf(issue.status);

  return (
    <>
      <PageHead
        title={t(S.head, { ref: issue.refNumber })}
        back={{ href: '/resident/issues', label: S.myIssues }}
        actions={issue.status === 'SUBMITTED' ? (
          <form method="post" action={'/resident/issues/' + issueId + '/withdraw'}
                onSubmit={(e) => { if (!confirm(t(S.withdrawAsk))) e.preventDefault(); }}>
            <Csrf />
            <Button type="submit" variant="danger" icon="x">{t(S.withdraw)}</Button>
          </form>
        ) : undefined}
      />

      <Flash flash={flash} />

      <div className="stack">
        {withdrawn ? (
          <Card title={t(C.status)} icon="history" sub={t(S.withdrawnSub)}>
            <Badge tone="muted">{t(S.withdrawn)}</Badge>
          </Card>
        ) : (
          <Card title={t(S.workflow)} icon="history"
                sub={t(S.workflowSub)}>
            <div className="stepper">
              {ISSUE_STATUS_ORDER.map((s, i) => {
                const done = currentIdx >= 0 && i < currentIdx;
                const current = i === currentIdx;
                return (
                  <span key={s} style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
                    {i > 0 && <span className="arrow">→</span>}
                    <span className={cx('step', done && 'is-done', current && 'is-current')}>
                      <span className="n">{i + 1}</span>
                      {enumLabel('IssueStatus', s)}
                    </span>
                  </span>
                );
              })}
            </div>
          </Card>
        )}

        <Card title={t(S.details)} icon="wrench">
          <DL items={[
            [t(S.trackingNo), <span className="mono strong">{issue.refNumber}</span>],
            [t(C.category), enumLabel('IssueCategory', issue.category)],
            [t(C.status), <EnumBadge type="IssueStatus" value={issue.status} />],
            [t(C.priority), <EnumBadge type="IssuePriority" value={issue.priority} />],
            [t(S.assignedTo), issue.assignedTo || <span className="muted">{t(S.notAssigned)}</span>],
            [t(S.reported), issue.createdAt],
            [t(S.lastUpdated), issue.updatedAt],
            [t(C.description), <span style={{ whiteSpace: 'pre-wrap' }}>{issue.description}</span>],
            issue.resolutionNote
              ? [t(S.mgmtNote), <span style={{ whiteSpace: 'pre-wrap' }}>{issue.resolutionNote}</span>]
              : null,
          ]} />
        </Card>

        {issue.photoFilename && (
          <Card title={t(C.photo)} icon="file">
            <img src={`/uploads/${issue.photoFilename}`} alt={t(S.photoAlt)}
                 style={{ maxWidth: 320, borderRadius: 8, border: '1px solid var(--border)' }} />
          </Card>
        )}

        <Card title={t(S.conversation)} icon="mail" sub={t(S.conversationSub)}>
          {comments.length === 0
            ? <EmptyState icon="mail" title={t(S.noComments)} />
            : (
              <div style={{ marginBottom: 4 }}>
                {comments.map((c, i) => (
                  <div className="chat-msg" key={i}>
                    <div className={cx('chat-msg__avatar', c.authorRole === 'RESIDENT' ? 'me' : 'ai')}>
                      {initials(c.authorName)}
                    </div>
                    <div style={{ flex: 1 }}>
                      <div className="row" style={{ gap: 8, marginBottom: 6, alignItems: 'center' }}>
                        <span className="strong">{c.authorName}</span>
                        <Badge tone={c.authorRole === 'RESIDENT' ? 'muted' : 'brand'}>
                          {enumLabel('Role', c.authorRole)}
                        </Badge>
                        <span className="small muted">{c.createdAt}</span>
                      </div>
                      <div className="chat-bubble">{c.body}</div>
                    </div>
                  </div>
                ))}
              </div>
            )}

          <form method="post" action={'/resident/issues/' + issueId + '/comment'}>
            <Csrf />
            <Textarea name="body" rows={3} required placeholder={t(S.commentHint)} />
            <div style={{ marginTop: 10 }}>
              <Button type="submit" variant="primary" icon="send">{t(S.postComment)}</Button>
            </div>
          </form>
        </Card>
      </div>
    </>
  );
}
