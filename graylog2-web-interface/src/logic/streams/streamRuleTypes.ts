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

// The actual stream types will be provided by the backend.
// This map is only being used for frontend related logic.
const STREAM_RULE_TYPES = {
  FIELD_PRESENCE: 5,
  ALWAYS_MATCHES: 7,
  MATCH_INPUT: 8,
};

export default STREAM_RULE_TYPES;
