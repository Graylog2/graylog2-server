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
import { useEffect, useId, useState } from 'react';
import styled, { createGlobalStyle, css, useTheme } from 'styled-components';
import DOMPurify from 'dompurify';

import Spinner from 'components/common/Spinner';
import { COLOR_SCHEME_DARK } from 'theme/sawmill';
import { useTrackMarkdownRender } from 'components/common/markdown/MarkdownRenderTracker';

const Diagram = styled.div(
  ({ theme }) => css`
    margin: ${theme.spacings.sm} 0;
    max-width: 100%;
    overflow-x: auto;
    text-align: center;
  `,
);

const ID_PREFIX = 'mermaid-';
const GANTT_WIDTH = 800;

// Mermaid's labels use the class "label", which Bootstrap styles as badges. Global, as mermaid also measures the labels
// in an element of its own.
const LabelReset = createGlobalStyle`
  svg[id^='${ID_PREFIX}'] .label {
    display: inline;
    padding: 0;
    border-radius: 0;
    font-size: inherit;
    font-weight: inherit;
    line-height: inherit;
    white-space: normal;
  }
`;

const ErrorMessage = styled.p(
  ({ theme }) => css`
    margin: ${theme.spacings.xs} 0 0;
    color: ${theme.colors.text.secondary};
    font-size: ${theme.fonts.size.small};
  `,
);

// Unnamed, as plugins bundle it too and named chunks of different builds would overwrite each other.
const loadMermaid = () => import('mermaid').then((module) => module.default);

// Matches CSS that loads external resources. Fragments like `url(#arrowhead)` refer to the diagram itself.
const EXTERNAL_CSS_FUNCTION = /\b(?:url|src)\(\s*(?!['"]?#)|\b(?:image-set|image|cross-fade)\(/gi;
const EXTERNAL_CSS_AT_RULE = /@(?:import|font-face)/gi;

// Removing backslashes first prevents CSS escapes like `\75rl(`. The browser drops the invalidated declarations.
const invalidateExternalCss = (styles: string) =>
  styles.replace(/\\/g, '').replace(EXTERNAL_CSS_FUNCTION, 'invalid(').replace(EXTERNAL_CSS_AT_RULE, '@invalid');

const svgPurifier = DOMPurify();

// Mermaid passes styles of diagrams on unchecked, which could load images from sources the Markdown configuration
// does not allow.
svgPurifier.addHook('afterSanitizeAttributes', (node) => {
  if (node.nodeName.toLowerCase() === 'style') {
    node.replaceChildren(invalidateExternalCss(node.textContent ?? ''));
  }

  Array.from(node.attributes).forEach(({ name, value }) => {
    if (['href', 'xlink:href'].includes(name) && node.nodeName.toLowerCase() !== 'a' && !value.startsWith('#')) {
      node.removeAttribute(name);

      return;
    }

    const invalidated = invalidateExternalCss(value);

    if (invalidated !== value) {
      node.setAttribute(name, invalidated);
    }
  });
});

// Mermaid sanitizes labels already; this keeps what it adds to the page to SVG and the HTML of its labels.
const sanitizeSvg = (svg: string) =>
  svgPurifier.sanitize(svg, {
    USE_PROFILES: { svg: true, svgFilters: true, html: true },
    ADD_TAGS: ['foreignObject', 'style'],
    FORBID_TAGS: ['image', 'img', 'feImage'],
    HTML_INTEGRATION_POINTS: { foreignobject: true },
  });

type Props = {
  source: string;
};

type Result = { svg: string; error?: undefined } | { svg?: undefined; error: string };

const MermaidDiagram = ({ source }: Props) => {
  const theme = useTheme();
  const id = `${ID_PREFIX}${useId().replace(/[^a-zA-Z0-9_-]/g, '')}`;
  const [result, setResult] = useState<Result>();
  const darkMode = theme.mode === COLOR_SCHEME_DARK;
  useTrackMarkdownRender(!result);

  useEffect(() => {
    let cancelled = false;

    loadMermaid()
      .then(async (mermaid) => {
        mermaid.initialize({
          startOnLoad: false,
          securityLevel: 'strict',
          theme: darkMode ? 'dark' : 'default',
          // Gantt charts take the width of the hidden element they are rendered in, and their text shrinks when they
          // are scaled down to the diagram's width.
          gantt: { useWidth: GANTT_WIDTH },
        });
        const { svg } = await mermaid.render(id, source);

        return { svg: sanitizeSvg(svg) };
      })
      .catch((error: unknown) => {
        // Mermaid leaves the error diagram it renders into a failed diagram's container in the page's body.
        document.getElementById(`d${id}`)?.remove();

        return { error: error instanceof Error ? error.message : String(error) };
      })
      .then((next) => {
        if (!cancelled) {
          setResult(next);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [darkMode, id, source]);

  if (!result) {
    return (
      <>
        <LabelReset />
        <Spinner text="Rendering diagram..." />
      </>
    );
  }

  if (result.error !== undefined) {
    return (
      <div>
        <pre>
          <code>{source}</code>
        </pre>
        <ErrorMessage>This diagram could not be rendered: {result.error}</ErrorMessage>
      </div>
    );
  }

  return (
    <>
      <LabelReset />
      <Diagram role="img" aria-label="Diagram" dangerouslySetInnerHTML={{ __html: result.svg }} />
    </>
  );
};

export default MermaidDiagram;
