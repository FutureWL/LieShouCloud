/**
 * 环形/饼图占比图（ECharts · 2026-08-25 下沉 packages/ui，无 DataV 兼容层）.
 *
 * - ring：环形图（内径 = 外径 - 16%）
 * - pie：实心饼图（扇区显示 名称+百分比）
 * - fill 时随父容器自适应高度
 */
import { useMemo } from 'react';
import EChart from '../components/EChart';

export interface DvRingDatum {
  name: string;
  value: number;
  color?: string;
}

export interface DvRingProps {
  data: DvRingDatum[];
  height?: number;
  radius?: string;
  fill?: boolean;
  type?: 'ring' | 'pie';
}

export default function DvRing({ data, height = 140, radius = '70%', fill = false, type = 'ring' }: DvRingProps) {
  const option = useMemo(() => {
    const colors = data.map((d) => d.color).filter((c): c is string => Boolean(c));
    const seriesData = data.map((d) => ({ name: d.name, value: d.value }));
    const isRing = type === 'ring';

    return {
      color: colors,
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(9,30,60,0.92)',
        borderColor: '#1e5b8a',
        textStyle: { color: '#e6f4ff', fontSize: 13 },
        formatter: '{b}: {c} ({d}%)',
      },
      series: [
        {
          name: '占比',
          type: 'pie',
          radius: isRing ? [Math.max(parseFloat(String(radius)) - 16, 0) + '%', radius] : radius,
          center: ['50%', '52%'],
          data: seriesData,
          label: isRing ? { show: false } : { show: true, color: '#8fc1e3', fontSize: 13, formatter: '{b}: {d}%' },
          labelLine: isRing ? { show: false } : { lineStyle: { color: 'rgba(143,193,227,0.4)' } },
          itemStyle: {
            borderColor: 'rgba(9,30,60,0.85)',
            borderWidth: 2,
            borderRadius: 4,
          },
          emphasis: { scaleSize: 6 },
          animationDuration: 700,
        },
      ],
    };
  }, [data, radius, type]);

  return (
    <div style={{ width: '100%', height: fill ? '100%' : height, minHeight: fill ? 80 : undefined }}>
      <EChart option={option} />
    </div>
  );
}
