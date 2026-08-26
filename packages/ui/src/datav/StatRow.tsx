/**
 * 大屏 KPI 统计行（2026-08-25 泛化下沉 packages/ui）.
 *
 * 泛化前：StatRow 绑定 IotOverview（设备总数/在线/离线/告警），只服务电网驾驶舱；
 * 泛化后：接收 stats 数组，任何行业版大屏都能用（窄屏自动 2 列 / 标准 5 列）。
 */
import FlopStat from './FlopStat';

export interface StatItem {
  title: string;
  value: number;
  suffix?: string;
  color?: string;
}

interface StatRowProps {
  /** KPI 统计项（数字 > 单位 > 标题，状态用颜色表达） */
  stats: StatItem[];
  /** 列数（窄屏 2 列 / 标准 5 列） */
  cols?: number;
}

export default function StatRow({ stats, cols = 5 }: StatRowProps) {
  const gridCols = cols === 1 ? 'repeat(2, 1fr)' : `repeat(${Math.min(cols, stats.length || 1)}, 1fr)`;
  return (
    <div style={{ display: 'grid', gridTemplateColumns: gridCols, gap: 10, marginBottom: 10 }}>
      {stats.map((s) => (
        <FlopStat key={s.title} title={s.title} value={s.value} suffix={s.suffix} color={s.color} />
      ))}
    </div>
  );
}
