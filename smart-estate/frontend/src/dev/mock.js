// Dev-only mock payloads so the design system can be previewed without the
// Spring backend. Pick a page with ?page=<name>. Not bundled into production
// (only index.html loads it, and Thymeleaf injects the real window.__PAGE__).

const CSRF = { parameterName: '_csrf', token: 'dev', headerName: 'X-CSRF-TOKEN' };
const adminUser = { name: 'Alex Chan', role: 'ADMIN' };
const residentUser = { name: 'Mei Wong', role: 'RESIDENT' };
const mgmtUser = { name: 'Sam Lee', role: 'MANAGEMENT' };

const FIX = {
  login: {
    name: 'login', title: 'Sign in', user: null, csrf: CSRF,
    data: { error: false, logout: false },
  },
  'resident-dashboard': {
    name: 'resident-dashboard', title: 'Dashboard', user: residentUser, csrf: CSRF,
    activeEmergency: null,
    data: {
      unitFullCode: 'Azalea Tower 5/F-03', unitFeeStatus: 'OVERDUE',
      unreadCount: 2, openIssueCount: 1,
      inbox: [
        { id: 1, title: 'Annual General Meeting — 2026 AGM Notice', category: 'MEETING', dispatchedAt: '06-21 09:30', read: false, status: 'DELIVERED' },
        { id: 2, title: 'Q2 management fee reminder', category: 'FEE_REMINDER', dispatchedAt: '06-18 14:05', read: false, status: 'READ' },
        { id: 3, title: 'Water tank cleaning notice', category: 'MAINTENANCE', dispatchedAt: '06-10 11:00', read: true, status: 'READ' },
      ],
      issues: [
        { id: 11, refNumber: 'RPT-0007', category: 'PLUMBING', status: 'IN_PROGRESS', priority: 'HIGH' },
        { id: 12, refNumber: 'RPT-0003', category: 'NOISE', status: 'RESOLVED', priority: 'LOW' },
      ],
      schedules: [
        { date: '2026-07-04', title: 'Lift annual inspection', affectedScope: 'Azalea Tower' },
        { date: '2026-07-12', title: 'Water tank cleaning', affectedScope: 'All blocks' },
      ],
    },
  },
};

const params = new URLSearchParams(location.search);
const pick = params.get('page') || 'resident-dashboard';
window.__PAGE__ = FIX[pick] || FIX['resident-dashboard'];
