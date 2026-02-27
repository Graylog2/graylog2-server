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

import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
type Props = {
  id: string;
  title: string;
  type: 'pipeline' | 'rule';
};

const ConnectedPipelineLinkedCell = ({ id, title, type }: Props) => {
  const getRoute = () => {
    switch (type) {
      case 'pipeline':
        return Routes.SYSTEM.PIPELINES.PIPELINE(id);
      case 'rule':
        return Routes.SYSTEM.PIPELINES.RULE(id);
      default:
        return undefined;
    }
  };

  const route = getRoute();

  if (!route) return <>{title}</>;

  return <Link to={route}>{title}</Link>;
};
export default ConnectedPipelineLinkedCell;
