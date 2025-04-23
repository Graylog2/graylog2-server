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
      description: 'disallow specific words in string literals',
    },
    schema: [
      {
        type: 'object',
        properties: {
          words: {
            type: 'array',
            items: { type: 'string' },
          },
        },
        additionalProperties: false,
      },
    ],
    messages: {
      bannedWord: "The word '{{word}}' is not allowed.",
    },
  },

  create(context) {
    const options = context.options[0] || {};
    const bannedWords = new Set(options.words || []);
    // eslint-disable-next-line
    console.log('🚫 Current banned words:', Array.from(bannedWords));
    // eslint-disable-next-line
    console.log('Running custom ESLint rule');
    function check(value, node) {
      // eslint-disable-next-line no-restricted-syntax
      for (const word of bannedWords) {
        if (value.includes(word)) {
          context.report({
            node,
            messageId: 'bannedWord',
            data: { word },
          });
        }
      }
    }
    // eslint-disable-next-line
    console.log('Running custom ESLint rule');

    return {
      Literal(node) {
        // eslint-disable-next-line
        console.log('Running custom ESLint rule');
        if (typeof node.value === 'string') {
          check(node.value, node);
        }
      },
      TemplateElement(node) {
        // eslint-disable-next-line
        console.log('Running custom ESLint rule');
        check(node.value.raw, node);
      },
    };
  },
};
