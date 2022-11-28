const browserslist = require('browserslist');

const targets = browserslist('defaults, not ie 11');

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
