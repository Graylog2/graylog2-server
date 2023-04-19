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

import { Button, Icon, Menu, MenuTarget, MenuItem, MenuDropdownWrapper, Text } from 'preflight/components/common';

const StyledButton = styled(Button)(({ theme }) => css`
  border-radius: 50px;
  padding: 0 13px;
  background-color: ${theme.colors.variant.lightest.default};
`);

const HelpMenu = () => {
  return (
    <Menu width={250}
          position="bottom-end">
      <MenuTarget>
        <StyledButton variant="default">
          <Text weight={500} size="sm" mr={3}>Get Help</Text>
          <Icon name="chevron-down" />
        </StyledButton>
      </MenuTarget>
      <MenuDropdownWrapper>
        <MenuItem component="a"
                  rightSection={<Icon name="arrow-up-right-from-square" />}
                  href="https://docs.graylog.org/docs"
                  target="_blank">
          Graylog Documentation
        </MenuItem>
        <MenuItem component="a"
                  rightSection={<Icon name="arrow-up-right-from-square" />}
                  href="https://docs.graylog.org/docs/changelog"
                  target="_blank">
          Graylog changelogs
        </MenuItem>
        <MenuItem component="a"
                  rightSection={<Icon name="arrow-up-right-from-square" />}
                  href="https://docs.graylog.org/docs/changelog-graylog"
                  target="_blank">
          Graylog Operations changelogs
        </MenuItem>
        <MenuItem component="a"
                  rightSection={<Icon name="arrow-up-right-from-square" />}
                  href="https://support.graylog.org/portal"
                  target="_blank">
          Support
        </MenuItem>
      </MenuDropdownWrapper>
    </Menu>
  );
};

export default HelpMenu;
