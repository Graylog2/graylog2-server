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

const ConfigurationWrapper = styled.div`
  > div {
    border-bottom: 1px solid grey;
    margin-bottom: 5px;

    :last-child {
      border-bottom: 0;
      margin-bottom: 0;
    }
  }
  .form-group {
    margin-bottom: 5px;
  }
`;

type Props = {
  children: React.ReactNode,
  isPermanentElement: boolean,
  onDeleteAll: () => void
  title: string,
}

const ElementConfigurationContainer = ({
  children,
  isPermanentElement,
  onDeleteAll,
  title,
}: Props) => {
  return (
    <Wrapper>
      <Header>
        <div>{title}</div>
        <div>
          {!isPermanentElement && (
            <IconButton title={`Remove ${title}`} name="trash" onClick={onDeleteAll} />
          )}
        </div>
      </Header>
      <ConfigurationWrapper>
        {children}
      </ConfigurationWrapper>
    </Wrapper>
  );
};

export default ElementConfigurationContainer;
