// Localised text for the flash messages the server sends after a redirect.
//
// The server (com.smartestate.web.Flash) sends a code, positional arguments and
// the English sentence. Codes listed here render in the reader's language;
// anything not listed falls back to the English the server already sent, so a
// new message on the Java side is never a blank alert.
//
// `enums` names the enum type for an argument that arrives as an enum NAME
// (e.g. "CRITICAL"), so it is shown with a localised label rather than raw.

import { t } from './i18n.js';
import { enumLabel } from './enums.js';

export const FLASH = {
  /* ---- resident: preferences & issues ---- */
  'prefs.saved': {
    en: 'Your communication preferences have been saved.',
    zh: '已儲存你的通訊偏好設定。',
  },
  'issue.submitted': {
    en: 'Issue submitted. Your tracking number is {0}.',
    zh: '問題已提交，你的追蹤編號為 {0}。',
  },
  'issue.commentAdded': {
    en: 'Your comment was added.',
    zh: '已新增你的留言。',
  },
  'issue.withdrawn': {
    en: 'Issue {0} was withdrawn.',
    zh: '問題 {0} 已撤回。',
  },
  'issue.withdrawTooLate': {
    en: 'This issue is already being handled and can no longer be withdrawn.',
    zh: '此問題已在處理中，無法再撤回。',
  },

  /* ---- admin: buildings, blocks, floors, units ---- */
  'building.registered': {
    en: 'Building “{0}” registered.',
    zh: '已登記大廈「{0}」。',
  },
  'building.deleted': { en: 'Building deleted.', zh: '已刪除大廈。' },
  'block.added': {
    en: 'Block “{0}” added with {1} units.',
    zh: '已新增座別「{0}」，共 {1} 個單位。',
  },
  'block.deleted': { en: 'Block deleted.', zh: '已刪除座別。' },
  'floor.added':   { en: 'Floor {0} added.', zh: '已新增 {0} 樓。' },
  'floor.deleted': { en: 'Floor deleted.',   zh: '已刪除樓層。' },
  'unit.added':    { en: 'Unit {0} added.',  zh: '已新增單位 {0}。' },
  'unit.updated':  { en: 'Unit updated.',    zh: '已更新單位資料。' },
  'unit.deleted':  { en: 'Unit deleted.',    zh: '已刪除單位。' },

  /* ---- admin: users ---- */
  'user.emailInUse': { en: 'Email already in use.', zh: '此電郵地址已被使用。' },
  'user.created':    { en: 'User {0} created.',     zh: '已建立使用者 {0}。' },
  'user.updated':    { en: 'User updated.',         zh: '已更新使用者資料。' },
  'user.enabled':    { en: 'Account enabled.',      zh: '已啟用帳戶。' },
  'user.disabled':   { en: 'Account disabled.',     zh: '已停用帳戶。' },
  'user.deleted':    { en: 'User deleted.',         zh: '已刪除使用者。' },

  /* ---- emergency ---- */
  'emergency.declared': {
    en: 'Emergency declared: {0}. The system is now in elevated state — broadcast to all residents below.',
    zh: '已發佈緊急事故：{0}。系統已進入戒備狀態 — 請於下方向全體住戶廣播。',
    enums: { 0: 'EmergencyType' },
  },
  'emergency.noneActive': {
    en: 'No active emergency. Declare an emergency before broadcasting.',
    zh: '目前沒有生效中的緊急事故，請先發佈事故再進行廣播。',
  },
  'emergency.broadcastSent': {
    en: 'Forced broadcast sent to {0} residents across SMS, WhatsApp and Email ({1}/{2} delivered). Run voice escalation for anyone not yet reached.',
    zh: '已透過短訊、WhatsApp 及電郵向 {0} 位住戶強制廣播（{1}/{2} 已送達）。請對尚未接收的住戶執行語音升級通知。',
  },
  'emergency.broadcastNotFound': {
    en: 'Broadcast not found.',
    zh: '找不到該廣播紀錄。',
  },
  'emergency.escalationNoneNeeded': {
    en: 'Voice escalation complete: every resident had already been reached on another channel.',
    zh: '語音升級通知完成：所有住戶已透過其他渠道成功接收。',
  },
  'emergency.escalationPlaced': {
    en: 'Voice escalation placed {0} automated call(s) to residents not reached on any other channel.',
    zh: '語音升級通知已向未能透過其他渠道接收的住戶撥出 {0} 個自動語音電話。',
  },
  'emergency.closed': {
    en: 'Emergency closed. Routing has returned to normal (per-resident preferences).',
    zh: '緊急事故已解除，通知派送已回復正常（按住戶個人偏好）。',
  },

  /* ---- AI documents ---- */
  'aiDoc.ingested': {
    en: 'Document “{0}” ingested and indexed. The chatbot can now use it.',
    zh: '文件「{0}」已匯入並建立索引，聊天助理現可使用。',
  },
  'aiDoc.updated': {
    en: 'Document updated and re-indexed. The chatbot now uses the new text.',
    zh: '文件已更新並重新索引，聊天助理現使用新內容。',
  },
  'aiDoc.removed': {
    en: 'Document removed and index rebuilt.',
    zh: '已移除文件並重建索引。',
  },

  /* ---- management: residents ---- */
  'resident.emailExists': {
    en: 'A user with email {0} already exists.',
    zh: '已有使用者使用電郵地址 {0}。',
  },
  'resident.created': {
    en: 'Resident {0} created (default password: resident123).',
    zh: '已建立住戶 {0}（預設密碼：resident123）。',
  },
  'resident.updated': { en: 'Resident {0} updated.', zh: '已更新住戶 {0} 的資料。' },
  'resident.removed': { en: 'Resident removed.',     zh: '已移除住戶。' },

  /* ---- management: notifications ---- */
  'notice.scheduled':  { en: 'Notification scheduled for {0}.',        zh: '通知已排定於 {0} 發送。' },
  'notice.dispatched': { en: 'Notification dispatched to {0} resident(s).', zh: '通知已發送給 {0} 位住戶。' },
  'notice.updatedDispatched': {
    en: 'Notice updated. (It was already dispatched, so recipients keep the version they received.)',
    zh: '通知已更新。（此通知已發送，收件者仍保留他們收到的版本。）',
  },
  'notice.updatedScheduled': {
    en: 'Notice updated. The scheduled send will use the new content.',
    zh: '通知已更新，排定發送將採用新內容。',
  },

  /* ---- management: issues ---- */
  'issue.replyPosted': {
    en: 'Reply posted. The resident has been notified.',
    zh: '已發佈回覆，並已通知住戶。',
  },
  'issue.statusUpdated': {
    en: 'Status updated to {0}. Resident has been notified.',
    zh: '狀態已更新為{0}，並已通知住戶。',
    enums: { 0: 'IssueStatus' },
  },
  'issue.prioritySet': {
    en: 'Priority set to {0}.',
    zh: '優先次序已設定為{0}。',
    enums: { 0: 'IssuePriority' },
  },
  'issue.assigned': { en: 'Issue assigned to {0}.', zh: '已將問題指派給 {0}。' },

  /* ---- management: fees ---- */
  'fees.remindersDispatched': {
    en: 'Fee reminders dispatched: {0} due, {1} overdue (escalated).',
    zh: '已發送費用提醒：{0} 個待繳，{1} 個逾期（已升級處理）。',
  },
  'fees.updatedForUnit': { en: 'Fee updated for {0}.', zh: '已更新單位 {0} 的費用資料。' },
  'fees.noResidentForUnit': {
    en: 'No resident is assigned to this unit, so no reminder could be sent.',
    zh: '此單位未有住戶，因此無法發送提醒。',
  },
  'fees.reminderSent': {
    en: 'Reminder sent to {0} for {1}.',
    zh: '已就單位 {1} 向 {0} 發送提醒。',
  },
  'fees.reminderSentFinal': {
    en: 'Final (overdue) notice sent to {0} for {1}.',
    zh: '已就單位 {1} 向 {0} 發送最終逾期通知。',
  },

  /* ---- IoT ---- */
  'iot.emergencyDeclared': {
    en: 'Emergency declared for {0}. The system is now in elevated state.',
    zh: '已就 {0} 發佈緊急事故，系統已進入戒備狀態。',
  },

  /* ---- Access cards ---- */
  'access.requestSubmitted': {
    en: 'Your request was submitted and is awaiting management approval.',
    zh: '你的申請已提交，正等待管理處審批。',
  },
  'access.needCard': { en: 'Please enter a card number.', zh: '請輸入卡號。' },
  'access.approved': { en: 'Card request approved.',      zh: '已批准卡片申請。' },
  'access.rejected': { en: 'Card request rejected.',      zh: '已拒絕卡片申請。' },
};

/**
 * Render one flash message. Falls back to the English sentence the server sent
 * whenever the code is unknown (or absent, for messages not yet converted).
 */
export function flashText(code, args, fallback) {
  const entry = code && FLASH[code];
  if (!entry) return fallback || '';

  const list = Array.isArray(args) ? args : [];
  const vars = {};
  list.forEach((value, i) => {
    const enumType = entry.enums?.[i];
    vars[i] = enumType ? enumLabel(enumType, value) : value;
  });
  return t(entry, vars);
}
