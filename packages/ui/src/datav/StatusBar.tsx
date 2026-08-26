/**
 * 底部状态栏（精简版 · COCKPIT_GUIDE §4.3 文字清零）：仅在线率，无任何说明性文字 ·
 * 2026-08-25 下沉 packages/ui.
 */
import { BORDER, PANEL_BG } from './theme';

export interface StatusBarProps {
  total?: number;
  online?: number;
}

export default function StatusBar({ total, online }: StatusBarProps) {
  const rate = total ? Math.round(((online ?? 0) / total) * 100) : 0;
  return (
    <div
      style={{
        marginTop: 8,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'flex-end',
        padding: '5px 14px',
        border: BORDER,
        borderRadius: 6,
        background: PANEL_BG,
        fontSize: 16,
        color: '#8fc1e3',
      }}
    >
      在线率 <b style={{ color: '#52c41a', fontSize: 22, marginLeft: 4 }}>{rate}%</b>
      <span style={{ color: '#5a7f9f', marginLeft: 6 }}>
        {online ?? 0}/{total ?? 0}
      </span>
    </div>
  );
}
