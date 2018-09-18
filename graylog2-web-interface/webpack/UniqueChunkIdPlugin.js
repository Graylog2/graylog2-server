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
