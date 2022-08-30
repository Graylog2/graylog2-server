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
import React from 'react';

import { KeyValueTable } from 'components/common';
import type { LookupTableAdapter } from 'logic/lookup-tables/types';

import type { HTTPJSONPathAdapterConfig } from './types';

type Props = {
  dataAdapter: LookupTableAdapter & { config: HTTPJSONPathAdapterConfig },
};

const HTTPJSONPathAdapterSummary = ({ dataAdapter }: Props) => {
  const { config } = dataAdapter;

  return (
    <dl>
      <dt>Lookup URL:</dt>
      <dd>{config.url}</dd>

      <dt>Single value JSONPath:</dt>
      <dd><code>{config.single_value_jsonpath}</code></dd>

      <dt>Multi value JSONPath:</dt>
      <dd><code>{config.multi_value_jsonpath}</code></dd>

      <dt>HTTP User-Agent:</dt>
      <dd>{config.user_agent}</dd>

      <dt>HTTP Headers:</dt>
      <dd><KeyValueTable pairs={config.headers || {}} /></dd>
    </dl>
  );
};

export default HTTPJSONPathAdapterSummary;
