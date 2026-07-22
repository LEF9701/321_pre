import { data, flash } from '../lib/page.js';
import { enumLabel, enumOptions } from '../lib/enums.js';
import { PageHead, Card, Flash, Button, Field, Textarea, Select, Input, TableWrap, Csrf } from '../components/ui.jsx';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';

export const name = 'resident-issue-new';
export const title = { en: 'Report an Issue', zh: '報告問題' };

const S = {
  head:        { en: 'Report an Issue', zh: '報告問題' },
  sub:         { en: 'Submit with a category, description and optional photo. You will get a tracking number.',
                 zh: '請選擇類別、填寫描述，並可附上相片。提交後會獲發追蹤編號。' },
  myIssues:    { en: 'My Issues', zh: '我的問題個案' },
  aiTitle:     { en: 'AI suggestion', zh: 'AI 建議' },
  aiSub:       { en: `Predicted category preselected below — change it if it's wrong.`,
                 zh: '下方已預先選取建議類別，如不正確請自行更改。' },
  confidence:  { en: '(confidence {pct}%)', zh: '（信心度 {pct}%）' },
  ref:         { en: 'Ref', zh: '編號' },
  score:       { en: 'Score', zh: '分數' },
  descHint:    { en: 'Describe the problem, including the location and how long it has been happening.',
                 zh: '請描述問題，包括發生地點及已持續多久。' },
  photo:       { en: 'Photo (optional)', zh: '相片（選填）' },
  suggestAi:   { en: 'Suggest category with AI', zh: '以 AI 建議類別' },
  submitIssue: { en: 'Submit issue', zh: '提交問題' },
};

export default function ResidentIssueNew() {
  const d = data;
  const suggestion = d.suggestion || null;

  return (
    <>
      <PageHead title={t(S.head)}
                sub={t(S.sub)}
                back={{ href: '/resident/issues', label: S.myIssues }} />

      <Flash flash={flash} />

      {suggestion && (
        <Card title={t(S.aiTitle)} icon="sparkles"
              sub={t(S.aiSub)}>
          <p style={{ marginBottom: 12 }}>
            <strong>{enumLabel('IssueCategory', suggestion.predictedCategory)}</strong>
            {' '}<span className="muted">{t(S.confidence, { pct: suggestion.confidencePct })}</span>
          </p>
          {suggestion.neighbors && suggestion.neighbors.length > 0 && (
            <TableWrap>
              <thead>
                <tr><th>{t(S.ref)}</th><th>{t(C.category)}</th><th>{t(S.score)}</th></tr>
              </thead>
              <tbody>
                {suggestion.neighbors.map((nb, i) => (
                  <tr key={i}>
                    <td className="mono">{nb.refNumber}</td>
                    <td>{enumLabel('IssueCategory', nb.category)}</td>
                    <td className="num">{Number(nb.score).toFixed(2)}</td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
        </Card>
      )}

      <Card>
        <form method="post" action="/resident/issues" encType="multipart/form-data">
          <Csrf />
          <Field label={t(C.description)} htmlFor="description"
                 hint={t(S.descHint)} full>
            <Textarea id="description" name="description" rows={5} required defaultValue={d.description} />
          </Field>
          <Field label={t(C.category)} htmlFor="category">
            <Select id="category" name="category" required
                    options={enumOptions('IssueCategory')}
                    defaultValue={suggestion?.predictedCategory || ''} />
          </Field>
          <Field label={t(S.photo)} htmlFor="photo">
            <Input id="photo" type="file" name="photo" accept="image/*" />
          </Field>
          <div className="btn-row">
            <Button type="submit" variant="ghost" icon="sparkles"
                    formAction="/resident/issues/suggest"
                    formEncType="application/x-www-form-urlencoded">{t(S.suggestAi)}</Button>
            <Button type="submit" variant="primary" icon="send">{t(S.submitIssue)}</Button>
          </div>
        </form>
      </Card>
    </>
  );
}
