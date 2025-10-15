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
import { useMemo } from 'react';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import parse from 'html-react-parser';
import type { HTMLReactParserOptions } from 'html-react-parser';

import usePluginEntities from 'hooks/usePluginEntities';
import useMarkdownConfig from 'components/common/useMarkdownConfig';
import Spinner from 'components/common/Spinner';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

type Props = {
  text: string;
  augment?: boolean;
};

DOMPurify.addHook('afterSanitizeAttributes', (node) => {
  if (node instanceof HTMLAnchorElement && node.getAttribute('href')) {
    node.setAttribute('target', '_blank');
    node.setAttribute('rel', 'noopener noreferrer');
  }
});

type Transformer = Parameters<typeof parse>[1]['replace'];
type Renderer = ({ html, transformer }: { html: string; transformer?: Transformer }) => ReturnType<typeof parse>;

const HTML: Renderer = ({ html, transformer }) => parse(html, { replace: transformer });

const useMarkdownTransformer = (): HTMLReactParserOptions['replace'] => {
  const components = usePluginEntities('markdown.augment.components');

  return useMemo(() => {
    const componentMapping = Object.fromEntries(components.map(({ id, component }) => [id, component]));
    const markers = Object.keys(componentMapping).join('|');
    const componentRegexMatcher = new RegExp(`(#(?:${markers})#.+?#(?:${markers})#)`);
    const componentRegexExtractor = new RegExp(`(#(?:(${markers}))#(.+?)#(?:${markers})#)`);

    const splitByMarker = (text: string): string[] => text.split(componentRegexMatcher).filter((part) => part !== '');
    const renderComponent = (element: string, idx: number) => {
      const matchesComponent = element.match(componentRegexExtractor);
      if (matchesComponent) {
        const value = matchesComponent[3];
        const Component = componentMapping[matchesComponent[2]];

        if (Component) {
          return <Component key={`${value}-${idx}`} value={value} />;
        }
      }

      return element;
    };

    const replaceCustomComponents = (text: string): Array<string | React.ReactElement> =>
      splitByMarker(text).map(renderComponent);

    return (node) => {
      if (node.type === 'text' && node.data.match(componentRegexMatcher)) {
        return <>{replaceCustomComponents(node.data)}</>;
      }

      return undefined;
    };
  }, [components]);
};

const mergeTransformer =
  (transformers: Array<Transformer>): Transformer =>
  (domNode, idx) => {
    for (const transformer of transformers) {
      const result = transformer(domNode, idx);
      if (result) {
        return result;
      }
    }

    return undefined;
  };

const Augment: Renderer = ({ html, transformer }) => {
  const markdownTransformer = useMarkdownTransformer();
  const replace = useMemo(
    () => (transformer ? mergeTransformer([markdownTransformer, transformer]) : markdownTransformer),
    [markdownTransformer, transformer],
  );

  return parse(html, { replace });
};

const UnsupportedImageWarning = () => (
  <span>
    Images are not supported for security reasons. Please enable allowed sources{' '}
    <Link to={Routes.SYSTEM.configurationsSection('Markdown')}>here</Link>
  </span>
);

const imageWarningTransformer: Transformer = (domNode) => {
  if ('name' in domNode && domNode.name === 'img') {
    return <UnsupportedImageWarning />;
  }

  return undefined;
};

const Markdown = ({ augment = false, text }: Props) => {
  const { isInitialLoading, data: markdownConfig } = useMarkdownConfig();
  // Remove dangerous HTML
  const sanitizedText = DOMPurify.sanitize(text ?? '', { USE_PROFILES: { html: false } });

  // Remove dangerous markdown
  const html = useMemo(() => DOMPurify.sanitize(marked(sanitizedText, { async: false })), [sanitizedText]);
  const transformer =
    markdownConfig?.allow_all_image_sources == true || markdownConfig?.allowed_image_sources
      ? undefined
      : imageWarningTransformer;

  const RendererComponent = augment ? Augment : HTML;

  if (isInitialLoading) {
    return <Spinner />;
  }

  return <RendererComponent html={html} transformer={transformer} />;
};

export default Markdown;
