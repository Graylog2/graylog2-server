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

import { Badge } from 'components/bootstrap';
import type { BadgeColor } from 'components/bootstrap/Badge';
import { Link } from 'components/common';
import Routes from 'routing/Routes';

const BADGE_COLOR_BY_STATUS: Record<string, BadgeColor> = {
  installed: 'success',
  updatable: 'primary',
  edited: 'warning',
  error: 'danger',
};

type ContentPackStatusProps = {
  states?: string[];
  contentPackId?: string;
};

const ContentPackStatus = ({ contentPackId = undefined, states = [] }: ContentPackStatusProps) => {
  const badges = states.map((state) => (
    <Link key={state} to={Routes.SYSTEM.CONTENTPACKS.show(contentPackId)}>
      <Badge color={BADGE_COLOR_BY_STATUS[state] ?? 'gray'} variant="light" style={{ marginLeft: 4 }}>
        {state}
      </Badge>
    </Link>
  ));

  return <span>{badges}</span>;
};

export default ContentPackStatus;
