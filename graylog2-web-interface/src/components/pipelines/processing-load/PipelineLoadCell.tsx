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
import { useTranslation } from 'react-i18next';

import { Tooltip } from 'components/common';

type Props = {
  loadPercent: number | undefined;
  error?: boolean;
};

const formatLoadPercent = (loadPercent: number) => `${loadPercent.toFixed(2)}%`;

const PipelineLoadCell = ({ loadPercent, error = false }: Props) => {
  const { t } = useTranslation('pipelines');

  if (error) {
    const errorTooltip = t('load_cell.error_tooltip');

    return (
      <Tooltip label={errorTooltip}>
        <span aria-label={errorTooltip}>—</span>
      </Tooltip>
    );
  }

  if (loadPercent === undefined) {
    return null;
  }

  return <span>{formatLoadPercent(loadPercent)}</span>;
};

export default PipelineLoadCell;
