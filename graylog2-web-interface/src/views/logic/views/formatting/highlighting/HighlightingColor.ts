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
import * as chroma from 'chroma-js';

export type HighlightingColorJson = StaticColorJson | GradientColorJson;

abstract class HighlightingColor {
  abstract get type(): 'static' | 'gradient';

  static fromJSON(json: HighlightingColorJson) {
    switch (json.type) {
      // eslint-disable-next-line @typescript-eslint/no-use-before-define
      case 'gradient': return GradientColor.fromJSON(json);
      // eslint-disable-next-line @typescript-eslint/no-use-before-define
      case 'static': return StaticColor.fromJSON(json);
      default: // @ts-ignore
        throw new Error(`Invalid highlighting color type: ${json.type}`);
    }
  }

  abstract colorFor(value: any);

  isStatic(): this is StaticColor {
    return this.type === 'static';
  }

  isGradient(): this is GradientColor {
    return this.type === 'gradient';
  }
}

type StaticColorJson = {
  type: 'static',
  color: string,
};

export class StaticColor extends HighlightingColor {
  private readonly _color: string;

  private constructor(color: string) {
    super();
    this._color = color;
  }

  // eslint-disable-next-line class-methods-use-this
  get type() {
    return 'static' as const;
  }

  get color(): string {
    return this._color;
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  colorFor(value: any) {
    return this.color;
  }

  static fromJSON({ color }: StaticColorJson) {
    return new StaticColor(color);
  }

  static create(color: string) {
    return new StaticColor(color);
  }

  toJSON() {
    const { color } = this;

    return {
      type: 'static',
      color,
    };
  }
}

type GradientColorJson = {
  type: 'gradient',
  gradient: string,
  lower: number,
  upper: number,
}

const parseValue = (value: any, defaultValue: number = 0): number => {
  if (typeof value === 'number') {
    return value as number;
  }

  try {
    return Number.parseFloat(value);
  } catch (ignored) {
    return defaultValue;
  }
};

export class GradientColor extends HighlightingColor {
  private readonly _gradient: string;

  private readonly _lower: number;

  private readonly _upper: number;

  private readonly _scale: chroma.Scale;

  private constructor(gradient: string, lower: number, upper: number) {
    super();
    this._lower = lower;
    this._upper = upper;
    this._gradient = gradient;

    this._scale = chroma.scale(gradient);
  }

  // eslint-disable-next-line class-methods-use-this
  get type() {
    return 'gradient' as const;
  }

  get gradient(): string {
    return this._gradient;
  }

  get lower(): number {
    return this._lower;
  }

  get upper(): number {
    return this._upper;
  }

  colorFor(value: any) {
    const parsedValue = parseValue(value, this.lower);

    const spread = this.upper - this.lower;
    const normalizedValue = Math.max(this.lower, Math.min(this.upper, parsedValue));

    return this._scale((normalizedValue - this.lower) / spread);
  }

  static fromJSON({ gradient, lower, upper }: GradientColorJson) {
    return new GradientColor(gradient, lower, upper);
  }

  static create(gradient: string, lower: number, upper: number) {
    return new GradientColor(gradient, lower, upper);
  }

  withGradient(gradient: string) {
    return GradientColor.create(gradient, this.lower, this.upper);
  }

  withLower(lower: number) {
    return GradientColor.create(this.gradient, lower, this.upper);
  }

  withUpper(upper: number) {
    return GradientColor.create(this.gradient, this.lower, upper);
  }

  toJSON() {
    const { gradient, lower, upper } = this;

    return {
      type: 'gradient',
      gradient,
      lower,
      upper,
    };
  }
}

export default HighlightingColor;
