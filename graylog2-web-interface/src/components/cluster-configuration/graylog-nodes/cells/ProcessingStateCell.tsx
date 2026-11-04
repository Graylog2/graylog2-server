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

import { Label } from 'components/bootstrap';

import type { ClusterGraylogNode } from '../fetchClusterGraylogNodes';
import { MetricsColumn, MetricsRow } from '../../shared-components/NodeMetricsLayout';

type Props = {
  node: ClusterGraylogNode;
};

const ProcessingStateCell = ({ node }: Props) => {
  const { t } = useTranslation('system');

  if (node.is_processing == null) {
    return null;
  }

  const status = node.is_processing ? t('processing_state_cell.enabled') : t('processing_state_cell.disabled');

  return (
    <MetricsColumn>
      <MetricsRow>
        <Label
          bsStyle={node.is_processing ? 'success' : 'warning'}
          bsSize="xs"
          aria-label={t('processing_state_cell.aria_label', { status })}>
          {status}
        </Label>
      </MetricsRow>
    </MetricsColumn>
  );
};

export default ProcessingStateCell;
