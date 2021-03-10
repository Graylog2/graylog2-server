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
import * as Immutable from 'immutable';
import styled from 'styled-components';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

const Wrapper = styled.div`
  height: 100%;
  display: flex;
`;

const Controls = styled.div`
  height: 100%;
  min-width: 300px;
  max-width: 500px;
  flex: 1;
`;

const Visualization = styled.div`
  height: 100%;
  flex: 3;
`;

export type Props = {
  children: React.ReactNode,
  config: AggregationWidgetConfig,
  fields: Immutable.List<FieldTypeMapping>,
  onChange: (AggregationWidgetConfig) => void,
};

const AggregationWizard = ({ children }: Props) => {
  return (
    <Wrapper>
      <Controls>
        The controls
      </Controls>
      <Visualization>
        {children}
      </Visualization>
    </Wrapper>
  );
};

export default AggregationWizard;
