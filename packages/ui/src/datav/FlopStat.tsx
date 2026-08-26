/**
 * KPI 统计卡（翻牌滚动数字 · 2026-08-25 下沉 packages/ui）.
 *
 * 大屏经典翻牌效果：每位数字独立垂直滚动（0-9 数字列），
 * 值变化时数字像仪表盘计数器一样滚动到目标位。
 */
import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { FONT } from './theme';

interface FlopStatProps {
  title: string;
  value: number;
  suffix?: string;
  color?: string;
}

/** 数字列行高（与 56px 数字匹配） */
const LINE_H = 66;

/** 单位数翻牌：0-9 竖列，滚动到目标数字 */
function DigitRoller({ digit }: { digit: number }) {
  return (
    <span style={{ display: 'inline-block', overflow: 'hidden', height: LINE_H, verticalAlign: 'bottom' }}>
      <motion.span
        initial={false}
        animate={{ y: -digit * LINE_H }}
        transition={{ duration: 1.4, ease: [0.32, 0.72, 0.28, 1] }}
        style={{ display: 'flex', flexDirection: 'column' }}
      >
        {[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0].map((d, i) => (
          <span key={i} style={{ height: LINE_H, lineHeight: `${LINE_H}px`, textAlign: 'center' }}>
            {d}
          </span>
        ))}
      </motion.span>
    </span>
  );
}

/** 多位数：每位一个翻牌器（支持负号） */
function AnimatedNumber({ value }: { value: number }) {
  const str = String(value);
  return (
    <span style={{ display: 'inline-flex', alignItems: 'flex-end' }}>
      {str.split('').map((ch, i) =>
        ch === '-' || ch === '.' ? (
          <span key={i} style={{ height: LINE_H, lineHeight: `${LINE_H}px` }}>
            {ch}
          </span>
        ) : (
          <DigitRoller key={`${i}-${ch}`} digit={Number(ch)} />
        ),
      )}
    </span>
  );
}

export default function FlopStat({ title, value, suffix, color }: FlopStatProps) {
  const c = color ?? '#00e5ff';
  const [pulseKey, setPulseKey] = useState(0);

  // 数值变化 → 色条脉冲
  useEffect(() => {
    setPulseKey((k) => k + 1);
  }, [value]);

  return (
    <div
      style={{
        position: 'relative',
        border: '1px solid rgba(30,91,138,0.65)',
        borderTop: '2px solid rgba(0,188,235,0.8)',
        borderRadius: 8,
        background: 'rgba(9,30,60,0.55)',
        boxShadow: '0 0 12px rgba(0,188,235,0.10)',
        padding: '8px 10px 6px',
        textAlign: 'center',
        overflow: 'hidden',
      }}
    >
      {/* 顶部渐变发光色条（数值变化时脉冲） */}
      <motion.i
        key={pulseKey}
        initial={{ opacity: 0.4, scaleX: 0.6 }}
        animate={{ opacity: 1, scaleX: 1 }}
        transition={{ duration: 0.6, ease: 'easeOut' }}
        style={{
          position: 'absolute',
          top: 0,
          left: '15%',
          right: '15%',
          height: 2,
          borderRadius: 1,
          originX: 0.5,
          background: `linear-gradient(90deg, transparent, ${c}, transparent)`,
          boxShadow: `0 0 10px ${c}`,
        }}
      />
      <div style={{ color: '#8fc1e3', fontSize: FONT.strong, letterSpacing: 2, lineHeight: 1.4 }}>{title}</div>
      <div
        style={{
          fontSize: FONT.hero,
          fontWeight: 700,
          lineHeight: 1,
          color: c,
          fontVariantNumeric: 'tabular-nums',
          textShadow: `0 0 14px ${c}66`,
        }}
      >
        <AnimatedNumber value={value} />
        <span style={{ fontSize: 22, fontWeight: 500, marginLeft: 2 }}>{suffix ?? ''}</span>
      </div>
    </div>
  );
}
