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

import { Button, Panel } from 'components/bootstrap';
import { Icon } from 'components/common';

import AggregationConditionSummary from './AggregationConditionSummary';

const StyledPanel = styled(Panel)`
  margin-top: 10px;
`;

const StyledButton = styled(Button)`
  margin-left: 15px;
  vertical-align: baseline;
`;

type AggregationConditionsFormSummaryProps = {
  conditions: any;
  series: any[];
  expressionValidation?: any;
  showInlineValidation?: boolean;
  toggleShowValidation: (...args: any[]) => void;
};

const AggregationConditionsFormSummary = ({
  conditions,
  series,
  expressionValidation = { isValid: true },
  showInlineValidation = false,
  toggleShowValidation,
}: AggregationConditionsFormSummaryProps) => {
  const { t } = useTranslation('event_definitions');

  return (
    <div>
      <StyledPanel header={t('condition_summary.title')}>
        {expressionValidation.isValid ? (
          <p className="text-success">
            <Icon name="check_box" />
            &nbsp;{t('condition_summary.valid')}
          </p>
        ) : (
          <p className="text-danger">
            <Icon name="warning" />
            &nbsp;{t('condition_summary.invalid')}
            <StyledButton bsSize="xsmall" onClick={toggleShowValidation}>
              {showInlineValidation ? t('condition_summary.hide_errors') : t('condition_summary.show_errors')}
            </StyledButton>
          </p>
        )}
        <b>{t('condition_summary.preview')}</b> <AggregationConditionSummary series={series} conditions={conditions} />
      </StyledPanel>
    </div>
  );
};

export default AggregationConditionsFormSummary;
