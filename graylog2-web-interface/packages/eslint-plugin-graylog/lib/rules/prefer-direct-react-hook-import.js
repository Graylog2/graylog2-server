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

const REACT_HOOKS = [
  'useState',
  'useEffect',
  'useContext',
  'useReducer',
  'useCallback',
  'useMemo',
  'useRef',
  'useImperativeHandle',
  'useLayoutEffect',
  'useDebugValue',
];

module.exports = {
  meta: {
    type: 'problem',
    fixable: 'code',
    docs: {
      description: 'Require direct imports of React hooks instead of accessing them via React namespace',
      recommended: true,
    },
    messages: {
      preferDirectImport: 'Import {{hookName}} directly from React instead of using React.{{hookName}}',
    },
    schema: [],
  },
  create: (context) => {
    const hooksUsedInFile = new Set();
    let reactImportNode = null;
    let hasReactNamespaceImport = false;
    let hasReactDefaultImport = false;
    let existingNamedImports = [];

    return {
      // First pass: find the React import
      ImportDeclaration(node) {
        if (node.source.value === 'react') {
          reactImportNode = node;

          // Check for namespace import: import * as React
          const namespaceSpecifier = node.specifiers.find((s) => s.type === 'ImportNamespaceSpecifier');

          if (namespaceSpecifier) {
            hasReactNamespaceImport = true;
          }

          // Check for default import: import React
          const defaultSpecifier = node.specifiers.find((s) => s.type === 'ImportDefaultSpecifier');

          if (defaultSpecifier) {
            hasReactDefaultImport = true;
          }

          // Collect existing named imports
          const namedSpecifiers = node.specifiers.filter((s) => s.type === 'ImportSpecifier');

          existingNamedImports = namedSpecifiers.map((s) => s.imported.name);
        }
      },

      // Second pass: find React.useXxx usage
      MemberExpression(node) {
        if (
          node.object.type === 'Identifier' &&
          node.object.name === 'React' &&
          node.property.type === 'Identifier' &&
          REACT_HOOKS.includes(node.property.name)
        ) {
          const hookName = node.property.name;

          hooksUsedInFile.add(hookName);

          context.report({
            node,
            messageId: 'preferDirectImport',
            data: { hookName },
            fix(fixer) {
              const fixes = [];

              // Fix 1: Replace React.useXxx with useXxx
              fixes.push(fixer.replaceText(node, hookName));

              // Fix 2: Update import statement (only do this once per file)
              if (reactImportNode && !existingNamedImports.includes(hookName)) {
                // Add the hook to the list of hooks to import
                const allHooks = [...new Set([...existingNamedImports, ...Array.from(hooksUsedInFile)])].sort();

                let newImportText;

                if (hasReactNamespaceImport) {
                  // If there's a namespace import, add a separate named import line
                  // Find the position after the React import
                  const importEndPos = reactImportNode.range[1];

                  fixes.push(
                    fixer.insertTextAfterRange(
                      [importEndPos, importEndPos],
                      `\nimport { ${allHooks.join(', ')} } from 'react';`,
                    ),
                  );
                } else if (hasReactDefaultImport || existingNamedImports.length > 0) {
                  // Reconstruct the import with named imports
                  if (hasReactDefaultImport && allHooks.length > 0) {
                    newImportText = `import React, { ${allHooks.join(', ')} } from 'react';`;
                  } else if (hasReactDefaultImport) {
                    newImportText = `import React from 'react';`;
                  } else {
                    newImportText = `import { ${allHooks.join(', ')} } from 'react';`;
                  }

                  fixes.push(fixer.replaceText(reactImportNode, newImportText));
                } else {
                  // No React import found, this shouldn't happen but handle it gracefully
                  newImportText = `import { ${allHooks.join(', ')} } from 'react';`;
                  fixes.push(fixer.replaceText(reactImportNode, newImportText));
                }

                // Mark that we've added this hook to avoid duplicate additions
                existingNamedImports.push(hookName);
              }

              return fixes;
            },
          });
        }
      },
    };
  },
};

module.schema = [];
