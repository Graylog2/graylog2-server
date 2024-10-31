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

import { Radio } from 'components/bootstrap';
import { strategiesLabels } from 'views/logic/valueactions/createEventDefinition/Constants';
import type { StrategyId } from 'views/logic/valueactions/createEventDefinition/types';

const Container = styled.div`
  display: flex;
  gap: 5px;
  align-items: baseline;
`;

type Props = {
  strategy: StrategyId,
  onChange: (e: React.FormEvent<Radio>) => void,
  strategyAvailabilities: {[name in StrategyId]: boolean}
}

const RadioSection = ({ strategy, onChange, strategyAvailabilities }: Props) => {
  const { description } = strategiesLabels[strategy];

  return (
    <>
      <b>Strategy by:</b>
      <Container>
        {
      Object.entries(strategiesLabels).map(([curStrategy, { label }]) => {
        if (!strategyAvailabilities[curStrategy]) return null;

        return (
          <Radio id={curStrategy}
                 key={curStrategy}
                 name="strategy"
                 value={curStrategy}
                 checked={curStrategy === strategy}
                 onChange={onChange}>
            {label}
          </Radio>
        );
      })
    }
      </Container>
      <p>
        <b>Description: </b><i>{description}</i>
      </p>
    </>
  );
};

export default RadioSection;
