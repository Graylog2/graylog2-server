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

// Props whose values are shown to users. Other props (e.g. `id`, `name`, `type`) usually hold technical values.
const DEFAULT_VISIBLE_ATTRIBUTES = [
  'alt',
  'aria-label',
  'btnConfirmText',
  'buttonTitle',
  'confirmButtonText',
  'description',
  'emptyMessage',
  'errorText',
  'help',
  'helpText',
  'hoverTitle',
  'label',
  'noDataText',
  'placeholder',
  'submitButtonText',
  'submitLoadingText',
  'text',
  'title',
  'tooltip',
];

const LETTER = /\p{L}/u;

const containsText = (value) => typeof value === 'string' && LETTER.test(value);

const staticText = (node) => {
  if (!node) {
    return false;
  }

  if (node.type === 'Literal') {
    return containsText(node.value);
  }

  if (node.type === 'TemplateLiteral') {
    return node.quasis.some((quasi) => containsText(quasi.value.cooked));
  }

  if (node.type === 'JSXExpressionContainer') {
    return staticText(node.expression);
  }

  // e.g. `{isActive ? 'Active' : 'Inactive'}`
  if (node.type === 'ConditionalExpression') {
    return staticText(node.consequent) || staticText(node.alternate);
  }

  // e.g. `{title ?? 'Untitled'}`, but not `{isActive && <Badge />}`
  if (node.type === 'LogicalExpression') {
    return staticText(node.right);
  }

  return false;
};

module.exports = {
  meta: {
    type: 'problem',
    fixable: false,
    docs: {
      description: 'Disallow hardcoded user-visible text in translated parts of the UI; use `t()` from `useTranslation`',
      recommended: false,
    },
    messages: {
      untranslatedText: 'Translate user-visible text with `t()` from `useTranslation` instead of hardcoding it.',
    },
    schema: [
      {
        type: 'object',
        properties: {
          attributes: { type: 'array', items: { type: 'string' } },
        },
        additionalProperties: false,
      },
    ],
  },
  create: (context) => {
    const visibleAttributes = new Set(context.options[0]?.attributes ?? DEFAULT_VISIBLE_ATTRIBUTES);

    return {
      JSXText(node) {
        if (containsText(node.value)) {
          context.report({ node, messageId: 'untranslatedText' });
        }
      },

      JSXExpressionContainer(node) {
        if (node.parent.type !== 'JSXAttribute' && staticText(node.expression)) {
          context.report({ node, messageId: 'untranslatedText' });
        }
      },

      JSXAttribute(node) {
        if (visibleAttributes.has(node.name.name) && staticText(node.value)) {
          context.report({ node, messageId: 'untranslatedText' });
        }
      },
    };
  },
};
