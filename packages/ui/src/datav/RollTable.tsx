/**
 * 轮播表格（纯 React · 2026-08-25 下沉 packages/ui）.
 *
 * - 数据行数 > 显示行数时：**整体无缝无限滚动**（内容复制两份，translateY -50% 循环，如滚动字幕）
 * - hover 暂停；点击行回调（数据行索引）
 * - 主题色 = 表头底色
 */
import { useMemo } from 'react';

export type RollTableTheme = 'cyan' | 'red' | 'orange' | 'blue';

const THEME_HEADER_BGC: Record<RollTableTheme, string> = {
  cyan: 'rgba(0,188,235,0.16)',
  red: 'rgba(255,77,79,0.16)',
  orange: 'rgba(250,140,22,0.16)',
  blue: 'rgba(22,119,255,0.16)',
};

export interface RollTableProps {
  columns: string[];
  rows: (string | number)[][];
  align?: ('left' | 'center' | 'right')[];
  /** 显示行数（缺省 6） */
  rowNum?: number;
  /** 主题色（缺省 cyan） */
  theme?: RollTableTheme;
  /** 每行滚动时长 ms（缺省 1200；0 = 不滚动） */
  speed?: number;
  onClick?: (ri: number) => void;
}

export default function RollTable({
  columns,
  rows,
  align,
  rowNum = 6,
  theme = 'cyan',
  speed = 1200,
  onClick,
}: RollTableProps) {
  const aligns = align ?? columns.map(() => 'left' as const);
  const rowH = 30; // 行高 px
  const headerH = 32;

  const shouldScroll = rows.length > rowNum && speed > 0;
  // 无缝滚动时长：完整循环 = 全部行滚完一轮
  const duration = rows.length * speed;
  // 滚动轨道：数据两份（无缝循环），仅滚动部分使用
  const trackRows = shouldScroll ? [...rows, ...rows] : rows;

  const rendered = useMemo(
    () =>
      trackRows.map((row, i) => {
        const realRi = shouldScroll ? i % rows.length : i;
        return (
          <div
            key={`${realRi}-${i}`}
            onClick={onClick ? () => onClick(realRi) : undefined}
            style={{
              display: 'grid',
              gridTemplateColumns: `repeat(${columns.length}, 1fr)`,
              height: rowH,
              lineHeight: `${rowH}px`,
              fontSize: 13,
              color: '#cfe6f5',
              padding: '0 8px',
              cursor: onClick ? 'pointer' : 'default',
              background: realRi % 2 === 0 ? 'rgba(255,255,255,0.02)' : 'rgba(255,255,255,0.05)',
              borderBottom: '1px solid rgba(30,91,138,0.15)',
              overflow: 'hidden',
            }}
          >
            {row.map((cell, ci) => (
              <span
                key={ci}
                style={{
                  textAlign: aligns[ci] ?? 'left',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                }}
              >
                {cell}
              </span>
            ))}
          </div>
        );
      }),
    [trackRows, columns, aligns, rows.length, shouldScroll],
  );

  return (
    <div
      style={{
        position: 'relative',
        width: '100%',
        height: '100%',
        overflow: 'hidden',
        border: '1px solid rgba(30,91,138,0.4)',
        borderRadius: 4,
        background: 'rgba(9,30,60,0.35)',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* 无缝滚动动画 */}
      <style>{`
        @keyframes rolltable-roll {
          0% { transform: translateY(0); }
          100% { transform: translateY(-50%); }
        }
        .rolltable-track {
          animation: rolltable-roll ${duration}ms linear infinite;
        }
        .rolltable-viewport:hover .rolltable-track {
          animation-play-state: paused;
        }
      `}</style>

      {/* 表头 */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: `repeat(${columns.length}, 1fr)`,
          height: headerH,
          lineHeight: `${headerH}px`,
          background: THEME_HEADER_BGC[theme],
          borderBottom: '1px solid rgba(30,91,138,0.5)',
          fontSize: 13,
          color: '#8fc1e3',
          fontWeight: 600,
          padding: '0 8px',
          flexShrink: 0,
        }}
      >
        {columns.map((c, i) => (
          <span key={c} style={{ textAlign: aligns[i] ?? 'left' }}>{c}</span>
        ))}
      </div>

      {/* 滚动数据区 */}
      <div className="rolltable-viewport" style={{ flex: 1, overflow: 'hidden', minHeight: 0 }}>
        <div
          className="rolltable-track"
          style={{
            willChange: 'transform',
            // 滚动模式：轨道高度 = 两份数据 = 200% 视口，-50% 无缝循环；非滚动：单份
            ...(shouldScroll ? {} : { transform: 'none', animation: 'none' }),
          }}
        >
          {rendered}
        </div>
      </div>
    </div>
  );
}
