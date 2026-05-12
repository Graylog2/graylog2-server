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

import { Link } from 'components/common';
import usePluginEntities from 'hooks/usePluginEntities';
import { getValuesFromGRN } from 'logic/permissions/GRN';
import { useGetEntityRoute } from 'routing/hooks/useShowRouteForEntity';
import ExternalLink from 'components/common/ExternalLink';

import resolveGraylogUri from './resolveGraylogUri';

const GRAYLOG_URI_PREFIX = 'graylog:///';

type Props = {
  href: string;
  children?: React.ReactNode;
};

const useGraylogRoute = (href: string): string | null => {
  const resolvers = usePluginEntities('markdown.entityLinkResolvers');
  const getEntityRoute = useGetEntityRoute();

  if (!href.startsWith(GRAYLOG_URI_PREFIX)) {
    return null;
  }

  const grn = resolveGraylogUri(href, resolvers);

  if (!grn) {
    return null;
  }

  const { id, type } = getValuesFromGRN(grn);

  return getEntityRoute(id, type);
};

const MarkdownLink = ({ href, children = null }: Props) => {
  const internalRoute = useGraylogRoute(href);

  if (internalRoute) {
    return <Link to={internalRoute}>{children}</Link>;
  }

  return <ExternalLink href={href}>{children}</ExternalLink>;
};

export default MarkdownLink;
