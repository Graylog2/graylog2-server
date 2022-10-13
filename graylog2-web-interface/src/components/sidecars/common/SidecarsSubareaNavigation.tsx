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

import SubareaNavigation from 'components/common/SubareaNavigation';
import Routes from 'routing/Routes';
import { Row } from 'components/bootstrap';

const NAV_ITEMS = [
  { title: 'Overview', path: Routes.SYSTEM.SIDECARS.OVERVIEW, exactPathMatch: true },
  { title: 'Administration', path: Routes.SYSTEM.SIDECARS.ADMINISTRATION },
  { title: 'Configuration', path: Routes.SYSTEM.SIDECARS.CONFIGURATION },
];

const SidecarsSubareaNavigation = () => (
  <Row>
    <SubareaNavigation items={NAV_ITEMS} />
  </Row>
);

export default SidecarsSubareaNavigation;
