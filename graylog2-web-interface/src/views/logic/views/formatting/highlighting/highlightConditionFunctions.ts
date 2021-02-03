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

// Todo: this is copied from components/messagelist
const isNumeric = (str: any) => {
  if (typeof str === 'number') return true;
  if (typeof str !== 'string') return false;

  if (str.trim() === '') return false;

  return !Number.isNaN(Number(str));
};

const checkNumeric = (a: any, b: any, callback: (a: any, b: any) => boolean): boolean => {
  if (isNumeric(a) && isNumeric(b)) {
    return callback(parseFloat(a), parseFloat(b));
  }

  return false;
};

const highlightConditionFunctions = {
  equal: (a, b) => String(a) === String(b),
  not_equal: (a, b) => String(a) !== String(b),
  less_equal: (a, b) => checkNumeric(a, b, (aFloat, bFloat) => aFloat <= bFloat),
  greater_equal: (a, b) => checkNumeric(a, b, (aFloat, bFloat) => aFloat >= bFloat),
  greater: (a, b) => checkNumeric(a, b, (aFloat, bFloat) => aFloat > bFloat),
  less: (a, b) => checkNumeric(a, b, (aFloat, bFloat) => aFloat < bFloat),
};

export default highlightConditionFunctions;
