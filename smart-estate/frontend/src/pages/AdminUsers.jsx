import { data, flash } from '../lib/page.js';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Card, Badge, EnumBadge, Button, EmptyState, TableWrap, Csrf, Flash } from '../components/ui.jsx';

export const name = 'admin-users';
export const title = { en: 'Users & Roles', zh: '使用者及角色' };

const S = {
  head:       { en: 'Users & Roles', zh: '使用者及角色' },
  sub:        { en: 'Manage accounts and role assignments.', zh: '管理帳戶及角色分配。' },
  addUser:    { en: 'Add user',      zh: '新增使用者' },
  empty:      { en: 'No users yet',  zh: '暫未有使用者' },
  disable:    { en: 'Disable',       zh: '停用' },
  enable:     { en: 'Enable',        zh: '啟用' },
  confirmDel: { en: 'Delete this user?', zh: '刪除此使用者？' },
};

export default function AdminUsers() {
  const users = data.users || [];

  return (
    <>
      <PageHead
        title={t(S.head)}
        sub={t(S.sub)}
        actions={<Button href="/admin/users/new" variant="primary" icon="plus">{t(S.addUser)}</Button>}
      />

      <Flash flash={flash} />

      <Card flush>
        {users.length === 0 ? (
          <EmptyState icon="users" title={t(S.empty)} />
        ) : (
          <TableWrap>
            <thead>
              <tr><th>{t(C.name)}</th><th>{t(C.email)}</th><th>{t(C.role)}</th><th>{t(C.unit)}</th><th>{t(C.enabled)}</th><th /></tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td className="strong">{u.name}</td>
                  <td className="mono small">{u.email}</td>
                  <td><EnumBadge type="Role" value={u.role} /></td>
                  <td>{u.unitFullCode || <span className="muted">—</span>}</td>
                  <td>{u.enabled ? <Badge tone="ok">{t(C.yes)}</Badge> : <Badge tone="muted">{t(C.no)}</Badge>}</td>
                  <td className="nowrap">
                    <Button href={'/admin/users/' + u.id + '/edit'} size="sm" variant="ghost" icon="edit">{t(C.edit)}</Button>
                    {' '}
                    <form method="post" action={'/admin/users/' + u.id + '/toggle'} style={{ display: 'inline' }}>
                      <Csrf />
                      <Button type="submit" size="sm">{u.enabled ? t(S.disable) : t(S.enable)}</Button>
                    </form>
                    {' '}
                    <form
                      method="post"
                      action={'/admin/users/' + u.id + '/delete'}
                      style={{ display: 'inline' }}
                      onSubmit={(e) => { if (!confirm(t(S.confirmDel))) e.preventDefault(); }}
                    >
                      <Csrf />
                      <Button type="submit" size="sm" variant="danger" icon="trash" />
                    </form>
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
