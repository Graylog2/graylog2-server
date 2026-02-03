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
module.exports = {
  meta: {
    type: 'problem',
    docs: {
      description: 'Ensure class component event handlers are arrow functions or explicitly bound in the constructor',
      category: 'Best Practices',
      recommended: false,
    },
    messages: {
      useArrowFunction:
        "Event handler should be an arrow function or explicitly bound in the constructor to preserve 'this' scope.",
    },
    schema: [],
    name: 'use-arrow-function-for-event-handlers-in-classes',
  },
  create(context) {
    const boundMethods = new Set();

    return {
      MethodDefinition(node) {
        if (node.kind === 'constructor') {
          node.value.body.body.forEach((statement) => {
            if (
              statement.type === 'ExpressionStatement' &&
              statement.expression.type === 'AssignmentExpression' &&
              statement.expression.right.type === 'CallExpression' &&
              statement.expression.right.callee.property &&
              statement.expression.right.callee.property.name === 'bind' &&
              statement.expression.left.type === 'MemberExpression' &&
              statement.expression.left.object.type === 'ThisExpression'
            ) {
              boundMethods.add(statement.expression.left.property.name);
            }
          });
        }
      },
      JSXAttribute(node) {
        const { value } = node;
        if (!value || value.type !== 'JSXExpressionContainer') return;

        const expr = value.expression;
        if (expr.type === 'MemberExpression' && expr.object.type === 'ThisExpression') {
          let classBody = node;

          while (classBody && classBody.type !== 'ClassBody') {
            classBody = classBody.parent;
          }

          if (classBody) {
            const method = classBody.body.find(
              (_method) => _method.type === 'MethodDefinition' && _method.key.name === expr.property.name,
            );

            if (method && method.value.type !== 'ArrowFunctionExpression' && !boundMethods.has(expr.property.name)) {
              context.report({
                node: expr,
                messageId: 'useArrowFunction',
              });
            }
          }
        }
      },
    };
  },
};
