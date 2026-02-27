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
import styled, { css } from 'styled-components';

import TypeSpecificValue from 'views/components/TypeSpecificValue';
import useQueryFieldTypes from 'views/hooks/useQueryFieldTypes';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';

const ValueBox = styled.span<{ $bgColor: string | number }>(
  ({ theme, $bgColor }) => css`
    background-color: ${$bgColor ?? 'inherit'};
    color: ${$bgColor ? theme.utils.contrastingColor(String($bgColor)) : 'inherit'};
    padding: ${theme.spacings.xxs};
  `,
);

const Container = styled.span(
  ({ theme }) => css`
    display: inline-flex;
    align-items: center;
    gap: ${theme.spacings.xxs};
    font-size: ${theme.fonts.size.tiny};
  `,
);

const ValueRenderer = ({ value, field, traceColor }) => {
  const types = useQueryFieldTypes();
  const fieldType = fieldTypeFor(field, types);

  return (
    <Container>
      <ValueBox $bgColor={traceColor}>
        <TypeSpecificValue field={field} value={value} type={fieldType} />
      </ValueBox>
      <span>{field}</span>
    </Container>
  );
};

export default ValueRenderer;
