/**
 * 大屏纯函数与风险计算（无 React 依赖 · 可单测 · 2026-08-25 下沉 packages/ui）.
 *
 * 从驾驶舱拆出的纯逻辑：温度排行 / 时钟 / 告警统计 / 风险指数。
 */

/** 温度排行输入：health 聚合项（deviceId）或设备列表（id）均可 */
export interface RankableDevice {
  id: number;
  name: string;
  maxTemperature?: number | null;
}

/** 温度排行（有上报温度的设备按最高节点温度降序 Top N） */
export function tempRanking(devices: RankableDevice[], top = 8): { device: RankableDevice; temp: number }[] {
  return devices
    .filter(
      (d) =>
        d.maxTemperature !== null &&
        d.maxTemperature !== undefined &&
        Number.isFinite(d.maxTemperature),
    )
    .map((d) => ({ device: d, temp: Number(d.maxTemperature) }))
    .sort((a, b) => b.temp - a.temp)
    .slice(0, top);
}

/** 当前时钟 HH:mm:ss */
export function formatClock(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

/** 告警按小时分布（0-23 桶；跨日/坏时间忽略） */
export function alertByHour(alerts: { createdAt: string }[]): number[] {
  const buckets = new Array<number>(24).fill(0);
  for (const a of alerts) {
    const h = new Date(a.createdAt).getHours();
    if (!Number.isNaN(h)) buckets[h] += 1;
  }
  return buckets;
}

/** 影子快照里最热的节点 key（node{n}_temperature 最大值；无节点温度 → null） */
export function findHottestNodeKey(shadow: Record<string, unknown>): string | null {
  let best: string | null = null;
  let bestV = -Infinity;
  for (const [k, v] of Object.entries(shadow)) {
    if (!/^node\d+_temperature$/.test(k)) continue;
    const n = Number(v);
    if (Number.isFinite(n) && n > bestV) {
      bestV = n;
      best = k;
    }
  }
  return best;
}

/** 近 N 天每日告警数（本地日界，升序 [today-N+1 … today]） */
export function alertByDate(
  alerts: { createdAt: string }[],
  days = 7,
): { label: string; count: number }[] {
  const out: { label: string; count: number }[] = [];
  const now = new Date();
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth(), now.getDate() - i);
    const label = `${d.getMonth() + 1}/${d.getDate()}`;
    const start = d.getTime();
    const end = start + 86400000;
    const count = alerts.filter((a) => {
      const t = new Date(a.createdAt).getTime();
      return t >= start && t < end;
    }).length;
    out.push({ label, count });
  }
  return out;
}

// ── 风险指数（综合健康评分 · 0-100 越高越危险） ──

export interface RiskInput {
  total: number;
  online: number;
  /** 温度 ≥70℃ 台数（告警级） */
  overTempDevices: number;
  /** 温度 50~70℃ 台数（预警级） */
  warnTempDevices: number;
  /** 未确认告警数 */
  pendingAlerts: number;
  /** 局放超标台数（超声 >30dBuv 示例阈值） */
  pdOverDevices: number;
}

export interface RiskPart {
  key: string;
  label: string;
  /** 权重（总分占比） */
  weight: number;
  /** 该维度得分 */
  score: number;
  detail: string;
}

export interface RiskResult {
  score: number;
  level: 'low' | 'medium' | 'high';
  parts: RiskPart[];
}

/**
 * 风险指数：离线率 40 + 温度超标 30 + 未确认告警 20 + 局放超标 10。
 * 分级：<20 低 / 20-49 中 / ≥50 高（电网示例阈值，规则页可调）。
 */
export function calcRiskScore(input: RiskInput): RiskResult {
  const total = Math.max(input.total, 1);
  const offline = Math.max(total - input.online, 0);
  const parts: RiskPart[] = [
    {
      key: 'offline',
      label: '离线设备',
      weight: 40,
      score: Math.round(((offline / total) * 40) * 10) / 10,
      detail: `${offline}/${total} 台离线`,
    },
    {
      key: 'temp',
      label: '温度超标',
      weight: 30,
      // 告警级每台 10 分、预警级每台 5 分，封顶 30
      score: Math.min(input.overTempDevices * 10 + input.warnTempDevices * 5, 30),
      detail: `${input.overTempDevices} 台告警 / ${input.warnTempDevices} 台预警`,
    },
    {
      key: 'alerts',
      label: '未确认告警',
      weight: 20,
      // 4 条封顶
      score: Math.min((input.pendingAlerts / 4) * 20, 20),
      detail: `${input.pendingAlerts} 条待确认`,
    },
    {
      key: 'pd',
      label: '局放超标',
      weight: 10,
      // 2 台封顶
      score: Math.min((input.pdOverDevices / 2) * 10, 10),
      detail: `${input.pdOverDevices} 台超标`,
    },
  ];
  const score = Math.round(parts.reduce((s, p) => s + p.score, 0) * 10) / 10;
  const level: RiskResult['level'] = score < 20 ? 'low' : score < 50 ? 'medium' : 'high';
  return { score, level, parts };
}

export const RISK_LEVEL_META: Record<
  RiskResult['level'],
  { text: string; color: string; glow: string }
> = {
  low: { text: '低风险', color: '#52c41a', glow: 'rgba(82,196,26,0.5)' },
  medium: { text: '中风险', color: '#fa8c16', glow: 'rgba(250,140,22,0.5)' },
  high: { text: '高风险', color: '#ff4d4f', glow: 'rgba(255,77,79,0.6)' },
};
