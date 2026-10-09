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

const GRAYLOG_PATTERN = /Graylog/;

module.exports = {
  meta: {
    type: 'problem',
    fixable: false,
    docs: {
      description: "Avoid hardcoding 'Graylog' as a product name; use useProductName() or <ProductName /> instead",
      recommended: true,
    },
    messages: {
      noLiteralProductName:
        "Avoid hardcoding 'Graylog' as a product name. Use the useProductName() hook or <ProductName /> component instead.",
    },
    schema: [],
  },
  create: (context) => ({
    Literal(node) {
      if (typeof node.value !== 'string' || !GRAYLOG_PATTERN.test(node.value)) return;

      // Skip import/require paths
      if (node.parent.type === 'ImportDeclaration') return;
      if (node.parent.type === 'CallExpression' && node.parent.callee.name === 'require') return;

      context.report({ node, messageId: 'noLiteralProductName' });
    },

    JSXText(node) {
      if (GRAYLOG_PATTERN.test(node.value)) {
        context.report({ node, messageId: 'noLiteralProductName' });
      }
    },

    TemplateLiteral(node) {
      if (node.quasis.some((quasi) => GRAYLOG_PATTERN.test(quasi.value.raw))) {
        context.report({ node, messageId: 'noLiteralProductName' });
      }
    },
  }),
};
