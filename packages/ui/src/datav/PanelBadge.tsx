/**
 * 驾驶舱面板角标（右上角统一视觉 · 2026-08-25 下沉 packages/ui）.
 *
 * 替换 antd Tag（亮色主题在深色大屏上突兀、样式不统一）——深色半透明胶囊：
 * 语义色统一（green 正常 / orange 预警 / red 告警 / cyan 信息 / neutral 中性），
 * 可选前置状态圆点（red 带脉冲动画，值班告警一眼可见）。
 */
import { useMemo } from 'react';

export type PanelBadgeTone = 'neutral' | 'green' | 'orange' | 'red' | 'cyan';

interface PanelBadgeStyle {
  color: string;
  border: string;
  dot: string;
}

const TONE_STYLE: Record<PanelBadgeTone, PanelBadgeStyle> = {
  neutral: { color: '#8fc1e3', border: 'rgba(143,193,227,0.35)', dot: '#8fc1e3' },
  green: { color: '#52c41a', border: 'rgba(82,196,26,0.45)', dot: '#52c41a' },
  orange: { color: '#fa8c16', border: 'rgba(250,140,22,0.45)', dot: '#fa8c16' },
  red: { color: '#ff4d4f', border: 'rgba(255,77,79,0.5)', dot: '#ff4d4f' },
  cyan: { color: '#00e5ff', border: 'rgba(0,229,255,0.4)', dot: '#00e5ff' },
};

interface PanelBadgeProps {
  children: React.ReactNode;
  /** 语义色（缺省 neutral） */
  tone?: PanelBadgeTone;
  /** 前置状态圆点（red 带脉冲动画） */
  dot?: boolean;
  /** 悬浮提示 */
  title?: string;
}

export default function PanelBadge({ children, tone = 'neutral', dot, title }: PanelBadgeProps) {
  const s = TONE_STYLE[tone];
  const pulseKeyframes = useMemo(
    () =>
      `@keyframes cockpit-badge-pulse { 0%,100% { opacity: 1; box-shadow: 0 0 0 0 rgba(255,77,79,0.5); } 50% { opacity: .6; box-shadow: 0 0 6px 2px rgba(255,77,79,0.35); } }`,
    [],
  );
  return (
    <>
      {tone === 'red' && dot && <style>{pulseKeyframes}</style>}
      <span
        title={title}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 5,
          padding: '1px 9px',
          fontSize: 11,
          lineHeight: '18px',
          color: s.color,
          border: `1px solid ${s.border}`,
          borderRadius: 10,
          background: 'rgba(9,30,60,0.55)',
          whiteSpace: 'nowrap',
          fontVariantNumeric: 'tabular-nums',
        }}
      >
        {dot && (
          <i
            style={{
              width: 6,
              height: 6,
              borderRadius: '50%',
              background: s.dot,
              display: 'inline-block',
              animation: tone === 'red' ? 'cockpit-badge-pulse 1.6s ease-in-out infinite' : undefined,
            }}
          />
        )}
        {children}
      </span>
    </>
  );
}
