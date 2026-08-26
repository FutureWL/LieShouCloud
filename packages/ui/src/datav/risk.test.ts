/**
 * datavRisk 纯函数单测（2026-08-25 随驾驶舱下沉 packages/ui）.
 *
 * 原 iotMeta.test.ts 中的大屏通用纯函数（温度排行/时钟/告警聚合/风险指数），
 * 与业务无关，随组件一起进共享包。
 */
import { describe, expect, it } from 'vitest';

import {
  alertByDate,
  alertByHour,
  calcRiskScore,
  findHottestNodeKey,
  formatClock,
  RISK_LEVEL_META,
  tempRanking,
  type RankableDevice,
} from './risk';

describe('tempRanking（大屏温度排行）', () => {
  const dev = (id: number, t: number | null | undefined): RankableDevice => ({ id, name: `设备${id}`, maxTemperature: t });

  it('按温度降序 + 只取有温度的 + Top N', () => {
    const devices = [dev(1, 30), dev(2, null), dev(3, 75), dev(4, 55), dev(5, undefined)];
    const r = tempRanking(devices, 3);
    expect(r.map((x) => x.device.id)).toEqual([3, 4, 1]);
    expect(r[0].temp).toBe(75);
  });

  it('空/全无温度 → 空数组', () => {
    expect(tempRanking([])).toEqual([]);
    expect(tempRanking([dev(1, null)])).toEqual([]);
  });
});

describe('formatClock（大屏时钟）', () => {
  it('补零输出 HH:mm:ss', () => {
    expect(formatClock(new Date(2026, 7, 24, 9, 5, 3))).toBe('09:05:03');
    expect(formatClock(new Date(2026, 7, 24, 23, 59, 59))).toBe('23:59:59');
  });
});

describe('alertByHour（告警时段分布）', () => {
  it('按 createdAt 小时聚合 24 桶；坏时间忽略（期望小时同源计算，与主机时区无关）', () => {
    const h9 = new Date(2026, 7, 24, 9, 15).toISOString();
    const h9b = new Date(2026, 7, 24, 9, 30).toISOString();
    const h14 = new Date(2026, 7, 24, 14, 0).toISOString();
    const buckets = alertByHour([
      { createdAt: h9 },
      { createdAt: h9b },
      { createdAt: h14 },
      { createdAt: 'bad' },
    ]);
    expect(buckets).toHaveLength(24);
    expect(buckets[new Date(2026, 7, 24, 9).getHours()]).toBe(2);
    expect(buckets[new Date(2026, 7, 24, 14).getHours()]).toBe(1);
    expect(buckets.reduce((a, b) => a + b, 0)).toBe(3);
  });

  it('空列表 → 全 0', () => {
    expect(alertByHour([]).every((v) => v === 0)).toBe(true);
  });
});

describe('findHottestNodeKey（影子最热节点）', () => {
  it('取 node{n}_temperature 最大值对应的 key', () => {
    expect(findHottestNodeKey({ node1_temperature: 25, node2_temperature: 31.5, node3_temperature: 28 })).toBe('node2_temperature');
  });

  it('无节点温度 / 非数值 → null', () => {
    expect(findHottestNodeKey({ temperature: 30 })).toBeNull();
    expect(findHottestNodeKey({ node1_temperature: 'bad' })).toBeNull();
    expect(findHottestNodeKey({})).toBeNull();
  });
});

describe('alertByDate（近 7 天告警趋势）', () => {
  it('按本地日界聚合，升序 7 天；今天计数正确', () => {
    const now = new Date();
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
    const trend = alertByDate([{ createdAt: new Date(todayStart + 3600000).toISOString() }], 7);
    expect(trend).toHaveLength(7);
    expect(trend[6].count).toBe(1); // 最后一项=今天
    expect(trend[5].count).toBe(0);
  });

  it('空列表 → 7 天全 0', () => {
    expect(alertByDate([], 7).every((x) => x.count === 0)).toBe(true);
  });
});

describe('calcRiskScore（风险指数 0-100）', () => {
  const zero = {
    total: 12,
    online: 12,
    overTempDevices: 0,
    warnTempDevices: 0,
    pendingAlerts: 0,
    pdOverDevices: 0,
  };

  it('全绿 → 0 分低风险', () => {
    const r = calcRiskScore(zero);
    expect(r.score).toBe(0);
    expect(r.level).toBe('low');
    expect(r.parts).toHaveLength(4);
  });

  it('全离线 + 温度告警 + 告警积压 + 局放超标 → 100 分高风险', () => {
    const r = calcRiskScore({
      total: 12,
      online: 0,
      overTempDevices: 3,
      warnTempDevices: 0,
      pendingAlerts: 4,
      pdOverDevices: 2,
    });
    expect(r.score).toBe(100);
    expect(r.level).toBe('high');
  });

  it('部分风险：离线率 + 温度预警按比例计分，封顶生效', () => {
    const r = calcRiskScore({
      total: 12,
      online: 9, // 3 离线 → 40*3/12=10
      overTempDevices: 0,
      warnTempDevices: 2, // 5*2=10
      pendingAlerts: 1, // 20/4=5
      pdOverDevices: 0,
    });
    expect(r.score).toBe(25);
    expect(r.level).toBe('medium');
  });

  it('温度告警封顶 30（5 台也只算 30）', () => {
    const r = calcRiskScore({ ...zero, overTempDevices: 5 });
    expect(r.score).toBe(30);
  });

  it('等级边界：20=中 / 50=高', () => {
    expect(calcRiskScore({ ...zero, pendingAlerts: 4 }).level).toBe('medium'); // 20 分
    expect(calcRiskScore({ ...zero, online: 0 }).level).toBe('medium'); // 40 分
    expect(calcRiskScore({ ...zero, online: 0, warnTempDevices: 2 }).score).toBe(50);
    expect(calcRiskScore({ ...zero, online: 0, warnTempDevices: 2 }).level).toBe('high');
  });

  it('等级元数据齐全', () => {
    expect(RISK_LEVEL_META.low.text).toBe('低风险');
    expect(RISK_LEVEL_META.medium.text).toBe('中风险');
    expect(RISK_LEVEL_META.high.text).toBe('高风险');
  });
});
