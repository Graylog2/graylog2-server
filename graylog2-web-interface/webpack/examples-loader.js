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
// Borrowed from https://urmaul.com/blog/react-styleguidist/ and updated to work with react-styleguidist v11.1.7

const fs = require('fs');

const defaultLoader = require('react-styleguidist/lib/loaders/examples-loader').default;

function getMarkdown(source, resourcePath) {
  const mdPath = resourcePath.replace('.example.tsx', '.md');

  if (fs.existsSync(mdPath)) {
    return fs.readFileSync(mdPath, 'utf8');
  }

  const stage1 = source.match(
    /(<>(.*?)<\/>|export const ([A-Za-z0-9]*Example[A-Za-z0-9]*))/gs,
  ) || [];

  const codes = stage1
    .map((s) => {
      // Create Header w/ example name
      return s.replace(
        /^export const ([A-Za-z0-9]*)Example[A-Za-z0-9]*$/s,
        '### $1',
      );
    })
    // Add the example implementation code
    // Code inside React.Fragment tags (<>...</>) => pasted as code
    .map((s) => {
      const code = s.match(/^<>(.*)<\/>$/s);

      if (code) {
        // count leading spaces at beginning of file
        const spaceCount = code[1].search(/\S|$/) - 1;
        // strip empty spaces from beginning of each line
        const codeLines = code[1].split('\n').map((line) => line.slice(spaceCount)).join('\n');

        return s.replace(/^<>(.*)<\/>$/s, `\`\`\`js static ${codeLines}\n\`\`\``);
      }

      return s;
    });

  codes.forEach((s, i) => {
    const exampleName = s.match(/^### ([A-Za-z0-9]*)$/s);

    // Inject example component to render
    if (exampleName) {
      codes.splice(i + 1, 0, `\`\`\`js noeditor\nconst ${exampleName[1]}Example = require('${resourcePath}').${exampleName[1]}Example;\n<${exampleName[1]}Example />\n\`\`\`\n`);
    }
  });

  return codes.join('\n\n');
}

function tsxExamplesLoader(source) {
  const markdown = getMarkdown(source, this.resourcePath);

  return defaultLoader.apply(this, [markdown]);
}

module.exports = tsxExamplesLoader;
