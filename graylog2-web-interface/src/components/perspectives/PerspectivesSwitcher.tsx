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
import { useState, useContext } from 'react';
import styled, { css } from 'styled-components';
import { Menu } from '@mantine/core';

import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import Icon from 'components/common/Icon';
import PerspectivesContext from 'components/perspectives/contexts/PerspectivesContext';
import useActivePerspective from 'components/perspectives/hooks/useActivePerspective';

const Dropdown = styled.div``;

const Container = styled.span`
  display: flex;
  flex-direction: row;
  align-content: center;
  align-items: center;
  padding: 0 15px;
`;

const ItemContainer = styled.span(({ theme }) => css`
  padding-left: 15px;
  font-size: ${theme.fonts.size.large};
  display: flex;
  flex-direction: row;
  align-content: center;
  align-items: center;
`);
const Item = styled.span`
  padding-left: 15px;
`;

const PerspectiveSwitcher = () => {
  const [showMenu, setShowMenu] = useState(false);
  const { availablePerspectives } = useContext(PerspectivesContext);
  const activePerspectiveId = useActivePerspective();
  const activePerspective = availablePerspectives.find(({ id }) => id === activePerspectiveId);
  const ActiveBrandComponent = activePerspective.brandComponent;

  return (
    <Container>
      <Menu shadow="md" width={300} opened={showMenu} onChange={setShowMenu}>
        <LinkContainer relativeActive to={Routes.STARTPAGE}>
          <ActiveBrandComponent />
        </LinkContainer>
        <Menu.Target>
          <Dropdown onClick={() => setShowMenu((show) => !show)}>
            <Icon name="caret-down" />
          </Dropdown>
        </Menu.Target>
        <Menu.Dropdown>
          {availablePerspectives.map(({ brandComponent: BrandComponent, title, id }) => (
            <Menu.Item key={id}>
              <ItemContainer>
                <BrandComponent /><Item>{title}</Item>
              </ItemContainer>
            </Menu.Item>
          ))}
        </Menu.Dropdown>
      </Menu>
    </Container>
  );
};

export default PerspectiveSwitcher;
