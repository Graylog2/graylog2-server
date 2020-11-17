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
const crypto = require('crypto');

const pluginName = 'UniqueChunkIdPlugin';

class UniqueChunkIdPlugin {
  // eslint-disable-next-line class-methods-use-this
  apply(compiler) {
    const randomId = crypto.randomBytes(4).toString('hex');
    // eslint-disable-next-line prefer-template
    const prefix = randomId + '-';
    compiler.hooks.compilation.tap(pluginName, (compilation) => {
      compilation.hooks.optimizeChunkIds.tap(pluginName, chunks => chunks.map((chunk) => {
        // eslint-disable-next-line prefer-template, no-param-reassign
        if (chunk.id && chunk.id.startsWith && chunk.id.startsWith(prefix)) {
          return chunk;
        }
        // eslint-disable-next-line no-param-reassign
        chunk.id = prefix + chunk.id;
        // eslint-disable-next-line prefer-template, no-param-reassign
        chunk.ids = chunk.ids.map(id => randomId + '-' + id);
        return chunk;
      }));
    });
  }
}

module.exports = UniqueChunkIdPlugin;
