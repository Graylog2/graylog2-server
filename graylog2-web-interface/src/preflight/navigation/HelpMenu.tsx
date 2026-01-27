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

import { Text } from 'preflight/components/common';
import Button from 'components/bootstrap/Button';
import Menu from 'components/bootstrap/Menu';
import Icon from 'components/common/Icon';
import DocsHelper from 'util/DocsHelper';
import useResourceCustomization from 'brand-customization/useResourceCustomization';
import { MenuItem } from 'components/bootstrap';

const StyledButton = styled(Button)(
  ({ theme }) => css`
    border-radius: 50px;
    padding: 0 13px;
    background-color: ${theme.colors.variant.lightest.default};
  `,
);

const HelpMenu = () => {
  const { enabled, url } = useResourceCustomization('contact_support');

  return (
    <Menu width={250} position="bottom-end">
      <Menu.Target>
        <StyledButton>
          <Text fw={500} size="sm" mr={3}>
            Get Help
          </Text>
          <Icon name="keyboard_arrow_down" />
        </StyledButton>
      </Menu.Target>
      <Menu.Dropdown>
        <MenuItem component="a" href={DocsHelper.versionedDocsHomePage()} target="_blank" icon="open_in_new">
          Documentation
        </MenuItem>
        <MenuItem
          component="a"
          href={DocsHelper.toString(DocsHelper.PAGES.CHANGELOG)}
          target="_blank"
          icon="open_in_new">
          Changelogs
        </MenuItem>
        <MenuItem
          component="a"
          href={DocsHelper.toString(DocsHelper.PAGES.OPERATIONS_CHANGELOG)}
          target="_blank"
          icon="open_in_new">
          Operations changelogs
        </MenuItem>
        {enabled && (
          <MenuItem component="a" href={url} target="_blank" icon="open_in_new">
            Support
          </MenuItem>
        )}
      </Menu.Dropdown>
    </Menu>
  );
};

export default HelpMenu;
