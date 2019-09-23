/* eslint-disable func-names */
module.exports = function (context) {
  const bannedImports = [{ name: 'react-bootstrap', alternative: 'components/graylog' }];
  return {
    ImportDeclaration(node) {
      // eslint-disable-next-line no-unused-expressions
      node.specifiers && node.specifiers.map((item) => {
        const bannedItem = bannedImports.find(({ name }) => name === item.parent.source.value);
        if (bannedItem) {
          return context.report(node, item.loc, `Do not use ${bannedItem.name}, instead use ${bannedItem.alternative}`);
        }
      });

      return null;
    },
  };
};
