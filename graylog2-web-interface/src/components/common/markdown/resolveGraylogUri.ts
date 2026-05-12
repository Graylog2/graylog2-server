import type { PluginExports } from 'graylog-web-plugin/plugin';

import { createGRN } from 'logic/permissions/GRN';

/**
 * Parses a `graylog:///<segment>(/<more>...)` URI and resolves it to a GRN by delegating to
 * the first matching {@link EntityLinkResolver} in the supplied registry.
 *
 * Returns `null` when the URI doesn't match the scheme, no resolver claims the first segment,
 * or the matching resolver declines (e.g. the id isn't a valid 24-character hex).
 *
 * This is the function we expect core's `<Markdown>` (once extended) to call against the
 * merged `markdown.entityLinkResolvers` plugin namespace.
 */
const GRAYLOG_URI = /^graylog:\/\/\/([^/]+)(?:\/(.+))?$/;

const resolveGraylogUri = (uri: string, resolvers: PluginExports['markdown.entityLinkResolvers']): string | null => {
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

  const resolved = resolver.resolve([trailingSegments.at(-1)]);

  if (!resolved) {
    return null;
  }

  return createGRN(resolved.grnType, resolved.id);
};

export default resolveGraylogUri;
