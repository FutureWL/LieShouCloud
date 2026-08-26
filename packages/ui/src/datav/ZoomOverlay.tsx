/**
 * 卡片单独全屏层（点面板标题栏放大图标触发；等比放大 + 顶栏关闭/浏览器全屏 ·
 * 2026-08-25 下沉 packages/ui）.
 */
import { Button, Space } from 'antd';
import type { ZoomInfo } from './Panel';
import { BORDER } from './theme';

interface ZoomOverlayProps {
  zoom: ZoomInfo | null;
  isFullscreen: boolean;
  onToggleFullscreen: () => void;
  onClose: () => void;
}

export default function ZoomOverlay({ zoom, isFullscreen, onToggleFullscreen, onClose }: ZoomOverlayProps) {
  if (!zoom) return null;
  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 500,
        background: 'rgba(4,12,24,0.96)',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '8px 20px',
          borderBottom: BORDER,
          background: 'rgba(0,188,235,0.08)',
        }}
      >
        <span style={{ fontSize: 18, color: '#00e5ff', letterSpacing: 3 }}>▍{zoom.title}</span>
        <Space size={8}>
          <Button size="small" onClick={onToggleFullscreen}>
            {isFullscreen ? '退出浏览器全屏' : '浏览器全屏'}
          </Button>
          <Button size="small" type="primary" onClick={onClose}>
            关闭
          </Button>
        </Space>
      </div>
      <div style={{ flex: 1, display: 'grid', placeItems: 'center', overflow: 'auto', padding: 16 }}>
        <ZoomContent info={zoom} />
      </div>
    </div>
  );
}

/** 卡片单独全屏：按面板原始尺寸等比放大内容（scale = min(视口/面板, 2.8)） */
function ZoomContent({ info }: { info: ZoomInfo }) {
  const scale = Math.min(
    (window.innerWidth - 60) / info.w,
    (window.innerHeight - 120) / info.h,
    2.8,
  );
  return (
    <div style={{ width: info.w * scale, height: info.h * scale }}>
      <div
        style={{
          width: info.w,
          height: info.h,
          transform: `scale(${scale})`,
          transformOrigin: 'top left',
        }}
      >
        <div
          style={{
            width: '100%',
            height: '100%',
            background: 'rgba(9,30,60,0.6)',
            border: BORDER,
            borderRadius: 8,
            padding: 10,
            display: 'flex',
            flexDirection: 'column',
            overflow: 'auto',
          }}
        >
          {info.content}
        </div>
      </div>
    </div>
  );
}
