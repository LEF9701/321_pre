import { data } from '../lib/page.js';
import { enumLabel } from '../lib/enums.js';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Card, Stat, Badge, Bar, EmptyState, TableWrap } from '../components/ui.jsx';

export const name = 'ai-evaluation';
export const title = { en: 'AI Evaluation', zh: 'AI 評估' };

const S = {
  head:          { en: 'AI Evaluation', zh: 'AI 評估' },
  headSub:       { en: 'k-NN classifier + retrieval metrics (rp.pdf §5)',
                   zh: 'k-NN 分類器及檢索指標（rp.pdf §5）' },

  /* ---- classification ---- */
  autoCat:       { en: 'Issue auto-categorisation (FR-8.3)', zh: '問題自動分類（FR-8.3）' },
  testExamples:  { en: 'Test examples',   zh: '測試樣本' },
  correct:       { en: 'Correct',         zh: '正確' },
  accuracy:      { en: 'Accuracy',        zh: '準確度' },
  kHint:         { en: 'k={k}, train size={n} (evaluated on a held-out 25% split)',
                   zh: 'k={k}，訓練集 {n} 筆（以保留的 25% 測試集評估）' },
  perCategory:   { en: 'Per-category accuracy', zh: '各類別準確度' },
  noCategories:  { en: 'No categories evaluated', zh: '未有評估任何類別' },
  testItems:     { en: 'Test items',      zh: '測試項目' },
  predictions:   { en: 'Predictions',     zh: '預測結果' },
  noPredictions: { en: 'No predictions',  zh: '未有預測結果' },
  ref:           { en: 'Ref',             zh: '編號' },
  actual:        { en: 'Actual',          zh: '實際類別' },
  predicted:     { en: 'Predicted',       zh: '預測類別' },
  result:        { en: 'Result',          zh: '結果' },

  /* ---- retrieval ---- */
  retrieval:     { en: 'Retrieval (FR-8.1)', zh: '文件檢索（FR-8.1）' },
  recall1:       { en: 'Recall@1',        zh: '召回率@1' },
  recall3:       { en: 'Recall@3',        zh: '召回率@3' },
  recall5:       { en: 'Recall@5',        zh: '召回率@5' },
  docsServed:    { en: 'Docs served / question', zh: '每題引用文件數' },
  docsServedHint:{ en: 'distinct documents actually cited', zh: '實際引用的不重複文件數' },
  retrievalNote: { en: 'MRR {mrr} over {n} questions. Recall and MRR are measured on the unranked '
                     + 'document ordering; “docs served” counts the distinct documents that actually '
                     + 'reach the answer once the similarity threshold, top-K and the per-document cap '
                     + 'are applied.',
                   zh: '{n} 條問題的 MRR 為 {mrr}。召回率與 MRR 以未經排序的文件次序計算；'
                     + '「每題引用文件數」則統計在相似度門檻、top-K 及每份文件上限套用後，'
                     + '真正用於作答的不重複文件數。' },
  retrievalRows: { en: 'Retrieval results', zh: '檢索結果' },
  noQuestions:   { en: 'No questions evaluated', zh: '未有評估任何問題' },
  question:      { en: 'Question',        zh: '問題' },
  expectedDoc:   { en: 'Expected doc',    zh: '預期文件' },
  rank:          { en: 'Rank',            zh: '排名' },
  topRetrieved:  { en: 'Top retrieved',   zh: '檢索排首文件' },

  /* ---- follow-ups ---- */
  followUps:     { en: 'Follow-up questions (FR-8.1)', zh: '追問處理（FR-8.1）' },
  followUpsSub:  { en: 'Rank of the expected document for a question that cannot stand on its own',
                   zh: '無法獨立理解的追問，其預期文件的排名' },
  noFollowUps:   { en: 'No follow-ups evaluated', zh: '未有評估任何追問' },
  priorQuestion: { en: 'Preceding question', zh: '前一條問題' },
  followUp:      { en: 'Follow-up',       zh: '追問' },
  rankWithout:   { en: 'Rank without history', zh: '無對話歷史排名' },
  rankWith:      { en: 'Rank with history',    zh: '有對話歷史排名' },
  followUpNote:  { en: 'Rank-1 hits: {without}/{n} without conversation history, {with}/{n} with it. '
                     + 'Recall@K over standalone questions cannot observe conversational ability, so '
                     + 'this measures it directly.',
                   zh: '排名第一命中：無對話歷史 {without}/{n}，有對話歷史 {with}/{n}。'
                     + '以獨立問題計算的 Recall@K 無法反映對話能力，此項直接量度。' },

  /* ---- bilingual parity ---- */
  bilingual:     { en: 'Bilingual parity 中英對照 (FR-8.1)', zh: '中英雙語對等（FR-8.1）' },
  bilingualSub:  { en: 'The same question asked in English and in 繁體中文 — does it find the same document?',
                   zh: '同一條問題分別以英文及繁體中文提問，能否檢索到同一份文件？' },
  noBilingual:   { en: 'No bilingual pairs evaluated', zh: '未有評估任何雙語問題組' },
  enRank1:       { en: 'English rank-1',  zh: '英文排名第一' },
  zhRank1:       { en: 'Chinese rank-1',  zh: '中文排名第一' },
  bothLangs:     { en: 'Both languages',  zh: '兩種語言均命中' },
  enQuestion:    { en: 'English question', zh: '英文問題' },
  zhQuestion:    { en: 'Chinese question', zh: '中文問題' },
  enRank:        { en: 'EN rank',         zh: '英文排名' },
  zhRank:        { en: 'ZH rank',         zh: '中文排名' },
  bilingualNote: { en: 'A bilingual corpus can look complete in English while answering Chinese '
                     + 'questions from a thin translated footnote. Asking the same question in both '
                     + 'languages is the only way to see that; a gap between the two columns is the '
                     + 'metric to watch.',
                   zh: '雙語語料庫可能在英文一側看似完整，但中文問題卻只靠一段簡略譯文作答。'
                     + '以兩種語言提出同一條問題，是唯一能發現這情況的方法；'
                     + '兩欄之間的差距正是需要關注的指標。' },
};

