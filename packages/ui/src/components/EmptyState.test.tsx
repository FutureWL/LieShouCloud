/**
 * EmptyState 单测.
 */
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { EmptyState } from './EmptyState';

describe('EmptyState', () => {
  it('默认描述「暂无数据」', () => {
    render(<EmptyState />);
    expect(screen.getByText('暂无数据')).toBeInTheDocument();
  });

  it('自定义描述', () => {
    render(<EmptyState description="没有客户" />);
    expect(screen.getByText('没有客户')).toBeInTheDocument();
  });

  it('支持 action 槽位', () => {
    render(<EmptyState action={<button>新建客户</button>} />);
    expect(screen.getByRole('button', { name: '新建客户' })).toBeInTheDocument();
  });
});