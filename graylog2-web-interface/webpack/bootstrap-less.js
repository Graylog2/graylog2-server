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
const path = require('path');
const fs = require('fs');

const ROOT_PATH = path.resolve(__dirname, '..');

const BOOTSTRAPVARS = JSON.parse(
  fs.readFileSync(path.resolve(ROOT_PATH, 'public/stylesheets/bootstrap-config.json'), 'utf-8'),
).vars;

function insertAtTop(element) {
  const parent = document.querySelector('head');
  // @ts-ignore
  const lastInsertedElement = window._lastElementInsertedByStyleLoader;

  if (!lastInsertedElement) {
    parent.insertBefore(element, parent.firstChild);
  } else if (lastInsertedElement.nextSibling) {
    parent.insertBefore(element, lastInsertedElement.nextSibling);
  } else {
    parent.appendChild(element);
  }

  // @ts-ignore
  window._lastElementInsertedByStyleLoader = element;
}

function createBootstrapLessRule(styleLoaderOptions = {}) {
  return {
    test: /bootstrap\.less$/,
    use: [
      { loader: 'style-loader', options: { insert: insertAtTop, ...styleLoaderOptions } },
      'css-loader',
      { loader: 'less-loader', options: { lessOptions: { modifyVars: BOOTSTRAPVARS } } },
    ],
  };
}

const bootstrapLessRule = createBootstrapLessRule();
const lessRule = { test: /\.less$/, use: ['style-loader', 'css-loader', 'less-loader'], exclude: /bootstrap\.less$/ };

module.exports = { createBootstrapLessRule, bootstrapLessRule, lessRule };
