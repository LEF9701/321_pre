import { data, flash } from '../lib/page.js';
import { cx, initials } from '../lib/page.js';
import { enumLabel, enumOptions } from '../lib/enums.js';
import { PageHead, Card, Badge, EnumBadge, Button, Field, Input, Textarea, Select, Csrf, EmptyState, DL, Flash } from '../components/ui.jsx';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';

export const name = 'mgmt-issue-detail';

const S = {
  head:          { en: 'Issue {ref}',   zh: '個案 {ref}' },
  backToIssues:  { en: 'Issues',        zh: '問題個案' },

  reporter:      { en: 'Reporter',      zh: '報告人' },
  assignedTo:    { en: 'Assigned to',   zh: '指派給' },
  reported:      { en: 'Reported',      zh: '報告時間' },
  note:          { en: 'Note',          zh: '備註' },
  photoAlt:      { en: 'Issue photo',   zh: '個案相片' },

  updateStatus:  { en: 'Update status', zh: '更新狀態' },
  noteHint:      { en: 'Resident auto-notified (FR-5.5).', zh: '住戶會自動收到通知（FR-5.5）。' },
  notePlaceholder: { en: 'Note to resident', zh: '給住戶的備註' },

  setPriority:   { en: 'Set priority',  zh: '設定優先次序' },

  assign:        { en: 'Assign',        zh: '指派' },
  assignee:      { en: 'Assignee',      zh: '負責人' },
  assigneePlaceholder: { en: 'e.g. Evergreen Maintenance Team', zh: '例如：常青維修隊' },

  conversation:  { en: 'Conversation',  zh: '對話紀錄' },
  conversationSub: { en: 'Updates and replies between the resident and management.',
                     zh: '住戶與管理處之間的更新及回覆。' },
  noComments:    { en: 'No comments yet.', zh: '暫時未有留言。' },
  replyPlaceholder: { en: 'Reply to the resident…', zh: '回覆住戶…' },
  reply:         { en: 'Reply',         zh: '回覆' },
};

export default function MgmtIssueDetail() {
  const i = data.issue || {};
  const issueId = data.issueId ?? i.id;
  const comments = data.comments || [];
  const action = (suffix) => `/mgmt/issues/${issueId}/${suffix}`;

  return (
    <>
      <PageHead title={t(S.head, { ref: i.refNumber })} back={{ href: '/mgmt/issues', label: S.backToIssues }} />

      <Flash flash={flash} />

      <Card>
        <DL items={[
          [t(S.reporter), i.reporterName],
          [t(C.unit), i.unitFullCode],
          [t(C.category), enumLabel('IssueCategory', i.category)],
          [t(C.status), <EnumBadge type="IssueStatus" value={i.status} />],
          [t(C.priority), <EnumBadge type="IssuePriority" value={i.priority} />],
          [t(S.assignedTo), i.assignedTo],
          [t(S.reported), i.createdAt],
          [t(C.description), i.description],
          i.resolutionNote && [t(S.note), i.resolutionNote],
          i.photoFilename && [t(C.photo),
            <img
              src={'/uploads/' + i.photoFilename}
              style={{ maxWidth: 320, borderRadius: 8, border: '1px solid var(--border)' }}
              alt={t(S.photoAlt)}
            />],
        ]} />
      </Card>

      <div className="grid grid--3">
        <Card title={t(S.updateStatus)} icon="edit">
          <form method="post" action={action('status')}>
            <Csrf />
            <Field label={t(C.status)} htmlFor="status">
              <Select id="status" name="status" options={enumOptions('IssueStatus')} defaultValue={i.status} />
            </Field>
            <Field label={t(S.note)} htmlFor="note" hint={t(S.noteHint)}>
              <Textarea id="note" name="note" rows={2} placeholder={t(S.notePlaceholder)} />
            </Field>
            <Button type="submit" variant="primary">{t(S.updateStatus)}</Button>
          </form>
        </Card>

        <Card title={t(S.setPriority)} icon="alert">
          <form method="post" action={action('priority')}>
            <Csrf />
            <Field label={t(C.priority)} htmlFor="priority">
              <Select id="priority" name="priority" options={enumOptions('IssuePriority')} defaultValue={i.priority} />
            </Field>
            <Button type="submit" variant="primary">{t(S.setPriority)}</Button>
          </form>
        </Card>

        <Card title={t(S.assign)} icon="users">
          <form method="post" action={action('assign')}>
            <Csrf />
            <Field label={t(S.assignee)} htmlFor="assignee">
              <Input id="assignee" name="assignee" defaultValue={i.assignedTo} placeholder={t(S.assigneePlaceholder)} />
            </Field>
            <Button type="submit" variant="primary">{t(S.assign)}</Button>
          </form>
        </Card>
      </div>

      <Card title={t(S.conversation)} icon="mail" sub={t(S.conversationSub)}>
        {comments.length === 0
          ? <EmptyState icon="mail" title={t(S.noComments)} />
          : (
            <div style={{ marginBottom: 4 }}>
              {comments.map((c, idx) => (
                <div className="chat-msg" key={idx}>
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

        <form method="post" action={action('comment')}>
          <Csrf />
          <Textarea name="body" rows={3} required placeholder={t(S.replyPlaceholder)} />
          <div style={{ marginTop: 10 }}>
            <Button type="submit" variant="primary" icon="send">{t(S.reply)}</Button>
          </div>
        </form>
      </Card>
    </>
  );
}
