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
import { useCallback } from 'react';

import Menu from 'components/bootstrap/Menu';
import Icon from 'components/common/Icon';
import usePerspectives from 'components/perspectives/hooks/usePerspectives';
import useActivePerspective from 'components/perspectives/hooks/useActivePerspective';
import useHistory from 'routing/useHistory';

import ActivePerspectiveBrand from './ActivePerspectiveBrand';

const CONTAINER_CLASS = 'navbar-brand';

const Container = styled.span`
  display: flex;
  flex-direction: row;
  align-content: center;
  align-items: center;
  padding-right: 0;
`;

const ItemContainer = styled.span(({ theme }) => css`
  font-size: ${theme.fonts.size.large};
  display: flex;
  flex-direction: row;
  align-content: center;
  align-items: center;
`);

const DropdownTrigger = styled.button`
  background: transparent;
  border: 0;
`;

const StyledMenuDropdown = styled(Menu.Dropdown)`
  z-index: 1032 !important;
`;

const DropdownIcon = styled(Icon)(({ theme }) => css`
  color: ${theme.colors.global.textDefault};
`);

const Switcher = () => {
  const { setActivePerspective } = useActivePerspective();
  const perspectives = usePerspectives();
  const history = useHistory();

  const onChangePerspective = useCallback((nextPerspectiveId: string) => () => {
    const { welcomeRoute } = perspectives.find(({ id }) => id === nextPerspectiveId);

    history.push(welcomeRoute);
    setActivePerspective(nextPerspectiveId);
  }, [history, perspectives, setActivePerspective]);

  return (
    <Container className={CONTAINER_CLASS}>
      <Menu shadow="md" withinPortal>
        <ActivePerspectiveBrand>
          <Menu.Target>
            <DropdownTrigger type="button" title="Change UI perspective">
              <DropdownIcon name="arrow_drop_down" />
            </DropdownTrigger>
          </Menu.Target>
        </ActivePerspectiveBrand>
        <StyledMenuDropdown>
          {perspectives.map(({ title, id }) => (
            <Menu.Item key={id} onClick={onChangePerspective(id)}>
              <ItemContainer>{title}</ItemContainer>
            </Menu.Item>
          ))}
        </StyledMenuDropdown>
      </Menu>
    </Container>
  );
};

const PerspectivesSwitcher = () => {
  const perspectives = usePerspectives();

  if (perspectives.length === 1) {
    return (
      <ActivePerspectiveBrand className="navbar-brand" />
    );
  }

  return <Switcher />;
};

export default PerspectivesSwitcher;
