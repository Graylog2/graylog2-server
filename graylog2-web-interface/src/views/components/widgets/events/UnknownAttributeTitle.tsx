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
import styled, { css } from 'styled-components';

import { OverlayTrigger, Icon } from 'components/common';

const UnknownFieldTitleContainer = styled.div`
  display: flex;
  gap: 5px;
`;

const ErrorIcon = styled(Icon)(({ theme }) => css`
  color: ${theme.colors.variant.warning};
`);

const UnknownAttributeTitle = () => (
  <UnknownFieldTitleContainer>
    Unknown
    <OverlayTrigger overlay="This attribute is currently not available, because it requires a valid license."
                    placement="bottom">
      <ErrorIcon name="error" />
    </OverlayTrigger>
  </UnknownFieldTitleContainer>
);

export default UnknownAttributeTitle;
