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

type WhoisAdapterSummaryProps = {
  dataAdapter: {
    config?: {
      connect_timeout: number;
      read_timeout: number;
    };
  };
};

const WhoisAdapterSummary = ({
  dataAdapter,
}: WhoisAdapterSummaryProps) => {
  const { config } = dataAdapter;

  return (
    <dl>
      <dt>Connect timeout</dt>
      <dd>{config.connect_timeout} ms</dd>
      <dt>Read timeout</dt>
      <dd>{config.read_timeout} ms</dd>
    </dl>
  );
};

export default WhoisAdapterSummary;
