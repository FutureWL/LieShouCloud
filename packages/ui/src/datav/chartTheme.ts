/**
 * ECharts 深色大屏主题工厂（2026-08-25 · 下沉 packages/ui）.
 *
 * 所有驾驶舱图表统一从这里取 tooltip/轴/颜色，避免 option 重复。
 */

/** 深色 tooltip（item = 单项，如饼图；axis = 坐标轴触发，如折线/柱） */
export function darkTooltip(trigger: 'item' | 'axis' = 'item') {
  return {
    trigger,
    backgroundColor: 'rgba(9,30,60,0.92)',
    borderColor: '#1e5b8a',
    textStyle: { color: '#e6f4ff', fontSize: 13 },
    confine: true,
  };
}

/** 坐标轴标签 */
export const darkAxisLabel = { color: '#5a7f9f', fontSize: 13 };

/** 分割线 */
export const darkSplitLine = { lineStyle: { color: 'rgba(255,255,255,0.06)' } };

/** 轴主线 */
export const darkAxisLine = { lineStyle: { color: 'rgba(255,255,255,0.12)' } };

/** 饼图扇区通用样式（深色描边，防色块粘连） */
export const pieItemStyle = {
  borderColor: 'rgba(9,30,60,0.85)',
  borderWidth: 2,
  borderRadius: 4,
};

/** 默认调色板 */
export const darkColors = ['#00e5ff', '#fa8c16', '#52c41a', '#ff4d4f', '#722ed1', '#13c2c2'];
