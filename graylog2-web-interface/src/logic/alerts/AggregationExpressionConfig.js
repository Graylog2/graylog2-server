import uuid from 'uuid/v4';
import { omit } from 'lodash';

export const enrichExpressionTree = (expressionTree) => {
  const expressionTreeWithId = expressionTree;

  if (!expressionTreeWithId.id) {
    expressionTreeWithId.id = uuid();
  }

  if (expressionTree.left && expressionTree.right) {
    expressionTreeWithId.left = enrichExpressionTree(expressionTree.left);
    expressionTreeWithId.right = enrichExpressionTree(expressionTree.right);
  }

  return expressionTreeWithId;
};

export const cleanExpressionTree = (expressionTree) => {
  const expressionTreeWithoutId = omit(expressionTree, 'id');

  if (expressionTree.left && expressionTree.right) {
    expressionTreeWithoutId.left = cleanExpressionTree(expressionTree.left);
    expressionTreeWithoutId.right = cleanExpressionTree(expressionTree.right);
  }

  return expressionTreeWithoutId;
};

export const emptyComparisonExpressionConfig = () => {
  return {
    expr: undefined,
    left: {
      expr: 'number-ref',
      ref: uuid(),
    },
    right: {
      expr: 'number',
      value: 0,
    },
  };
};

export const emptyBooleanExpressionConfig = ({ operator = '&&', left = emptyComparisonExpressionConfig(), right = emptyComparisonExpressionConfig() }) => {
  return {
    expr: operator,
    left: left,
    right: right,
  };
};
