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
import { useTranslation } from 'react-i18next';
import styled from 'styled-components';

import { Radio } from 'components/common';
import { strategiesLabels } from 'views/logic/valueactions/createEventDefinition/Constants';
import type { StrategyId } from 'views/logic/valueactions/createEventDefinition/types';

const Container = styled.div`
  display: flex;
  gap: 5px;
  align-items: baseline;
`;

type Props = {
  strategy: StrategyId;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  strategyAvailabilities: { [name in StrategyId]: boolean };
};

const RadioSection = ({ strategy, onChange, strategyAvailabilities }: Props) => {
  const { t } = useTranslation('search');
  const { description } = strategiesLabels[strategy];

  return (
    <>
      <b>{t('create_event_definition.strategy_by')}</b>
      <Container>
        {Object.entries(strategiesLabels).map(([curStrategy, { label }]) => {
          if (!strategyAvailabilities[curStrategy]) return null;

          return (
            <Radio
              id={curStrategy}
              key={curStrategy}
              name="strategy"
              value={curStrategy}
              checked={curStrategy === strategy}
              onChange={onChange}>
              {t(label)}
            </Radio>
          );
        })}
      </Container>
      <p>
        <b>{t('create_event_definition.description')} </b>
        <i>{t(description)}</i>
      </p>
    </>
  );
};

export default RadioSection;
