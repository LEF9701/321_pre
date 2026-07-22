import { data } from '../lib/page.js';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';
import { PageHead, Stat, Card, Icon } from '../components/ui.jsx';

export const name = 'admin-home';
export const title = { en: 'Admin Home', zh: '管理主頁' };

const S = {
  users:  { en: 'Users',  zh: '使用者' },
  manage: { en: 'Manage', zh: '管理項目' },
};

const NAV = [
  { href: '/admin/buildings', icon: 'building',
    title: { en: 'Buildings & units', zh: '大廈及單位' },
    sub: { en: 'Register and configure the estate', zh: '登記及設定屋苑資料' } },
  { href: '/admin/floormap', icon: 'map',
    title: { en: 'Floor Map', zh: '樓層平面圖' },
    sub: { en: 'Colour-coded occupancy view', zh: '以顏色顯示入住狀況' } },
  { href: '/admin/users', icon: 'users',
    title: { en: 'Users & Roles', zh: '使用者及角色' },
    sub: { en: 'Manage all accounts', zh: '管理所有帳戶' } },
  { href: '/admin/emergency', icon: 'alert',
    title: { en: 'Emergency', zh: '緊急事故' },
    sub: { en: 'Declare, broadcast, escalate, close', zh: '發佈、廣播、升級、結束' } },
  { href: '/admin/audit', icon: 'history',
    title: { en: 'Audit Log', zh: '稽核紀錄' },
    sub: { en: 'Sensitive actions & compliance', zh: '敏感操作及合規紀錄' } },
];

export default function AdminHome() {
  const d = data;
  return (
    <>
      <PageHead title={t({ en: 'Admin Home', zh: '管理主頁' })} />

      <div className="grid grid--4" style={{ marginBottom: 16 }}>
        <Stat label={t(C.buildings)} value={d.buildingCount ?? 0} icon="building" />
        <Stat label={t(C.blocks)} value={d.blockCount ?? 0} icon="building" />
        <Stat label={t(C.units)} value={d.unitCount ?? 0} icon="home" />
        <Stat label={t(S.users)} value={d.userCount ?? 0} icon="users" />
      </div>

      <Card title={t(S.manage)} icon="sliders">
        <div className="grid grid--auto">
          {NAV.map((n) => (
            <a key={n.href} href={n.href} className="card"
               style={{ display: 'flex', gap: 12, alignItems: 'center', padding: 16, textDecoration: 'none' }}>
              <div className="stat__icon"><Icon name={n.icon} size={20} /></div>
              <div>
                <div className="strong">{t(n.title)}</div>
                <div className="sub">{t(n.sub)}</div>
              </div>
              <div className="spacer" />
              <Icon name="chevronRight" size={16} />
            </a>
          ))}
        </div>
      </Card>
    </>
  );
}
