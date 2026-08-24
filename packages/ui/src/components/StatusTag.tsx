/**
 * StatusTag — 跨端共享的状态 Tag 包装（Phase 9 共享包充实）.
 *
 * 用法：把各业务模块已有的 STATUS_META 对象传入，组件负责 antd Tag 渲染。
 * 例：`<StatusTag meta={STATUS_META[row.status]} />`
 *
 * 为什么不做 `<StatusTag status="NEW" meta={STATUS_META} />`：
 *   - 让组件保持 dumb（不依赖业务枚举字符串）
 *   - 跨业务复用：User/Customer/Tenant/后续角色等都可传自己的 META
 */
import { Tag } from 'antd';
import type { ReactNode } from 'react';

import type { StatusMeta } from '@lieshoucloud/types';

interface StatusTagProps {
  meta: StatusMeta;
  /** 额外 className（用于测试 / 自定义样式） */
  className?: string;
}

export function StatusTag({ meta, className }: StatusTagProps): ReactNode {
  return (
    <Tag color={meta.color} className={className}>
      {meta.text}
    </Tag>
  );
}