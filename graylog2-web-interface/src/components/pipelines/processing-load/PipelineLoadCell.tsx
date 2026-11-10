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

import { Tooltip } from 'components/common';

type Props = {
  loadPercent: number | undefined;
  error?: boolean;
};

const ERROR_TOOLTIP = 'Pipeline Load is unavailable: failed to load processing-load metrics.';

const formatLoadPercent = (loadPercent: number) => `${loadPercent.toFixed(2)}%`;

const PipelineLoadCell = ({ loadPercent, error = false }: Props) => {
  if (error) {
    return (
      <Tooltip label={ERROR_TOOLTIP}>
        <span aria-label={ERROR_TOOLTIP}>—</span>
      </Tooltip>
    );
  }

  if (loadPercent === undefined) {
    return null;
  }

  return <span>{formatLoadPercent(loadPercent)}</span>;
};

export default PipelineLoadCell;
