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

export type DurationUnit = 'seconds' | 'minutes' | 'hours' | 'days';

export type Condition = { property: string; in: Array<string> };

export type TunableSchema = {
  type?: string | Array<string>;
  title?: string;
  description?: string;
  format?: string;
  enum?: Array<string | null>;
  'x-graylog-enum-labels'?: Record<string, string>;
  minimum?: number;
  maximum?: number;
  minLength?: number;
  maxLength?: number;
  pattern?: string;
  writeOnly?: boolean;
  properties?: Record<string, TunableSchema>;
  required?: Array<string>;
  anyOf?: Array<TunableSchema>;
  'x-graylog-order'?: number;
  'x-graylog-units'?: Array<DurationUnit>;
  'x-graylog-enabled-when'?: Array<Condition>;
  'x-graylog-enabled-when-any'?: Array<Condition>;
  'x-graylog-visible-when'?: Array<Condition>;
  'x-graylog-hidden'?: boolean;
  'x-graylog-secret'?: boolean;
};

export type RuntimeTunableFieldProps = {
  name: string;
  schema: TunableSchema;
  label: string;
  help?: string;
  disabled?: boolean;
};

export type SecretValue = { is_set: boolean } | { set_value: string } | { keep_value: true } | { delete_value: true };

export type TunableValues = Record<string, unknown>;

export type RuntimeTunable = {
  id: string;
  title: string;
  description: string;
  schema: TunableSchema;
  value: TunableValues;
  defaults: TunableValues;
  isDefault: boolean;
  canEdit: boolean;
};

export type RuntimeTunableFeatureSummary = {
  id: string;
  title: string;
  tunableCount: number;
};

export type RuntimeTunableFeature = {
  id: string;
  title: string;
  tunables: Array<RuntimeTunable>;
};
