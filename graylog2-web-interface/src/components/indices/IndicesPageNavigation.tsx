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

import AppConfig from 'util/AppConfig';
import PageNavigation from 'components/common/PageNavigation';
import Routes from 'routing/Routes';
import { Row } from 'components/bootstrap';

const PREM_ONLY_NAV_ITEMS = [
  { title: 'Index Set Templates', path: Routes.SYSTEM.INDICES.TEMPLATES.OVERVIEW, exactPathMatch: false },
];

const NAV_ITEMS = [
  { title: 'Indices & Index Sets', path: Routes.SYSTEM.INDICES.LIST, exactPathMatch: true },
  { title: 'Field Type Profiles', path: Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.OVERVIEW, exactPathMatch: false },
];

const IndicesPageNavigation = () => {
  const navItems = AppConfig.isCloud() ? NAV_ITEMS : [...NAV_ITEMS, ...PREM_ONLY_NAV_ITEMS];

  return (

    <Row>
      <PageNavigation items={navItems} />
    </Row>
  );
};

export default IndicesPageNavigation;
