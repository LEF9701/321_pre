// Central enum label + tone maps. Templates inject only the enum NAME (e.g.
// "OVERDUE"); the UI derives the human label and the status colour here. This
// mirrors the server enum labels exactly (see model/*.java), with the Chinese
// side matching the labelZh() the server uses for outbound messages.

import { t } from './i18n.js';

export const ENUMS = {
  Role: {
    RESIDENT:   { label: { en: 'Resident',   zh: '住戶' } },
    MANAGEMENT: { label: { en: 'Management', zh: '管理處' } },
    ADMIN:      { label: { en: 'Admin',      zh: '系統管理員' } },
  },
  // A language names itself in its own language, in both UIs.
  Language: {
    EN: { label: { en: 'English', zh: 'English' } },
    ZH: { label: { en: '中文',    zh: '中文' } },
  },
  FeeStatus: {
    PAID:    { label: { en: 'Paid',    zh: '已繳費' },   tone: 'ok' },
    DUE:     { label: { en: 'Due',     zh: '待繳費' },   tone: 'warn' },
    OVERDUE: { label: { en: 'Overdue', zh: '逾期未繳' }, tone: 'danger' },
  },
  UnitStatus: {
    VACANT:   { label: { en: 'Vacant',   zh: '空置' },   tone: 'muted' },
    OCCUPIED: { label: { en: 'Occupied', zh: '已入住' }, tone: 'ok' },
  },
  Channel: {
    SMS:      { label: { en: 'SMS',        zh: '短訊' } },
    WHATSAPP: { label: { en: 'WhatsApp',   zh: 'WhatsApp' } },
    EMAIL:    { label: { en: 'Email',      zh: '電郵' } },
    VOICE:    { label: { en: 'Voice Call', zh: '語音來電' } },
    PUSH:     { label: { en: 'Push',       zh: '推送通知' } },
  },
  Severity: {
    INFO:     { label: { en: 'Info',     zh: '一般' }, tone: 'info' },
    WARNING:  { label: { en: 'Warning',  zh: '警示' }, tone: 'warn' },
    CRITICAL: { label: { en: 'Critical', zh: '緊急' }, tone: 'danger' },
  },
  NoticeCategory: {
    EMERGENCY:            { label: { en: 'Emergency',            zh: '緊急事故' },          tone: 'danger' },
    ANNOUNCEMENT:         { label: { en: 'Announcement',         zh: '公告' },              tone: 'brand' },
    MEETING:              { label: { en: 'Meeting / AGM Notice', zh: '會議／業主大會通知' }, tone: 'info' },
    FEE_REMINDER:         { label: { en: 'Fee Reminder',         zh: '費用提醒' },          tone: 'warn' },
    SERVICE_INTERRUPTION: { label: { en: 'Service Interruption', zh: '服務中斷' },          tone: 'warn' },
    MAINTENANCE:          { label: { en: 'Maintenance',          zh: '維修保養' },          tone: 'info' },
    OBSERVATORY:          { label: { en: 'HK Observatory Alert', zh: '天文台警告' },        tone: 'warn' },
    GENERAL:              { label: { en: 'General',              zh: '一般事項' },          tone: 'muted' },
  },
  DeliveryStatus: {
    PENDING:   { label: { en: 'Pending',   zh: '待發送' },  tone: 'muted' },
    SENT:      { label: { en: 'Sent',      zh: '已發送' },  tone: 'info' },
    DELIVERED: { label: { en: 'Delivered', zh: '已送達' },  tone: 'brand' },
    READ:      { label: { en: 'Read',      zh: '已閱讀' },  tone: 'ok' },
    FAILED:    { label: { en: 'Failed',    zh: '發送失敗' }, tone: 'danger' },
  },
  IssueStatus: {
    SUBMITTED:    { label: { en: 'Submitted',    zh: '已提交' }, tone: 'info' },
    UNDER_REVIEW: { label: { en: 'Under Review', zh: '審核中' }, tone: 'brand' },
    IN_PROGRESS:  { label: { en: 'In Progress',  zh: '處理中' }, tone: 'warn' },
    RESOLVED:     { label: { en: 'Resolved',     zh: '已解決' }, tone: 'ok' },
    WITHDRAWN:    { label: { en: 'Withdrawn',    zh: '已撤回' }, tone: 'muted' },
  },
  IssueCategory: {
    PLUMBING:    { label: { en: 'Plumbing',          zh: '水管渠務' } },
    ELECTRICAL:  { label: { en: 'Electrical',        zh: '電力' } },
    AC_HEATING:  { label: { en: 'Air-con / Heating', zh: '冷氣／暖氣' } },
    SECURITY:    { label: { en: 'Security',          zh: '保安' } },
    COMMON_AREA: { label: { en: 'Common Area',       zh: '公共地方' } },
    NOISE:       { label: { en: 'Noise',             zh: '噪音' } },
    OTHER:       { label: { en: 'Other',             zh: '其他' } },
  },
  IssuePriority: {
    LOW:    { label: { en: 'Low',    zh: '低' }, tone: 'muted' },
    MEDIUM: { label: { en: 'Medium', zh: '中' }, tone: 'info' },
    HIGH:   { label: { en: 'High',   zh: '高' }, tone: 'danger' },
  },
  EmergencyType: {
    FIRE:       { label: { en: 'Fire',       zh: '火警' } },
    TYPHOON:    { label: { en: 'Typhoon',    zh: '颱風' } },
    FLOOD:      { label: { en: 'Flood',      zh: '水浸' } },
    GAS_LEAK:   { label: { en: 'Gas Leak',   zh: '氣體洩漏' } },
    STRUCTURAL: { label: { en: 'Structural', zh: '結構問題' } },
    OTHER:      { label: { en: 'Other',      zh: '其他' } },
  },
  SensorType: {
    SMOKE:      { label: { en: 'Smoke',      zh: '煙霧' } },
    HEAT:       { label: { en: 'Heat',       zh: '高溫' } },
    WATER_LEAK: { label: { en: 'Water leak', zh: '漏水' } },
    FIRE_DOOR:  { label: { en: 'Fire door',  zh: '防火門' } },
    DOOR:       { label: { en: 'Door',       zh: '一般門' } },
  },
  SensorStatus: {
    NORMAL:  { label: { en: 'Normal',  zh: '正常' }, tone: 'ok' },
    ALERT:   { label: { en: 'Alert',   zh: '警報' }, tone: 'danger' },
    OFFLINE: { label: { en: 'Offline', zh: '離線' }, tone: 'muted' },
  },
  AccessPointType: {
    BUILDING_ENTRANCE: { label: { en: 'Building entrance', zh: '大廈入口' } },
    ELEVATOR:          { label: { en: 'Elevator',          zh: '升降機' } },
  },
  AccessDecision: {
    GRANTED: { label: { en: 'Granted', zh: '允許' }, tone: 'ok' },
    DENIED:  { label: { en: 'Denied',  zh: '拒絕' }, tone: 'danger' },
  },
  AccessDirection: {
    IN:  { label: { en: 'Entry', zh: '進入' } },
    OUT: { label: { en: 'Exit',  zh: '離開' } },
  },
  CardStatus: {
    ACTIVE:    { label: { en: 'Active',    zh: '生效' },   tone: 'ok' },
    SUSPENDED: { label: { en: 'Suspended', zh: '已停用' }, tone: 'muted' },
  },
  CardRequestAction: {
    ADD:     { label: { en: 'Register a new card', zh: '登記新卡' } },
    REPLACE: { label: { en: 'Replace card',        zh: '更換卡片' } },
    REMOVE:  { label: { en: 'Remove card',         zh: '移除卡片' } },
  },
  CardRequestStatus: {
    PENDING:  { label: { en: 'Pending approval', zh: '待審批' }, tone: 'warn' },
    APPROVED: { label: { en: 'Approved',         zh: '已批准' }, tone: 'ok' },
    REJECTED: { label: { en: 'Rejected',         zh: '已拒絕' }, tone: 'danger' },
  },
};

// Fixed display order for the issue status stepper.
export const ISSUE_STATUS_ORDER = ['SUBMITTED', 'UNDER_REVIEW', 'IN_PROGRESS', 'RESOLVED'];

export function enumLabel(type, name) {
  if (name == null) return '';
  const label = ENUMS[type]?.[name]?.label;
  return label ? t(label) : String(name);
}
export function enumTone(type, name) {
  return ENUMS[type]?.[name]?.tone ?? 'muted';
}
// [{ value, label }] for building <select> options from an enum.
export function enumOptions(type) {
  return Object.entries(ENUMS[type] || {}).map(([value]) => ({ value, label: enumLabel(type, value) }));
}
