/**
 * 横向滚动字幕横幅（新闻 ticker · 2026-08-25 下沉 packages/ui）.
 *
 * 底部告警/系统信息横向无缝滚动；hover 暂停。
 * 内容 = items 用分隔符拼接，复制两份实现无缝循环。
 */
import { useMemo } from 'react';
import { TXT } from './theme';

interface TickerProps {
  /** 滚动条目（每条一句，如 "严重 · GJXA终端05 · 温度 82℃"） */
  items: string[];
  /** 条目间隔符号（缺省 ●） */
  sep?: string;
  /** 滚动速度 px/s（缺省 60） */
  speed?: number;
  /** 前缀图标/文字（如 ⚠） */
  prefix?: string;
  /** 无条目时显示 */
  emptyText?: string;
}

export default function Ticker({ items, sep = '●', speed = 14, prefix, emptyText = '系统运行正常' }: TickerProps) {
  const text = useMemo(() => (items.length > 0 ? items.join(`   ${sep}   `) : emptyText), [items, sep, emptyText]);
  const hasItems = items.length > 0;

  // 滚动时长 = 文本长度 / 速度
  const duration = Math.max(text.length / speed, 16);

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 10,
        border: '1px solid rgba(30,91,138,0.6)',
        borderRadius: 6,
        background: 'rgba(9,30,60,0.55)',
        padding: '6px 12px',
        overflow: 'hidden',
      }}
    >
      {/* 前缀标签 */}
      <div
        style={{
          flexShrink: 0,
          display: 'flex',
          alignItems: 'center',
          gap: 6,
          fontSize: 13,
          color: '#00e5ff',
          background: 'rgba(0,188,235,0.12)',
          border: '1px solid rgba(0,188,235,0.3)',
          borderRadius: 4,
          padding: '2px 10px',
          letterSpacing: 2,
        }}
      >
        {prefix ?? (hasItems ? '实时告警' : '系统状态')}
      </div>
      {/* 滚动内容 */}
      <div style={{ flex: 1, overflow: 'hidden', minWidth: 0 }}>
        <style>{`
          @keyframes ticker-roll {
            0% { transform: translateX(0); }
            100% { transform: translateX(-50%); }
          }
          .ticker-track {
            display: flex;
            white-space: nowrap;
            width: max-content;
            animation: ticker-roll ${duration}s linear infinite;
          }
          .ticker-viewport:hover .ticker-track {
            animation-play-state: paused;
          }
        `}</style>
        <div className="ticker-viewport">
          <div className="ticker-track" style={{ fontSize: 13, color: TXT.secondary }}>
            {/* 两份内容无缝循环 */}
            <span style={{ paddingRight: 20 }}>{text}</span>
            <span>{text}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
