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
import styled from 'styled-components';
import { useTranslation } from 'react-i18next';

import StringUtils from 'util/StringUtils';

const NodeState = styled.dl`
  margin-top: 0;
  margin-bottom: 0;

  dt {
    float: left;
  }

  dd {
    margin-left: 180px;
  }
`;

type SystemOverviewSummaryProps = {
  information: any;
};

export const SystemOverviewSummary = ({ information }: SystemOverviewSummaryProps) => {
  const { t } = useTranslation('system');
  const lbStatus = information.lb_status.toUpperCase();

  return (
    <NodeState>
      <dt>{t('system_overview_summary.current_lifecycle_state')}</dt>
      <dd>{StringUtils.capitalizeFirstLetter(information.lifecycle)}</dd>
      <dt>{t('system_overview_summary.message_processing')}</dt>
      <dd>
        {information.is_processing ? t('system_overview_summary.enabled') : t('system_overview_summary.disabled')}
      </dd>
      <dt>{t('system_overview_summary.lb_indication')}</dt>
      <dd className={lbStatus === 'DEAD' ? 'text-danger' : ''}>{lbStatus}</dd>
    </NodeState>
  );
};

export default SystemOverviewSummary;
