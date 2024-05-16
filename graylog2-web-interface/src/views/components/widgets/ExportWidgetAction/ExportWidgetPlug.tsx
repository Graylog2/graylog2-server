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

import { IconButton, OverlayTrigger } from 'components/common';

const title = 'Export widget';
const Explanation = () => (
  <span>Export aggregation widget feature is available for the enterprise version.
    Graylog provides options to export your data into most popular file formats such as
    CSV, JSON, YAML, XML etc.
  </span>
);

const ExportWidgetPlug = () => (
  <OverlayTrigger trigger="click" title={title} overlay={<Explanation />} placement="bottom">
    <IconButton name="download" title={title} />
  </OverlayTrigger>
);

export default ExportWidgetPlug;
