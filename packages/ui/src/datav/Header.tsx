/**
 * 大屏顶栏（2026-08-25 泛化下沉 packages/ui）.
 *
 * 泛化前：标题硬编码「电网监控驾驶舱」；泛化后 title 走 props，业务文案留在 app 层。
 * 布局：左标题（发光装饰条）| 时钟 + 数据新鲜度 | 刷新/全屏/返回按钮。
 * 按钮统一深色科技风（.cockpit-btn 类，样式由使用方大屏页注入）。
 */
import { Button, Space } from 'antd';
import { FullscreenOutlined, ReloadOutlined } from '@ant-design/icons';
import { GLOW } from './theme';

export interface HeaderProps {
  /** 大屏标题（如「电网监控驾驶舱」） */
  title: string;
  clock: string;
  loading: boolean;
  isFullscreen: boolean;
  onRefresh: () => void;
  onToggleFullscreen: () => void;
  onBack: () => void;
}

export default function Header({
  title,
  clock,
  loading,
  isFullscreen,
  onRefresh,
  onToggleFullscreen,
  onBack,
}: HeaderProps) {
  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '4px 6px 10px',
      }}
    >
      {/* 左：标题 + 发光竖条装饰（与 Panel 标题栏同系） */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <i
          style={{
            display: 'inline-block',
            width: 4,
            height: 26,
            borderRadius: 2,
            background: 'linear-gradient(180deg, #00e5ff, #1e5b8a)',
            boxShadow: '0 0 8px rgba(0,188,235,0.7)',
          }}
        />
        <span style={{ fontSize: 28, fontWeight: 700, color: '#e6f4ff', letterSpacing: 4, textShadow: GLOW }}>
          {title}
        </span>
      </div>

      {/* 右：时钟 | 按钮组 */}
      <Space size={14} align="center">
        <span style={{ color: '#00e5ff', fontVariantNumeric: 'tabular-nums', fontSize: 36, fontWeight: 700, letterSpacing: 2, textShadow: GLOW }}>
          {clock}
        </span>
        <i style={{ display: 'inline-block', width: 1, height: 22, background: 'rgba(30,91,138,0.6)' }} />
        <Space size={8}>
          <Button size="small" className="cockpit-btn" icon={<ReloadOutlined />} loading={loading} onClick={onRefresh}>
            刷新
          </Button>
          <Button size="small" className="cockpit-btn" icon={<FullscreenOutlined />} onClick={onToggleFullscreen}>
            {isFullscreen ? '退出全屏' : '浏览器全屏'}
          </Button>
          <Button size="small" className="cockpit-btn-ghost" onClick={onBack}>
            返回工作台
          </Button>
        </Space>
      </Space>
    </div>
  );
}