export default function AiEvaluation() {
  const d = data;
  const c = d.classification || {};
  const r = d.retrieval || {};
  const f = d.followups || {};
  const fRows = f.rows || [];
  const b = d.bilingual || {};
  const bRows = b.rows || [];
  const perCategory = c.perCategory || [];
  const examples = c.examples || [];
  const rows = r.rows || [];

  return (
    <>
      <PageHead title={t(S.head)} sub={t(S.headSub)} />

      <Card title={t(S.autoCat)} icon="sparkles">
        <div className="grid grid--3">
          <Stat label={t(S.testExamples)} value={c.n ?? 0} icon="clipboard" />
          <Stat label={t(S.correct)} value={c.correct ?? 0} icon="check" />
          <Stat label={t(S.accuracy)} value={(c.accuracyPct ?? 0) + '%'} icon="chart" />
        </div>
        <div className="small muted" style={{ marginTop: 12 }}>
          {t(S.kHint, { k: c.k, n: c.trainSize })}
        </div>
      </Card>

      <Card title={t(S.perCategory)} icon="chart" flush>
        {perCategory.length === 0
          ? <EmptyState icon="chart" title={t(S.noCategories)} />
          : (
            <TableWrap>
              <thead>
                <tr><th>{t(C.category)}</th><th className="num">{t(S.testItems)}</th><th className="num">{t(S.correct)}</th><th>{t(S.accuracy)}</th></tr>
              </thead>
              <tbody>
                {perCategory.map((pc, i) => (
                  <tr key={i}>
                    <td>{enumLabel('IssueCategory', pc.category)}</td>
                    <td className="num">{pc.total}</td>
                    <td className="num">{pc.correct}</td>
                    <td><Bar pct={pc.accuracyPct} showLabel /></td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>

      <Card title={t(S.predictions)} icon="clipboard" flush>
        {examples.length === 0
          ? <EmptyState icon="clipboard" title={t(S.noPredictions)} />
          : (
            <TableWrap>
              <thead>
                <tr><th>{t(S.ref)}</th><th>{t(S.actual)}</th><th>{t(S.predicted)}</th><th>{t(S.result)}</th></tr>
              </thead>
              <tbody>
                {examples.map((ex, i) => (
                  <tr key={i}>
                    <td className="mono">{ex.refNumber}</td>
                    <td>{enumLabel('IssueCategory', ex.actual)}</td>
                    <td>{ex.predicted ? enumLabel('IssueCategory', ex.predicted) : <span className="muted">—</span>}</td>
                    <td>{ex.correct ? <Badge tone="ok">✓</Badge> : <Badge tone="danger">✗</Badge>}</td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>

      <Card title={t(S.retrieval)} icon="search">
        <div className="grid grid--4">
          <Stat label={t(S.recall1)} value={(r.recall1Pct ?? 0) + '%'} icon="search" />
          <Stat label={t(S.recall3)} value={(r.recall3Pct ?? 0) + '%'} icon="search" />
          <Stat label={t(S.recall5)} value={(r.recall5Pct ?? 0) + '%'} icon="search" />
          <Stat
            label={t(S.docsServed)}
            value={Number(r.avgServedDocs ?? 0).toFixed(2)}
            hint={t(S.docsServedHint)}
            icon="file"
          />
        </div>
        <div className="small muted" style={{ marginTop: 10 }}>
          {t(S.retrievalNote, { mrr: Number(r.mrr ?? 0).toFixed(2), n: r.n ?? 0 })}
        </div>
      </Card>

      <Card title={t(S.retrievalRows)} icon="search" flush>
        {rows.length === 0
          ? <EmptyState icon="search" title={t(S.noQuestions)} />
          : (
            <TableWrap>
              <thead>
                <tr><th>{t(S.question)}</th><th>{t(S.expectedDoc)}</th><th className="center">{t(S.rank)}</th><th>{t(S.topRetrieved)}</th></tr>
              </thead>
              <tbody>
                {rows.map((row, i) => (
                  <tr key={i}>
                    <td>{row.question}</td>
                    <td className="mono">{row.expected}</td>
                    <td className="center">{row.rank}</td>
                    <td className="mono small">{row.topDocs}</td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>

      <Card
        title={t(S.followUps)}
        sub={t(S.followUpsSub)}
        icon="sparkles"
        flush
      >
        {fRows.length === 0
          ? <EmptyState icon="sparkles" title={t(S.noFollowUps)} />
          : (
            <>
              <TableWrap>
                <thead>
                  <tr>
                    <th>{t(S.priorQuestion)}</th><th>{t(S.followUp)}</th><th>{t(S.expectedDoc)}</th>
                    <th className="center">{t(S.rankWithout)}</th><th className="center">{t(S.rankWith)}</th>
                  </tr>
                </thead>
                <tbody>
                  {fRows.map((row, i) => (
                    <tr key={i}>
                      <td className="muted small">{row.prior}</td>
                      <td>{row.followUp}</td>
                      <td className="mono">{row.expected}</td>
                      <td className="center muted">{row.rankWithout}</td>
                      <td className="center">
                        <Badge tone={row.improved ? 'ok' : 'muted'}>{row.rankWith}</Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </TableWrap>
              <div className="small muted" style={{ padding: '10px 16px' }}>
                {t(S.followUpNote, { without: f.hitsWithout ?? 0, with: f.hitsWith ?? 0, n: f.n ?? 0 })}
              </div>
            </>
          )}
      </Card>

      <Card
        title={t(S.bilingual)}
        sub={t(S.bilingualSub)}
        icon="file"
        flush
      >
        {bRows.length === 0
          ? <EmptyState icon="file" title={t(S.noBilingual)} />
          : (
            <>
              <div className="grid grid--3" style={{ padding: '14px 16px 4px' }}>
                <Stat label={t(S.enRank1)} value={`${b.hitsEn ?? 0}/${b.n ?? 0}`} icon="search" />
                <Stat label={t(S.zhRank1)} value={`${b.hitsZh ?? 0}/${b.n ?? 0}`} icon="search" />
                <Stat label={t(S.bothLangs)} value={`${b.bothTop1 ?? 0}/${b.n ?? 0}`} icon="check" />
              </div>
              <TableWrap>
                <thead>
                  <tr>
                    <th>{t(S.enQuestion)}</th><th>{t(S.zhQuestion)}</th><th>{t(S.expectedDoc)}</th>
                    <th className="center">{t(S.enRank)}</th><th className="center">{t(S.zhRank)}</th>
                  </tr>
                </thead>
                <tbody>
                  {bRows.map((row, i) => (
                    <tr key={i}>
                      <td>{row.english}</td>
                      <td lang="zh-Hant">{row.chinese}</td>
                      <td className="mono">{row.expected}</td>
                      <td className="center">{row.rankEn}</td>
                      <td className="center">
                        <Badge tone={row.bothTop1 ? 'ok' : 'muted'}>{row.rankZh}</Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </TableWrap>
              <div className="small muted" style={{ padding: '10px 16px' }}>
                {t(S.bilingualNote)}
              </div>
            </>
          )}
      </Card>
    </>
  );
}
