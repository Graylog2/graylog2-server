/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';

export type ShapeName = 'circle' | 'square' | 'diamond' | 'triangleUp' | 'cross' | 'star';

export const SHAPE_ORDER: ShapeName[] = ['circle', 'square', 'diamond', 'triangleUp', 'cross', 'star'];

export const SHAPE_LABELS: Record<ShapeName, string> = {
  circle: 'Circle',
  square: 'Square',
  diamond: 'Diamond',
  triangleUp: 'Triangle',
  cross: 'Cross',
  star: 'Star',
};

/** Resolve the shape for a field value, respecting manual overrides and auto-assignment order. */
export const resolveShape = (value: string, overrides: Record<string, string>, autoIndex: number): ShapeName =>
  (overrides[value] as ShapeName) ?? SHAPE_ORDER[autoIndex % SHAPE_ORDER.length];

/**
 * Build a map from field value → ShapeName given all unique values in the data.
 * Manual overrides take precedence; remaining values are assigned in encounter order.
 */
export const buildShapeIndex = (values: string[], overrides: Record<string, string>): Map<string, ShapeName> => {
  const map = new Map<string, ShapeName>();
  let autoIdx = 0;

  values.forEach((v) => {
    if (overrides[v]) {
      map.set(v, overrides[v] as ShapeName);
    } else {
      // Skip slots already claimed by overrides to avoid collisions
      while (Object.values(overrides).includes(SHAPE_ORDER[autoIdx % SHAPE_ORDER.length]) && autoIdx < SHAPE_ORDER.length) {
        autoIdx += 1;
      }
      map.set(v, SHAPE_ORDER[autoIdx % SHAPE_ORDER.length]);
      autoIdx += 1;
    }
  });

  return map;
};

type DotProps = {
  cx: number;
  cy: number;
  r: number;
  fill: string;
  opacity: number;
  style?: React.CSSProperties;
  onClick?: (e: React.MouseEvent<SVGElement>) => void;
};

/** Render a single event dot as the appropriate SVG shape. */
export const renderDot = (shape: ShapeName, { cx, cy, r, fill, opacity, style, onClick }: DotProps): React.ReactElement => {
  const shared = { fill, opacity, style, onClick } as const;

  switch (shape) {
    case 'square':
      return <rect x={cx - r} y={cy - r} width={r * 2} height={r * 2} rx={1} {...shared} />;
    case 'diamond':
      return <path d={`M${cx},${cy - r}L${cx + r},${cy}L${cx},${cy + r}L${cx - r},${cy}Z`} {...shared} />;
    case 'triangleUp':
      return <path d={`M${cx},${cy - r}L${cx + r * 1.1},${cy + r}L${cx - r * 1.1},${cy + r}Z`} {...shared} />;
    case 'cross': {
      const arm = r * 0.35;
      return (
        <path
          d={`M${cx - r},${cy - arm}H${cx + r}V${cy + arm}H${cx - r}ZM${cx - arm},${cy - r}V${cy + r}H${cx + arm}V${cy - r}Z`}
          {...shared}
        />
      );
    }
    case 'star': {
      const outer = r;
      const inner = r * 0.45;
      const points = 6;
      let d = '';

      for (let i = 0; i < points * 2; i += 1) {
        const angle = (Math.PI / points) * i - Math.PI / 2;
        const radius = i % 2 === 0 ? outer : inner;
        const x = cx + radius * Math.cos(angle);
        const y = cy + radius * Math.sin(angle);
        d += `${i === 0 ? 'M' : 'L'}${x.toFixed(2)},${y.toFixed(2)}`;
      }

      return <path d={`${d}Z`} {...shared} />;
    }
    case 'circle':
    default:
      return <circle cx={cx} cy={cy} r={r} {...shared} />;
  }
};

/** Small SVG preview icon for use in the edit UI (shape picker). */
export const ShapeIcon = ({ shape, size = 16, color = 'currentColor' }: { shape: ShapeName; size?: number; color?: string }) => {
  const r = size * 0.35;
  const mid = size / 2;

  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} style={{ display: 'inline-block', verticalAlign: 'middle' }}>
      {renderDot(shape, { cx: mid, cy: mid, r, fill: color, opacity: 1 })}
    </svg>
  );
};
