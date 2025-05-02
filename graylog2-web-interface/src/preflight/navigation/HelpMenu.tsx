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

import { Button, Text } from 'preflight/components/common';
import Menu from 'components/bootstrap/Menu';
import Icon from 'components/common/Icon';
import DocsHelper from 'util/DocsHelper';
import useResourceCustomization from 'brand-customization/useResourceCustomization';

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
        <StyledButton variant="default">
          <Text fw={500} size="sm" mr={3}>
            Get Help
          </Text>
          <Icon name="keyboard_arrow_down" />
        </StyledButton>
      </Menu.Target>
      <Menu.Dropdown>
        <Menu.Item
          component="a"
          rightSection={<Icon name="open_in_new" />}
          href={DocsHelper.versionedDocsHomePage()}
          target="_blank">
          Documentation
        </Menu.Item>
        <Menu.Item
          component="a"
          rightSection={<Icon name="open_in_new" />}
          href={DocsHelper.toString(DocsHelper.PAGES.CHANGELOG)}
          target="_blank">
          Changelogs
        </Menu.Item>
        <Menu.Item
          component="a"
          rightSection={<Icon name="open_in_new" />}
          href={DocsHelper.toString(DocsHelper.PAGES.OPERATIONS_CHANGELOG)}
          target="_blank">
          Operations changelogs
        </Menu.Item>
        {enabled && (
          <Menu.Item component="a" rightSection={<Icon name="open_in_new" />} href={url} target="_blank">
            Support
          </Menu.Item>
        )}
      </Menu.Dropdown>
    </Menu>
  );
};

export default HelpMenu;
