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
import type { Stream } from 'stores/streams/StreamsStore';

export const stream: Stream = {
  content_pack: null,
  created_at: '2020-10-10T10:10:00.000Z',
  creator_user_id: 'admin',
  description: 'Lorem ipsum dolor sit amet',
  disabled: true,
  id: 'stream-id-1',
  index_set_id: 'index-set-id-1',
  is_default: false,
  is_editable: true,
  matching_type: 'AND',
  outputs: [],
  remove_matches_from_default_stream: false,
  rules: [],
  title: 'Example stream',
  categories: [],
};
/**
 *     const streams = [
 *       { key: 'One Stream', value: 'streamId1' },
 *       { key: 'another Stream', value: 'streamId2' },
 *       { key: 'Yet another Stream', value: 'streamId3' },
 *       { key: '101 Stream', value: 'streamId4' },
 *     ];
 */

export const streams: Stream[] = [
  {
    content_pack: null,
    created_at: '2020-10-10T10:10:00.000Z',
    creator_user_id: 'admin',
    description: 'Lorem ipsum dolor sit amet',
    disabled: true,
    id: 'streamId1',
    index_set_id: 'index-set-id-1',
    is_default: false,
    is_editable: true,
    matching_type: 'AND',
    outputs: [],
    remove_matches_from_default_stream: false,
    rules: [],
    title: 'One Stream',
    categories: [],
  },
  {
    content_pack: null,
    created_at: '2020-10-10T10:10:00.000Z',
    creator_user_id: 'admin',
    description: 'Lorem ipsum dolor sit amet',
    disabled: true,
    id: 'streamId2',
    index_set_id: 'index-set-id-1',
    is_default: false,
    is_editable: true,
    matching_type: 'AND',
    outputs: [],
    remove_matches_from_default_stream: false,
    rules: [],
    title: 'another Stream',
    categories: [],
  },
  {
    content_pack: null,
    created_at: '2020-10-10T10:10:00.000Z',
    creator_user_id: 'admin',
    description: 'Lorem ipsum dolor sit amet',
    disabled: true,
    id: 'streamId3',
    index_set_id: 'index-set-id-1',
    is_default: false,
    is_editable: true,
    matching_type: 'AND',
    outputs: [],
    remove_matches_from_default_stream: false,
    rules: [],
    title: 'Yet another Stream',
    categories: [],
  },
  {
    content_pack: null,
    created_at: '2020-10-10T10:10:00.000Z',
    creator_user_id: 'admin',
    description: 'Lorem ipsum dolor sit amet',
    disabled: true,
    id: 'streamId4',
    index_set_id: 'index-set-id-1',
    is_default: false,
    is_editable: true,
    matching_type: 'AND',
    outputs: [],
    remove_matches_from_default_stream: false,
    rules: [],
    title: '101 Stream',
    categories: [],
  },
];
