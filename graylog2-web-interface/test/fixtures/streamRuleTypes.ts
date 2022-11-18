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

/* eslint-disable import/prefer-default-export */

export const streamRuleTypes = [
  { id: 1, short_desc: 'match exactly', long_desc: 'match exactly', name: 'Stream rule match exactly' },
  { id: 2, short_desc: 'match regular expression', long_desc: 'match regular expression', name: 'Stream rule match regular' },
  { id: 3, short_desc: 'greater than', long_desc: 'greater than', name: 'Stream rule greater than' },
  { id: 4, short_desc: 'smaller than', long_desc: 'smaller than', name: 'Stream rule smaller than' },
  { id: 5, short_desc: 'field presence', long_desc: 'field presence', name: 'Stream rule field presence' },
  { id: 6, short_desc: 'contain', long_desc: 'contain', name: 'Stream rule contain' },
  { id: 7, short_desc: 'always match', long_desc: 'always match', name: 'Stream rule always match' },
  { id: 8, short_desc: 'match input', long_desc: 'match input', name: 'Stream rule match input' },
];
