/**
 * StatusTag 单测（Phase 9 共享包充实）.
 */
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { StatusTag } from "./StatusTag";

describe("StatusTag", () => {
  it("用传入的 meta 渲染 antd Tag", () => {
    render(<StatusTag meta={{ text: "已转化", color: "green" }} />);
    expect(screen.getByText("已转化")).toBeInTheDocument();
  });

  it("不同颜色独立", () => {
    const { container, rerender } = render(<StatusTag meta={{ text: "A", color: "red" }} />);
    expect(container.querySelector(".ant-tag-red")).not.toBeNull();
    rerender(<StatusTag meta={{ text: "B", color: "blue" }} />);
    expect(container.querySelector(".ant-tag-blue")).not.toBeNull();
  });
});
