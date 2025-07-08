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

import Timestamp from 'components/common/Timestamp';

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

const HTML = ({ html }: { html: string }) => parse(html);

const componentMapping = {
  'ts': ({ value }: { value: string }) => (
    <strong>
      <Timestamp dateTime={value} />
    </strong>
  ),
};
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

const transform: HTMLReactParserOptions['replace'] = (node) => {
  if (node.type === 'text' && node.data.match(componentRegexMatcher)) {
    return <>{replaceCustomComponents(node.data)}</>;
  }

  return undefined;
};

const Augment = ({ html }: { html: string }) => parse(html, { replace: transform });

const Markdown = ({ augment = false, text }: Props) => {
  // Remove dangerous HTML
  const sanitizedText = DOMPurify.sanitize(text ?? '', { USE_PROFILES: { html: false } });

  // Remove dangerous markdown
  const html = useMemo(() => DOMPurify.sanitize(marked(sanitizedText, { async: false })), [sanitizedText]);

  return augment ? <Augment html={html} /> : <HTML html={html} />;
};

export default Markdown;
