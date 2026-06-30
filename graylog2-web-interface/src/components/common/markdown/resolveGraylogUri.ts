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
import type { PluginExports } from 'graylog-web-plugin/plugin';

type EntityLinkResolvers = NonNullable<PluginExports['markdown.entityLinkResolvers']>;
export type EntityLinkResolution = NonNullable<ReturnType<EntityLinkResolvers[number]['resolve']>>;

/**
 * Parses a `graylog:///<segment>(/<more>...)` URI and delegates resolution to the first matching
 * {@link EntityLinkResolver} in the supplied registry. Returns the resolver's result — either an
 * entity descriptor `{ grnType, id }` that callers can turn into a route, or an `{ onClick }`
 * handler the link should invoke when activated.
 *
 * Returns `null` when the URI doesn't match the scheme, no resolver claims the first segment, or
 * the matching resolver declines.
 */
const GRAYLOG_URI = /^graylog:\/\/\/([^/]+)(?:\/(.+))?$/;

const resolveGraylogUri = (
  uri: string,
  resolvers: PluginExports['markdown.entityLinkResolvers'],
): EntityLinkResolution | null => {
  const match: RegExpExecArray | null = GRAYLOG_URI.exec(uri);

  if (!match) {
    return null;
  }

  const [, uriSegment, trailing] = match;
  const trailingSegments = trailing?.split('/') ?? [];

  const resolver = resolvers.find((candidate) => candidate.uriSegment === uriSegment);

  if (!resolver) {
    return null;
  }

  return resolver.resolve([trailingSegments.at(-1)]);
};

export default resolveGraylogUri;
