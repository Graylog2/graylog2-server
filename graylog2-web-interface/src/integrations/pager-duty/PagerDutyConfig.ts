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
export type PagerDutyConfig = {
  routing_key: string;
  pager_duty_title?: string;
  custom_incident?: boolean;
  key_prefix?: string;
  incident_key?: string;
  client_name: string;
  client_url: string;
};

export const defaultConfig = {
  client_name: '',
  client_url: '',
  custom_incident: false,
  key_prefix: '',
  routing_key: '',
  pager_duty_title: null,
  incident_key: null,
};
