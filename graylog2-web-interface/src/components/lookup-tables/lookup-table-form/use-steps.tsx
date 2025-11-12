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

import { CacheFormStep } from 'components/lookup-tables/cache-form';
import { DataAdapterFormStep } from 'components/lookup-tables/adapter-form';
import LookupTableFormFields from 'components/lookup-tables/LookupTableFormFields';

import SummaryStep from './summary-step';

function useSteps(
  activeStepKey: string = 'lookup-tables',
): [Array<any>, { activeStep: string; setActiveStep: (newStep: string) => void }] {
  const [activeStep, setActiveStep] = React.useState(activeStepKey);

  const steps = [
    {
      key: 'lookup-table',
      title: 'Lookup Table',
      component: (
        <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
          <LookupTableFormFields />
        </div>
      ),
    },
    {
      key: 'cache',
      title: 'Cache',
      component: <CacheFormStep />,
    },
    {
      key: 'data-adapter',
      title: 'Data Adapter',
      component: <DataAdapterFormStep />,
    },
    {
      key: 'summary',
      title: 'Summary',
      component: <SummaryStep />,
    },
  ];

  return [steps, { activeStep, setActiveStep }];
}

export default useSteps;
