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

import type { Token } from 'components/users/UsersTokenManagement/hooks/useTokens';
import { Label } from 'components/bootstrap';

type Props = {
  token: Token;
};

const Wrapper = styled.div<{ $enabled: boolean }>(
  ({ theme, $enabled }) => css`
    color: ${$enabled ? theme.colors.variant.success : theme.colors.variant.default};
    width: fit-content;
  `,
);

const IsExternalUserCell = ({ token }: Props) => {
  const isExternal = token?.external_user;

  return (
    <Wrapper $enabled={isExternal}>
      <Label bsStyle={isExternal ? 'success' : 'default'} >{isExternal? 'Yes': 'No'}</Label>
    </Wrapper>
  );
};

export default IsExternalUserCell;
