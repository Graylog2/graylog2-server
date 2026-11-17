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
import styled, { css } from 'styled-components';
import { Marked } from 'marked';
import DOMPurify from 'dompurify';
import escape from 'lodash/escape';
import parse, { domToReact, Element } from 'html-react-parser';
import type { DOMNode, HTMLReactParserOptions } from 'html-react-parser';

import usePluginEntities from 'hooks/usePluginEntities';
import useMarkdownConfig from 'components/common/useMarkdownConfig';
import Spinner from 'components/common/Spinner';
import { Link } from 'components/common';
import Routes from 'routing/Routes';
import MarkdownLink from 'components/common/markdown/MarkdownLink';
import Table from 'components/bootstrap/Table';
import MermaidDiagram from 'components/common/markdown/MermaidDiagram';
import { useTrackMarkdownRender } from 'components/common/markdown/MarkdownRenderTracker';

type Props = {
  text: string;
  augment?: boolean;
};

const ALLOWED_URI_REGEXP = /^(?:https?:|graylog:|[^a-z])/i;

const markdownParser = new Marked({
  async: false,
  renderer: {
    html: ({ text }) => escape(text),
  },
});

type Transformer = Parameters<typeof parse>[1]['replace'];
type Renderer = ({ html, transformer }: { html: string; transformer?: Transformer }) => ReturnType<typeof parse>;

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

// A table wider than its container scrolls instead of squeezing its columns.
const SpacedTable = styled(Table)(
  ({ theme }) => css`
    margin: ${theme.spacings.xs} 0;
    width: auto;
  `,
);

// The content of links and tables is transformed like everything else, e.g. its augmented values.
const parseHtml = (html: string, transformer: Transformer) => {
  const options: HTMLReactParserOptions = {};
  const tableTransformer: Transformer = (domNode) =>
    domNode instanceof Element && domNode.name === 'table' ? (
      <SpacedTable responsive condensed striped={false} hover={false}>
        {domToReact(domNode.children as DOMNode[], options)}
      </SpacedTable>
    ) : undefined;
  const linkTransformer: Transformer = (domNode) => {
    if (!(domNode instanceof Element) || domNode.name !== 'a' || !domNode.attribs?.href) {
      return undefined;
    }

    return (
      <MarkdownLink href={domNode.attribs.href}>{domToReact(domNode.children as DOMNode[], options)}</MarkdownLink>
    );
  };
  const builtInTransformers = [linkTransformer, tableTransformer];
  options.replace = mergeTransformer(transformer ? [transformer, ...builtInTransformers] : builtInTransformers);

  return parse(html, options);
};

const HTML: Renderer = ({ html, transformer }) => parseHtml(html, transformer);

const Augment: Renderer = ({ html, transformer }) => {
  const markdownTransformer = useMarkdownTransformer();
  const replace = useMemo(
    () => (transformer ? mergeTransformer([markdownTransformer, transformer]) : markdownTransformer),
    [markdownTransformer, transformer],
  );

  return parseHtml(html, replace);
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

const textOf = (nodes: Element['children']): string =>
  nodes
    .map((node) => {
      if ('data' in node) {
        return node.data;
      }

      return 'children' in node ? textOf(node.children) : '';
    })
    .join('');

// Code blocks in the language "mermaid" are shown as diagrams.
const mermaidTransformer: Transformer = (domNode) => {
  if (!(domNode instanceof Element) || domNode.name !== 'pre') {
    return undefined;
  }

  const code = domNode.children.find((child): child is Element => child instanceof Element && child.name === 'code');

  if (!code?.attribs?.class?.split(' ').includes('language-mermaid')) {
    return undefined;
  }

  return <MermaidDiagram source={textOf(code.children)} />;
};

const Markdown = ({ augment = false, text }: Props) => {
  const { isInitialLoading, data: markdownConfig } = useMarkdownConfig();
  useTrackMarkdownRender(isInitialLoading);

  // Raw HTML is escaped by the parser, sanitizing the output is a safety net
  const html = useMemo(
    () => DOMPurify.sanitize(markdownParser.parse(text ?? '', { async: false }), { ALLOWED_URI_REGEXP }),
    [text],
  );

  const imageTransformer: Transformer =
    markdownConfig?.allow_all_image_sources == true || markdownConfig?.allowed_image_sources
      ? undefined
      : imageWarningTransformer;

  const transformer = useMemo(
    () => mergeTransformer([mermaidTransformer, ...(imageTransformer ? [imageTransformer] : [])]),
    [imageTransformer],
  );

  const RendererComponent = augment ? Augment : HTML;

  if (isInitialLoading) {
    return <Spinner />;
  }

  return <RendererComponent html={html} transformer={transformer} />;
};

export default Markdown;
