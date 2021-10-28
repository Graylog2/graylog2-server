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
import { useCallback } from 'react';
import styled from 'styled-components';
import { useFormikContext } from 'formik';

import OnVisualizationConfigChangeContext from 'views/components/aggregationwizard/OnVisualizationConfigChangeContext';

import type { WidgetConfigFormValues } from './WidgetConfigForm';

const Container = styled.div`
  height: 100%;
  flex: 3;
`;

type Props = {
  children: React.ReactElement
}

const VisualizationContainer = ({ children }: Props) => {
  const { setFieldValue, values } = useFormikContext<WidgetConfigFormValues>();

  const onVisualizationConfigChange = useCallback((newVisualizationConfig) => {
    setFieldValue('visualization', { ...values.visualization, config: { ...values.visualization.config, ...newVisualizationConfig } });
  }, [values.visualization, setFieldValue]);

  return (
    <Container>
      <OnVisualizationConfigChangeContext.Provider value={onVisualizationConfigChange}>
        {children}
      </OnVisualizationConfigChangeContext.Provider>
    </Container>
  );
};

export default VisualizationContainer;
