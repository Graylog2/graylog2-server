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

import { NavDropdown } from 'components/bootstrap';
import { Icon } from 'components/common';
import AppConfig from 'util/AppConfig';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';
import Menu from 'components/bootstrap/Menu';

const HelpMenuItem = ({ href, children }: React.PropsWithChildren<{ href: string }>) => (
  <Menu.Item component="a" href={href} target="_blank" icon={<Icon name="external-link-alt" />}>
    {children}
  </Menu.Item>
);
const HelpMenu = () => (
  <NavDropdown title={<Icon name="question-circle" size="lg" />}
               aria-label="Help"
               noCaret>

    <HelpMenuItem href={DocsHelper.versionedDocsHomePage()}>
      Documentation
    </HelpMenuItem>

    {AppConfig.isCloud() && (
    <HelpMenuItem href={Routes.global_api_browser()}>
      Cluster Global API browser
    </HelpMenuItem>
    )}
  </NavDropdown>
);

export default HelpMenu;
