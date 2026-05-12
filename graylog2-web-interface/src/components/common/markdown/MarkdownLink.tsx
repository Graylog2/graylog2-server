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
import { useGetEntityRoute } from 'routing/hooks/useShowRouteForEntity';
import ExternalLink from 'components/common/ExternalLink';

import resolveGraylogUri from './resolveGraylogUri';

const GRAYLOG_URI_PREFIX = 'graylog:///';

type Props = {
  href: string;
  children?: React.ReactNode;
};

type ResolvedLink = { to: string } | { onClick: () => void };

const useResolvedGraylogLink = (href: string): ResolvedLink | null => {
  const resolvers = usePluginEntities('markdown.entityLinkResolvers');
  const getEntityRoute = useGetEntityRoute();

  if (!href.startsWith(GRAYLOG_URI_PREFIX)) {
    return null;
  }

  const resolved = resolveGraylogUri(href, resolvers);

  if (!resolved) {
    return null;
  }

  if ('onClick' in resolved) {
    return { onClick: resolved.onClick };
  }

  return { to: getEntityRoute(resolved.id, resolved.grnType) };
};

const MarkdownLink = ({ href, children = null }: Props) => {
  const resolved = useResolvedGraylogLink(href);

  if (resolved && 'to' in resolved) {
    return <Link to={resolved.to}>{children}</Link>;
  }

  if (resolved && 'onClick' in resolved) {
    return (
      <a
        href={href}
        onClick={(event) => {
          event.preventDefault();
          resolved.onClick();
        }}>
        {children}
      </a>
    );
  }

  return <ExternalLink href={href}>{children}</ExternalLink>;
};

export default MarkdownLink;
