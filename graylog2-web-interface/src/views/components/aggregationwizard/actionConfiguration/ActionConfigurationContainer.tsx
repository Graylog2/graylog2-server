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

import IconButton from 'components/common/IconButton';

const Wrapper = styled.div(({ theme }) => css`
  background-color: ${theme.colors.variant.lightest.default};
  border: 1px solid ${theme.colors.variant.lighter.default};
  padding: 10px;
  border-radius: 6px;
  margin-bottom: 10px;

  :last-child {
    margin-bottom: 0;
  }
`);

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
`;

const Headline = styled.h4`
  /* font-weight: bold; */
`;

const Actions = styled.div``;

type Props = {
  children: React.ReactNode,
  isPermanentAction: boolean,
  onDeleteAll: () => void
  title: string,
}

const ActionConfigurationContainer = ({
  children,
  isPermanentAction,
  onDeleteAll,
  title,
}: Props) => {
  return (
    <Wrapper>
      <Header>
        <Headline>{title}</Headline>
        <Actions>
          {!isPermanentAction && (
            <IconButton title={`Remove ${title}`} name="trash" onClick={onDeleteAll} />
          )}
        </Actions>
      </Header>
      <div>
        {children}
      </div>
    </Wrapper>
  );
};

export default ActionConfigurationContainer;
