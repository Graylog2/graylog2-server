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
import * as React from 'react';
import { useState } from 'react';
import styled from 'styled-components';
import { useTranslation } from 'react-i18next';

import LookupTableFormFields from './lookup-table-form-fields';
import DataAdapterFormStep from './adapter-step';
import CacheFormStep from './cache-step';
import SummaryStep from './summary-step';

const CenteredDiv = styled.div`
  max-width: 1250px;
  margin: 0 auto;
`;

function useSteps(
  activeStepKey: string = 'lookup-table',
): [Array<any>, { activeStep: string; setActiveStep: (newStep: string) => void }] {
  const { t } = useTranslation('lookup_tables');
  const [activeStep, setActiveStep] = useState(activeStepKey);

  const steps = [
    {
      key: 'lookup-table',
      title: t('lookup_table_form.steps.lookup_table'),
      component: (
        <CenteredDiv>
          <LookupTableFormFields />
        </CenteredDiv>
      ),
    },
    {
      key: 'cache',
      title: t('lookup_table_form.steps.cache'),
      component: <CacheFormStep />,
    },
    {
      key: 'data-adapter',
      title: t('lookup_table_form.steps.data_adapter'),
      component: <DataAdapterFormStep />,
    },
    {
      key: 'summary',
      title: t('lookup_table_form.steps.summary'),
      component: <SummaryStep />,
    },
  ];

  return [steps, { activeStep, setActiveStep }];
}

export default useSteps;
