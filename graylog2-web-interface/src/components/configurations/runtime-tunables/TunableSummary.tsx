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
import isEqual from 'lodash/isEqual';
import styled, { css } from 'styled-components';

import type { RuntimeTunable } from './types';
import formatTunableValue from './formatTunableValue';
import { isVisible, orderedProperties } from './schema';

const DefinitionList = styled.dl(
  ({ theme }) => css`
    display: grid;
    grid-template-columns: max-content 1fr;
    column-gap: ${theme.spacings.lg};
    margin-top: ${theme.spacings.sm};
  `,
);

const Term = styled.dt`
  font-weight: bold;
`;

const Description = styled.dd`
  margin: 0;
`;

const Modified = styled.span(
  ({ theme }) => css`
    margin-left: ${theme.spacings.xs};
    color: ${theme.colors.text.secondary};
    font-size: ${theme.fonts.size.small};
  `,
);

type Props = {
  tunable: RuntimeTunable;
};

const TunableSummary = ({ tunable }: Props) => (
  <DefinitionList>
    {orderedProperties(tunable.schema)
      .filter(([, property]) => isVisible(property, tunable.value))
      .map(([name, property]) => {
        const label = property.title ?? name;

        return (
          <React.Fragment key={name}>
            <Term>{label}:</Term>
            <Description aria-label={label}>
              {formatTunableValue(property, tunable.value?.[name])}
              {!isEqual(tunable.value?.[name], tunable.defaults?.[name]) && <Modified>(modified)</Modified>}
            </Description>
          </React.Fragment>
        );
      })}
  </DefinitionList>
);

export default TunableSummary;
