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

interface InputBase {
  title: string,
  type: string,
  global: boolean,
  node?: string,
}

export interface Input extends InputBase {
  id: string,
  name: string,
  created_at: string,
  creator_user_id: string,
  content_pack?: boolean,
  static_fields: { [field: string]: any },
  attributes: {
    [type: string]: any,
  },
}

export interface ConfiguredInput extends InputBase {
  configuration: {
    [type: string]: any,
  },
}

export type Codec ={
  type: string,
  name: string,
  requested_configuration: {
    [key: string]: {
      [key: string]: any,
    },
  },
};

export type CodecTypes = {
  [key: string]: Codec,
};
