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
export type Stream = {
  id: string;
  creator_user_id: string;
  outputs: any[];
  matching_type: 'AND' | 'OR';
  description: string;
  created_at: string;
  disabled: boolean;
  rules: StreamRule[];
  alert_conditions?: any[];
  alert_receivers?: {
    emails: Array<string>;
    users: Array<string>;
  };
  title: string;
  content_pack: any;
  remove_matches_from_default_stream: boolean;
  index_set_id: string;
  is_default: boolean;
  is_editable: boolean;
  categories: string[];
  favorite_fields?: Array<string>;
};

export type StreamRule = {
  id: string;
  type: number;
  value: string;
  field: string;
  inverted: boolean;
  stream_id: string;
  description: string;
};

export type StreamRuleType = {
  id: number;
  short_desc: string;
  long_desc: string;
  name: string;
};

type OutputSummary = {
  id: string;
  title: string;
  type: string;
  creator_user_id: string;
  created_at: string;
  configuration: { [key: string]: string };
};

type AlertConditionSummary = {
  id: string;
  type: string;
  creator_user_id: string;
  created_at: string;
  parameters: { [key: string]: any };
  in_grace: boolean | null | undefined;
  title: string | null | undefined;
};

export type StreamConfiguration = Pick<
  Stream,
  | 'index_set_id'
  | 'title'
  | 'matching_type'
  | 'remove_matches_from_default_stream'
  | 'description'
  | 'rules'
  | 'content_pack'
>;

type AlertReceiver = {
  emails: string[];
  users: string[];
};

export type StreamResponse = {
  id: string;
  creator_user_id: string;
  outputs: OutputSummary[];
  matching_type: string;
  description: string;
  created_at: string;
  disabled: boolean;
  rules: StreamRule[];
  alert_conditions: AlertConditionSummary[];
  alert_receivers: AlertReceiver;
  title: string;
  is_default: boolean | null | undefined;
  is_editable: boolean;
  remove_matches_from_default_stream: boolean;
  index_set_id: string;
  categories: string[];
};

export type MatchData = {
  matches: boolean;
  rules: { [id: string]: false };
};
