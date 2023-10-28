import * as React from 'react';
import { useState } from 'react';
import styled, { css } from 'styled-components';
import { Menu } from '@mantine/core';

import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import NavigationBrand from 'components/navigation/NavigationBrand';
import Icon from 'components/common/Icon';
import SecurityBrand from 'components/navigation/SecurityBrand';

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

  return (
    <Container>
      <Menu shadow="md" width={300} opened={showMenu} onChange={setShowMenu}>
        <LinkContainer relativeActive to={Routes.STARTPAGE}>
          <NavigationBrand />
        </LinkContainer>
        <Menu.Target><Dropdown onClick={() => setShowMenu((show) => !show)}><Icon name="caret-down" /></Dropdown></Menu.Target>
        <Menu.Dropdown>
          <Menu.Item>
            <ItemContainer>
              <NavigationBrand /><Item>Classic Graylog UI</Item>
            </ItemContainer>
          </Menu.Item>
          <Menu.Item>
            <ItemContainer>
              <SecurityBrand /><Item>Graylog Security</Item>
            </ItemContainer>
          </Menu.Item>
        </Menu.Dropdown>
      </Menu>
    </Container>
  );
};

export default PerspectiveSwitcher;
