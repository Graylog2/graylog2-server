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
import { useCallback } from 'react';
import type { ColorResult } from 'react-color';
import { SwatchesPicker } from 'react-color';

type Props = {
  color?: string
  colors?: Array<Array<string>>
  height?: number
  width?: number
  onChange: (color: string, event: React.ChangeEvent<HTMLInputElement>) => void,
};

/**
 * Color picker component that let the user select a color from a list of 95 colors grouped by hue.
 */
const ColorPicker = ({ onChange, height = (135 * 2) + 24 + 16, width = (50 * 5), ...rest }: Props) => {
  const onColorChange = useCallback((color: ColorResult, event: React.ChangeEvent<HTMLInputElement>) => {
    onChange(color.hex, event);
  }, [onChange]);

  return (
    <SwatchesPicker height={height} width={width} {...rest} onChange={onColorChange} />
  );
};

export default ColorPicker;
