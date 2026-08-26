/**
 * RoleTag 单测.
 */
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { RoleTag, ROLE_COLORS } from './RoleTag';

describe('RoleTag', () => {
  it('PLATFORM_ADMIN → gold', () => {
    const { container } = render(<RoleTag role="PLATFORM_ADMIN" />);
    expect(screen.getByText('平台管理员')).toBeInTheDocument();
    expect(container.querySelector('.ant-tag-gold')).not.toBeNull();
  });

  it('TENANT_ADMIN → orange', () => {
    const { container } = render(<RoleTag role="TENANT_ADMIN" />);
    expect(container.querySelector('.ant-tag-orange')).not.toBeNull();
  });

  it('USER → blue（默认）', () => {
    const { container } = render(<RoleTag role="USER" />);
    expect(container.querySelector('.ant-tag-blue')).not.toBeNull();
  });

  it('未知角色 → blue 默认色', () => {
    const { container } = render(<RoleTag role="SUPERADMIN_UNKNOWN" />);
    expect(container.querySelector('.ant-tag-blue')).not.toBeNull();
  });

  it('自定义 colorMap 覆盖默认', () => {
    const { container } = render(
      <RoleTag role="PLATFORM_ADMIN" colorMap={{ PLATFORM_ADMIN: 'red' }} />,
    );
    expect(container.querySelector('.ant-tag-red')).not.toBeNull();
  });

  it('ROLE_COLORS 包含三大角色', () => {
    expect(ROLE_COLORS.PLATFORM_ADMIN).toBe('gold');
    expect(ROLE_COLORS.TENANT_ADMIN).toBe('orange');
    expect(ROLE_COLORS.USER).toBe('blue');
  });
});