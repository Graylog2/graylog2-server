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
import type { BlockDict } from './types';

const getDictForFunction = (dict: BlockDict[], functionName: string) : BlockDict | undefined => (
  dict?.find((entry) => entry.name === functionName)
);

const jsonifyText = (text: string): string => {
  try {
    JSON.parse(text);

    return text;
  } catch {
    try {
      const rawMessageToJson = `{"${
        text
          .trim()
          .replace(/^\s*\n/gm, '')
          .replace(/"|'|`/g, '')
          .replace(/=/g, ':')
          .split('\n')
          .map((line) => line.trim().split(':').map((keyValue) => keyValue.trim()))
          .filter((keyValue) => keyValue[0] && keyValue[1])
          .map((keyValue) => keyValue.join('":"'))
          .join('","')
      }"}`;

      JSON.parse(rawMessageToJson);

      return rawMessageToJson;
    } catch {
      return text;
    }
  }
};

export { getDictForFunction, jsonifyText };
