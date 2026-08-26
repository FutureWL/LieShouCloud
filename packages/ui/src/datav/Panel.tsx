/**
 * 大屏面板容器（纯 CSS 发光边框 · 2026-08-25 替代 DataV BorderBox1 · 下沉 packages/ui）.
 *
 * - 四角发光角标（CSS ::before/::after + 两个角元素）
 * - 渐变发光边框 + 半透明深色底
 * - 标题栏：发光竖条 + 标题 + extra（PanelBadge/计数）+ 放大图标
 * - 内容区 overflow:hidden，flex column（子内容可自适应高度）
 */
import { useRef } from 'react';
import { motion } from 'framer-motion';
import { Button, Space } from 'antd';
import { ZoomInOutlined } from '@ant-design/icons';
import { FONT } from './theme';

/** 放大查看上报信息（外层全屏层据此等比缩放内容） */
export interface ZoomInfo {
  key: string;
  title: string;
  w: number;
  h: number;
  contentW: number;
  contentH: number;
  content: React.ReactNode;
}

interface PanelProps {
  title: string;
  children: React.ReactNode;
  extra?: React.ReactNode;
  zoomKey?: string;
  onZoom?: (info: ZoomInfo) => void;
}

export default function Panel({ title, children, extra, zoomKey, onZoom }: PanelProps) {
  const boxRef = useRef<HTMLDivElement>(null);
  const contentRef = useRef<HTMLDivElement>(null);

  return (
    <motion.div
      ref={boxRef}
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, ease: 'easeOut' }}
      style={{
        position: 'relative',
        width: '100%',
        height: '100%',
        minWidth: 0,
        border: '1px solid rgba(30,91,138,0.65)',
        borderRadius: 6,
        background: 'rgba(9,30,60,0.55)',
        boxShadow: '0 0 14px rgba(0,188,235,0.10), inset 0 0 24px rgba(0,188,235,0.04)',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
      }}
    >
      {/* 四角发光角标 */}
      <i style={{ position: 'absolute', top: -1, left: -1, width: 14, height: 14, borderTop: '2px solid #00e5ff', borderLeft: '2px solid #00e5ff', borderTopLeftRadius: 6, boxShadow: '-2px -2px 8px rgba(0,188,235,0.35)', pointerEvents: 'none' }} />
      <i style={{ position: 'absolute', top: -1, right: -1, width: 14, height: 14, borderTop: '2px solid #00e5ff', borderRight: '2px solid #00e5ff', borderTopRightRadius: 6, boxShadow: '2px -2px 8px rgba(0,188,235,0.35)', pointerEvents: 'none' }} />
      <i style={{ position: 'absolute', bottom: -1, left: -1, width: 14, height: 14, borderBottom: '2px solid #00e5ff', borderLeft: '2px solid #00e5ff', borderBottomLeftRadius: 6, boxShadow: '-2px 2px 8px rgba(0,188,235,0.35)', pointerEvents: 'none' }} />
      <i style={{ position: 'absolute', bottom: -1, right: -1, width: 14, height: 14, borderBottom: '2px solid #00e5ff', borderRight: '2px solid #00e5ff', borderBottomRightRadius: 6, boxShadow: '2px 2px 8px rgba(0,188,235,0.35)', pointerEvents: 'none' }} />

      {/* 标题栏 */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '7px 14px',
          flexShrink: 0,
          borderBottom: '1px solid rgba(30,91,138,0.6)',
          background: 'linear-gradient(90deg, rgba(0,188,235,0.14) 0%, rgba(0,188,235,0.02) 100%)',
        }}
      >
        <span style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0, fontSize: FONT.title, fontWeight: 600, color: '#e6f4ff', letterSpacing: 2 }}>
          {/* 发光竖条 */}
          <i
            style={{
              display: 'inline-block',
              width: 4,
              height: 20,
              borderRadius: 2,
              flexShrink: 0,
              background: 'linear-gradient(180deg, #00e5ff, #1e5b8a)',
              boxShadow: '0 0 8px rgba(0,188,235,0.7)',
            }}
          />
          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{title}</span>
        </span>
        <Space size={4}>
          {extra}
          {zoomKey && onZoom && (
            <Button
              type="text"
              size="small"
              icon={<ZoomInOutlined />}
              style={{ color: '#5a7f9f', fontSize: 14 }}
              title="放大查看"
              onClick={() => {
                const b = boxRef.current?.getBoundingClientRect();
                const c = contentRef.current?.getBoundingClientRect();
                if (b && c) {
                  onZoom({
                    key: zoomKey,
                    title,
                    w: b.width,
                    h: b.height,
                    contentW: c.width,
                    contentH: c.height,
                    content: children,
                  });
                }
              }}
            />
          )}
        </Space>
      </div>

      {/* 内容区 */}
      <div
        ref={contentRef}
        style={{
          flex: 1,
          padding: 12,
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
          minHeight: 0,
        }}
      >
        {children}
      </div>
    </motion.div>
  );
}
