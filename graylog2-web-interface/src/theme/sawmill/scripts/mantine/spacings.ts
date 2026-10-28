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
import { ThemeBase } from '../../types';

const SPACE = 0.08; // ratio we use for our calculations
const FIBONACCI = [3, 5, 8, 13, 21, 34, 55, 89, 144]; // skipped [1, 2]

const generateSpacings = (spacingNames: ThemeBase['spacings']) => {
  const result: Record<string, string> = {};

  spacingNames.forEach((spacingName, index) => {
    const value = SPACE * FIBONACCI[index];
    result[spacingName] = `${value.toFixed(2)}rem`;
  });

  return result;
};

export default generateSpacings;
