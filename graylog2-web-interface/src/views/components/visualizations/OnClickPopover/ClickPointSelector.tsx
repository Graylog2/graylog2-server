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

import { ListGroup, ListGroupItem } from 'components/bootstrap';
import type { ClickPoint } from 'views/components/visualizations/OnClickPopover/Types';
import ValueRenderer from 'views/components/visualizations/OnClickPopover/ValueRenderer';
import getHoverSwatchColor from 'views/components/visualizations/utils/getHoverSwatchColor';

const StyledListGroup = styled(ListGroup)`
  max-height: 300px;
  overflow-y: auto;
`;

type Props = {
  clickPointsInRadius: Array<ClickPoint>;
  onSelect: (point: ClickPoint) => void;
  metricMapper: (clickPoint: ClickPoint) => { value: string; metric: string };
};

const ClickPointSelector = ({ clickPointsInRadius, onSelect, metricMapper }: Props) => (
  <StyledListGroup>
    {clickPointsInRadius.map((clickPoint: ClickPoint) => {
      const { value, metric } = metricMapper(clickPoint);
      const traceColor = getHoverSwatchColor(clickPoint);

      return (
        <ListGroupItem onClick={() => onSelect(clickPoint)}>
          <ValueRenderer value={value} label={metric} traceColor={traceColor} />
        </ListGroupItem>
      );
    })}
  </StyledListGroup>
);

export default ClickPointSelector;
