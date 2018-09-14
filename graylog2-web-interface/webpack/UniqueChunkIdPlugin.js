const crypto = require('crypto');

const pluginName = 'UniqueChunkIdPlugin';

export default class UniqueChunkIdPlugin {
  // eslint-disable-next-line class-methods-use-this
  apply(compiler) {
    const randomId = crypto.randomBytes(4).toString('hex');
    compiler.hooks.compilation.tap(pluginName, (compilation) => {
      compilation.hooks.optimizeChunkIds.tap(pluginName, chunks => chunks.map((chunk) => {
        // eslint-disable-next-line prefer-template, no-param-reassign
        chunk.id = randomId + '-' + chunk.id;
        // eslint-disable-next-line prefer-template, no-param-reassign
        chunk.ids = chunk.ids.map(id => randomId + '-' + id);
        return chunk;
      }));
    });
  }
}
