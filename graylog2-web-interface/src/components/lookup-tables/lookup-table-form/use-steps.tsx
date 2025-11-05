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

import { CacheCreate, DataAdapterCreate } from 'components/lookup-tables';
import LookupTableFormFields from 'components/lookup-tables/LookupTableFormFields';
import type { LookupTableType } from 'components/lookup-tables/LookupTableForm';

type Props = {
  table: LookupTableType;
  activeStepKey?: string;
};

function useSteps({
  activeStepKey = 'lookup-tables',
}: Props): [Array<any>, { activeStep: string; setActiveStep: (newStep: string) => void }] {
  const [activeStep, setActiveStep] = React.useState(activeStepKey);

  const onClose = () => {};

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
      component: <CacheCreate saved={onClose} onCancel={onClose} validationErrors={{}} />,
    },
    {
      key: 'data-adapter',
      title: 'Data Adapter',
      component: <DataAdapterCreate saved={onClose} onCancel={onClose} validationErrors={{}} />,
    },
  ];

  return [steps, { activeStep, setActiveStep }];
}

export default useSteps;
