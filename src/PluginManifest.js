const path = require('path');

const create = (metadata, exports) => {
  return {
    metadata: metadata,
    exports: exports,
  };
}

module.exports = create;
