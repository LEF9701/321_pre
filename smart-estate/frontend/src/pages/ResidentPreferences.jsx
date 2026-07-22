import { data, flash } from '../lib/page.js';
import { enumLabel, enumOptions } from '../lib/enums.js';
import { PageHead, Card, Flash, Alert, Button, Select, TableWrap, Csrf } from '../components/ui.jsx';
import { t, plural } from '../lib/i18n.js';
import { C } from '../lib/strings.js';

export const name = 'resident-preferences';
export const title = { en: 'Communication Preferences', zh: '通訊偏好' };

const S = {
  head:          { en: 'Communication Preferences', zh: '通訊偏好' },
  sub:           { en: 'Choose your preferred channel and language for each kind of notice. Emergency alerts always override these.',
                   zh: '為每類通告選擇您偏好的接收渠道及語言。緊急警報一律凌駕以上設定。' },
  onlyActive:    { en: 'Only {list} {verb} currently active.', zh: '目前只有 {list} 可供使用。' },
  disabledNote:  { en: '{list} {verb} disabled and cannot be selected.', zh: '{list} 已停用，無法選擇。' },
  optDisabled:   { en: '{label} (disabled)', zh: '{label}（已停用）' },
  listSep:       { en: ', ', zh: '、' },
  noticeCat:     { en: 'Notice category', zh: '通告類別' },
  savePrefs:     { en: 'Save preferences', zh: '儲存偏好設定' },
};

export default function ResidentPreferences() {
  const d = data;
  const prefs = d.prefs || [];
  const enabled = d.enabledChannels && d.enabledChannels.length ? d.enabledChannels : ['SMS', 'WHATSAPP', 'EMAIL', 'VOICE'];
  const defaultChannel = d.defaultChannel || enabled[0] || 'SMS';

  // Show every channel, but grey out the ones that are switched off.
  const channelOptions = enumOptions('Channel').map((o) => ({
    ...o,
    disabled: !enabled.includes(o.value),
    label: enabled.includes(o.value) ? o.label : t(S.optDisabled, { label: o.label }),
  }));
  const disabledLabels = enumOptions('Channel').filter((o) => !enabled.includes(o.value)).map((o) => o.label);
  const enabledLabels = enabled.map((c) => enumLabel('Channel', c));

  // Split on the {list} slot so the channel names keep their emphasis without
  // gluing translated fragments together; the sentence order stays translatable.
  const sep = t(S.listSep);
  const activeParts = t(S.onlyActive, { verb: plural(enabledLabels.length, { one: 'is', other: 'are', zh: '' }) }).split('{list}');
  const offParts = t(S.disabledNote, { verb: plural(disabledLabels.length, { one: 'is', other: 'are', zh: '' }) }).split('{list}');

  return (
    <>
      <PageHead title={t(S.head)} sub={t(S.sub)} />

      <Flash flash={flash} />

      {disabledLabels.length > 0 && (
        <Alert tone="info" icon="bell">
          {activeParts[0]}<strong>{enabledLabels.join(sep)}</strong>{activeParts[1]}
          {' '}{offParts[0]}{disabledLabels.join(sep)}{offParts[1]}
        </Alert>
      )}

      <Card flush>
        <form method="post" action="/resident/preferences">
          <Csrf />
          <TableWrap>
            <thead>
              <tr>
                <th>{t(S.noticeCat)}</th>
                <th>{t(C.channel)}</th>
                <th>{t(C.language)}</th>
              </tr>
            </thead>
            <tbody>
              {prefs.map((p) => (
                <tr key={p.id}>
                  <td className="strong">{enumLabel('NoticeCategory', p.category)}</td>
                  <td>
                    <Select name={`channel_${p.id}`} options={channelOptions}
                            defaultValue={enabled.includes(p.channel) ? p.channel : defaultChannel} />
                  </td>
                  <td>
                    <Select name={`language_${p.id}`} options={enumOptions('Language')} defaultValue={p.language} />
                  </td>
                </tr>
              ))}
            </tbody>
          </TableWrap>
          <div style={{ padding: 16 }}>
            <Button type="submit" variant="primary" icon="check">{t(S.savePrefs)}</Button>
          </div>
        </form>
      </Card>
    </>
  );
}
