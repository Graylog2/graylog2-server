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
export const createStreamFixture = (
  identifier: string,
  defaultStream: boolean = false,
  editable: boolean = true,
  matchingType: 'AND' | 'OR' = 'OR',
) => ({
  id: `stream-id-${identifier}`,
  creator_user_id: `stream-creator-id-${identifier}`,
  outputs: [],
  matching_type: matchingType,
  description: `Stream Description ${identifier}`,
  created_at: new Date().toISOString(),
  disabled: false,
  rules: [],
  title: `Stream Title ${identifier}`,
  content_pack: undefined,
  remove_matches_from_default_stream: false,
  index_set_id: `index-set-id-${identifier}`,
  is_default: defaultStream,
  is_editable: editable,
  categories: [],
});

export default createStreamFixture;
