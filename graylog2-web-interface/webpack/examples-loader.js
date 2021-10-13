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
// Borrowed wholesale from https://urmaul.com/blog/react-styleguidist/ and updated to work with react-styleguidist v11.1.7

const fs = require('fs');

const defaultLoader = require('react-styleguidist/lib/loaders/examples-loader').default;

function getMarkdown(source, resourcePath) {
  const mdPath = resourcePath.replace('.example.tsx', '.md');

  if (fs.existsSync(mdPath)) {
    return fs.readFileSync(mdPath, 'utf8');
  }

  // Parsed vars
  // Code inside React.Fragment tags (<>...</>) => pasted as code
  // Comments starting with '### ' => as is (h3 headers)
  // Exported consts with "Example" word in name => code with import
  const codes = (
    source.match(
      /(<>(.*?)<\/>|### (.*?)\n|export const ([A-Za-z0-9]*Example[A-Za-z0-9]*))/gs,
    ) || []
  )
    .map((s) => s.replace(
      /^export const ([A-Za-z0-9]*)$/s,
      `\`\`\`js\nconst $1 = require('${resourcePath}').$1;\n<$1 />\n\`\`\`\n`,
    ))
    .map((s) => s.replace(/^<>(.*)<\/>$/s, '```js\n$1\n```'));

  return codes.join('\n\n');
}

function tsxExamplesLoader(source) {
  const markdown = getMarkdown(source, this.resourcePath);

  return defaultLoader.apply(this, [markdown]);
}

module.exports = tsxExamplesLoader;
