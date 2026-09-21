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
import styled from 'styled-components';

import { Link } from 'components/common';
import usePluginEntities from 'hooks/usePluginEntities';
import { useGetEntityRoute } from 'routing/hooks/useShowRouteForEntity';
import ExternalLink from 'components/common/ExternalLink';
import useRightSidebar from 'hooks/useRightSidebar';

import resolveGraylogUri from './resolveGraylogUri';
import type { EntityLinkResolution } from './resolveGraylogUri';

const GRAYLOG_URI_PREFIX = 'graylog:///';

type Props = {
  href: string;
  children?: React.ReactNode;
};

type OnClickHandler = Extract<EntityLinkResolution, { onClick: unknown }>['onClick'];
type ResolvedLink = { to: string } | { onClick: OnClickHandler };

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

// No `href`: the target is a `graylog:///` URI the browser cannot resolve, so exposing it would
// offer a broken "open in new tab" and show the internal URI on hover. `role`, `tabIndex` and the
// key handler keep the link reachable without one, and `cursor` restores what `a[href]` supplied.
const SidebarLink = styled.a`
  cursor: pointer;
`;

const OnClickLink = ({ handler, children }: { handler: OnClickHandler; children: React.ReactNode }) => {
  const { openSidebar } = useRightSidebar();
  const openInSidebar = () => handler({ openSidebar });

  return (
    <SidebarLink
      role="link"
      tabIndex={0}
      onClick={openInSidebar}
      onKeyDown={(event: React.KeyboardEvent) => {
        if (event.key === 'Enter') {
          openInSidebar();
        }
      }}>
      {children}
    </SidebarLink>
  );
};

const MarkdownLink = ({ href, children = null }: Props) => {
  const resolved = useResolvedGraylogLink(href);

  if (resolved && 'to' in resolved) {
    return <Link to={resolved.to}>{children}</Link>;
  }

  if (resolved && 'onClick' in resolved) {
    return <OnClickLink handler={resolved.onClick}>{children}</OnClickLink>;
  }

  return <ExternalLink href={href}>{children}</ExternalLink>;
};

export default MarkdownLink;
