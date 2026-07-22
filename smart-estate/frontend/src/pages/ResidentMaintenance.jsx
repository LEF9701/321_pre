import { data } from '../lib/page.js';
import { PageHead, Card, EmptyState, TableWrap } from '../components/ui.jsx';
import { t } from '../lib/i18n.js';
import { C } from '../lib/strings.js';

export const name = 'resident-maintenance';
export const title = { en: 'Maintenance Schedule', zh: '維修保養時間表' };

const S = {
  head:         { en: 'Maintenance Schedule', zh: '維修保養時間表' },
  sub:          { en: 'Scheduled work that may affect your unit or the estate.',
                  zh: '可能影響您的單位或屋苑的已安排工程。' },
  nothingSched: { en: 'Nothing scheduled', zh: '暫無安排' },
  work:         { en: 'Work', zh: '工程' },
  details:      { en: 'Details', zh: '詳情' },
  affected:     { en: 'Affected scope', zh: '影響範圍' },
};

export default function ResidentMaintenance() {
  const d = data;
  const schedules = d.schedules || [];

  return (
    <>
      <PageHead title={t(S.head)} sub={t(S.sub)} />

      <Card flush>
        {schedules.length === 0
          ? <EmptyState icon="calendar" title={t(S.nothingSched)} />
          : (
            <TableWrap>
              <thead>
                <tr>
                  <th>{t(C.date)}</th>
                  <th>{t(S.work)}</th>
                  <th>{t(S.details)}</th>
                  <th>{t(S.affected)}</th>
                </tr>
              </thead>
              <tbody>
                {schedules.map((s, i) => (
                  <tr key={i}>
                    <td className="nowrap">{s.date}</td>
                    <td className="strong">{s.title}</td>
                    <td className="muted">{s.description}</td>
                    <td>{s.affectedScope}</td>
                  </tr>
                ))}
              </tbody>
            </TableWrap>
          )}
      </Card>
    </>
  );
}
