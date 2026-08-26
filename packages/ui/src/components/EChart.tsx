/**
 * ECharts React 封装（大屏图表 · 2026-08 迁移 · 下沉 packages/ui）.
 *
 * - 挂载时 init，卸载时 dispose（React 19 安全）
 * - ResizeObserver 随容器自适应尺寸
 * - option 变化 setOption(option, true) 全量替换
 * - onEvents 注册 echarts 事件（click 等），依赖变化时自动重绑
 */
import { useEffect, useRef } from 'react';
import * as echarts from 'echarts';

export interface EChartProps {
  option: echarts.EChartsOption | Record<string, unknown>;
  style?: React.CSSProperties;
  /** echarts 事件处理器，如 { click: (params) => void } */
  onEvents?: Record<string, (params: unknown) => void>;
}

export default function EChart({ option, style, onEvents }: EChartProps) {
  const domRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<echarts.ECharts | null>(null);

  // 初始化 + 尺寸自适应 + 销毁
  useEffect(() => {
    if (!domRef.current) return;
    const chart = echarts.init(domRef.current, undefined, { renderer: 'canvas' });
    chartRef.current = chart;
    const ro = new ResizeObserver(() => chart.resize());
    ro.observe(domRef.current);
    return () => {
      ro.disconnect();
      chart.dispose();
      chartRef.current = null;
    };
  }, []);

  // option 更新
  useEffect(() => {
    chartRef.current?.setOption(option as echarts.EChartsOption, true);
  }, [option]);

  // 事件绑定
  useEffect(() => {
    const chart = chartRef.current;
    if (!chart || !onEvents) return;
    const entries = Object.entries(onEvents);
    entries.forEach(([evt, fn]) => chart.on(evt, fn as (params: unknown) => void));
    return () => entries.forEach(([evt, fn]) => chart.off(evt, fn as (params: unknown) => void));
  }, [onEvents]);

  return <div ref={domRef} style={{ width: '100%', height: '100%', ...style }} />;
}
