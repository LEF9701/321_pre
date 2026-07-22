import { useState } from 'react';
import { data, flash } from '../lib/page.js';
import { enumLabel, enumOptions } from '../lib/enums.js';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Card, Button, Field, Input, Textarea, Select, Csrf, Flash, EmptyState, TableWrap } from '../components/ui.jsx';

export const name = 'ai-documents';
export const title = { en: 'AI Documents', zh: 'AI 文件' };

export default function AiDocuments() {
  const d = data;
  const docs = d.docs || [];
  const [editing, setEditing] = useState(null); // open document, null = closed

  return (
    <>
      <PageHead
        title={t({ en: 'AI Documents', zh: 'AI 文件' })}
        sub={t({ en: 'Bylaw / FAQ corpus (FR-8.2)', zh: '屋苑守則／常見問題語料庫（FR-8.2）' })}
      />

      <Flash flash={flash} />

      <Card title={t({ en: 'Ingest a document', zh: '匯入文件' })} icon="plus">
        <form method="post" action="/ai/documents">
          <Csrf />

          <div className="form-grid">
            <Field label={t({ en: 'Code', zh: '文件編號' })} htmlFor="code">
              <Input id="code" name="code" placeholder={t({ en: 'auto-generated if blank', zh: '留空則自動產生' })} />
            </Field>
            <Field label={t(C.title)} htmlFor="title">
              <Input id="title" name="title" required />
            </Field>
          </div>

          <div className="form-grid">
            <Field label={t(C.language)} htmlFor="language">
              <Select id="language" name="language" options={enumOptions('Language')} />
            </Field>
            <Field label={t(C.category)} htmlFor="category">
              <Input id="category" name="category"
                     placeholder={t({ en: 'e.g. Fees, Noise, Fire safety', zh: '例如：管理費、噪音、消防安全' })} />
            </Field>
          </div>

          <Field label={t({ en: 'Full content', zh: '完整內容' })} htmlFor="content" full>
            <Textarea id="content" name="content" rows={6} required
                      placeholder={t({ en: 'Paste bylaw clause or FAQ answer here (English and/or 中文).',
                                       zh: '請於此貼上屋苑守則條文或常見問題答案（中文及／或英文）。' })} />
          </Field>

          <Button type="submit" variant="primary" icon="plus">
            {t({ en: 'Ingest & index', zh: '匯入並建立索引' })}
          </Button>
        </form>
      </Card>

      <Card title={t({ en: 'Indexed documents', zh: '已索引文件' })} icon="doc" flush>
        {docs.length === 0
          ? (
            <EmptyState icon="doc" title={t({ en: 'No documents yet', zh: '暫時未有文件' })}>
              {t({ en: 'Ingest a bylaw clause or FAQ above to make it queryable.',
                   zh: '請於上方匯入屋苑守則條文或常見問題，AI 助理即可引用。' })}
            </EmptyState>
          )
          : (
            <TableWrap>
              <thead>
                <tr>
                  <th>{t({ en: 'Code', zh: '文件編號' })}</th>
                  <th>{t(C.title)}</th>
                  <th>{t(C.language)}</th>
                  <th>{t(C.category)}</th>
                  <th className="num">{t({ en: 'Chars', zh: '字元數' })}</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {docs.map((doc) => (
                  <tr key={doc.id}>
                    <td className="mono">{doc.code}</td>
                    <td className="strong">{doc.title}</td>
                    <td>{enumLabel('Language', doc.language)}</td>
                    <td>{doc.category || <span className="muted">—</span>}</td>
                    <td className="num">{(doc.content || '').length}</td>
                    <td className="right nowrap">
                      <Button type="button" variant="ghost" size="sm" icon="edit"
                              onClick={() => setEditing(doc)} style={{ marginRight: 6 }}>
                        {t({ en: 'View / Edit', zh: '檢視／編輯' })}
                      </Button>
                      <form method="post" action={'/ai/documents/' + doc.id + '/delete'} style={{ display: 'inline' }}
                            onSubmit={(e) => { if (!confirm(t({ en: 'Remove this document from the index?', zh: '確定要將此文件從索引中移除？' }))) e.preventDefault(); }}>
                        <Csrf />
                        <Button type="submit" variant="danger" size="sm" icon="trash">{t(C.del)}</Button>
                      </form>
                    </td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>

      {editing && (
        <div
          onClick={() => setEditing(null)}
          style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.45)', display: 'grid', placeItems: 'center', zIndex: 100 }}
        >
          <Card
            title={t({ en: 'View / Edit document', zh: '檢視／編輯文件' })}
            icon="doc"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: 640, width: '90%', maxHeight: '85vh', overflow: 'auto' }}
          >
            <Field label={t({ en: 'Code', zh: '文件編號' })}>
              <div className="mono">{editing.code}</div>
            </Field>

            <form method="post" action={'/ai/documents/' + editing.id}>
              <Csrf />

              <Field label={t(C.title)} htmlFor="edit-title">
                <Input id="edit-title" name="title" required defaultValue={editing.title} />
              </Field>

              <Field label={t(C.language)} htmlFor="edit-language">
                <Select id="edit-language" name="language" options={enumOptions('Language')} defaultValue={editing.language} />
              </Field>

              <Field label={t(C.category)} htmlFor="edit-category">
                <Input id="edit-category" name="category" defaultValue={editing.category || ''} />
              </Field>

              <Field label={t(C.content)} htmlFor="edit-content" full>
                <Textarea id="edit-content" name="content" rows={12} required defaultValue={editing.content || ''} />
              </Field>

              <div className="btn-row" style={{ marginTop: 12 }}>
                <Button type="submit" variant="primary" icon="check">
                  {t({ en: 'Save & re-index', zh: '儲存並重建索引' })}
                </Button>
                <Button type="button" variant="ghost" onClick={() => setEditing(null)}>{t(C.cancel)}</Button>
              </div>
            </form>
          </Card>
        </div>
      )}
    </>
  );
}
