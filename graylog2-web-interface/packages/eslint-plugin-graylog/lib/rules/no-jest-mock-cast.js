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

const isJestMockTypeReference = (typeNode) =>
  !!typeNode &&
  typeNode.type === 'TSTypeReference' &&
  typeNode.typeName.type === 'TSQualifiedName' &&
  typeNode.typeName.left.type === 'Identifier' &&
  typeNode.typeName.left.name === 'jest' &&
  typeNode.typeName.right.type === 'Identifier' &&
  typeNode.typeName.right.name === 'Mock';

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      description: 'Disallow casting to `jest.Mock`, use the `asMock` helper function instead',
      recommended: true,
    },
    messages: {
      noJestMockCast: 'Do not cast to `jest.Mock` (`foo as jest.Mock`), use the `asMock` helper function instead.',
    },
    schema: [],
  },
  create: (context) => ({
    TSAsExpression(node) {
      if (isJestMockTypeReference(node.typeAnnotation)) {
        context.report({ node, messageId: 'noJestMockCast' });
      }
    },
  }),
};
