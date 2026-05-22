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

import HealthStatusIcon from './HealthStatusIcon';
import { STATUS_DESCRIPTION, STATUS_LABELS, STATUS_ORDER } from './healthStatusCopy';
import {
  BodyText,
  InterpretationPane,
  InterpretationTitle,
  LegendItem,
  LegendList,
  LegendText,
} from './HealthModule.styles';

const HealthInterpretationLegend = () => (
  <InterpretationPane>
    <InterpretationTitle>How to interpret this health report:</InterpretationTitle>
    <BodyText>
      This health report groups checks by subsystem. Select any branch or check on the left to review its status,
      affected entities, and recommended next steps. Every check resolves to one of four states:
    </BodyText>
    <LegendList>
      {STATUS_ORDER.map((status) => (
        <LegendItem key={status}>
          <HealthStatusIcon status={status} title={STATUS_LABELS[status]} />
          <LegendText>
            <strong>{STATUS_LABELS[status]}:</strong> {STATUS_DESCRIPTION[status]}
          </LegendText>
        </LegendItem>
      ))}
    </LegendList>
  </InterpretationPane>
);

export default HealthInterpretationLegend;
