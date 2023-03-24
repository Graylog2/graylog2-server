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
const browserslist = require('browserslist');

const targets = browserslist('defaults, not ie 11, chrome 68');

const supportedProducts = ['chrome', 'edge', 'firefox', 'ios_saf', 'opera', 'safari'];

const productMapping = {
  chrome: 'chrome',
  edge: 'edge',
  firefox: 'firefox',
  ios_saf: 'ios',
  opera: 'opera',
  safari: 'safari',
};

const mapProductAndVersion = (product, version) => {
  const mappedProduct = productMapping[product];
  const mappedVersion = version.includes('-')
    ? version.split('-')[0]
    : version;

  return `${mappedProduct}${mappedVersion}`;
};

const mappedTargets = targets
  .map((target) => target.split(' '))
  .filter(([product]) => supportedProducts.includes(product))
  .map(([product, version]) => mapProductAndVersion(product, version));

module.exports = mappedTargets;
