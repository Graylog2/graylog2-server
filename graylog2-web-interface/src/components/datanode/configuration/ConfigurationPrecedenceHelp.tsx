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

import ProductName from 'brand-customization/ProductName';
import HelpText, { HelpTextList } from 'components/datanode/HelpText';

const Container = styled(HelpText)(
  ({ theme }) => css`
    margin: 0 0 ${theme.spacings.md};
  `,
);

const PrecedenceList = styled(HelpTextList)`
  list-style-type: decimal;
`;

const ConfigurationPrecedenceHelp = () => (
  <Container>
    <ProductName /> and Data Node configuration can be set in a number of locations. To prevent conflicts, upon startup
    the services always use the following order of precedence when choosing which value is used:
    <PrecedenceList as="ol">
      <li>Configuration management in the UI (First).</li>
      <li>Environment variables.</li>
      <li>System properties.</li>
      <li>Configuration files.</li>
      <li>The built-in defaults (Last).</li>
    </PrecedenceList>
  </Container>
);

export default ConfigurationPrecedenceHelp;
