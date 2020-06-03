import { union } from 'lodash';

const flattenValidationTree = (validationTree, errors = []) => {
  if (validationTree.message) {
    return union(errors, [validationTree.message]);
  }
  if (validationTree.left) {
    return union(errors, flattenValidationTree(validationTree.left), flattenValidationTree(validationTree.right));
  }
  if (validationTree.child) {
    return union(errors, flattenValidationTree(validationTree.child));
  }
  return errors;
};

const validateExpressionTree = (expression, series, validationTree = {}) => {
  switch (expression.expr) {
    case 'number':
      return (Number.isFinite(expression.value) ? {} : { message: 'Threshold must be a valid number' });
    case 'number-ref':
      /* eslint-disable no-case-declarations */
      const error = { message: 'Function must be set' };
      if (!expression.ref) {
        return error;
      }
      const selectedSeries = series.find((s) => s.id === expression.ref);
      return (selectedSeries && selectedSeries.function ? {} : error);
      /* eslint-enable no-case-declarations */
    case '&&':
    case '||':
    case '<':
    case '<=':
    case '>':
    case '>=':
    case '==':
      return {
        left: validateExpressionTree(expression.left, series, validationTree),
        right: validateExpressionTree(expression.right, series, validationTree),
      };
    case 'group':
      return { child: validateExpressionTree(expression.child, series, validationTree) };
    default:
      return { message: 'Condition must be set' };
  }
};

const validateExpression = (expression, series) => {
  const validationResults = {};

  if (!expression) {
    validationResults.isValid = true;
    return validationResults;
  }

  validationResults.validationTree = validateExpressionTree(expression, series);
  validationResults.errors = flattenValidationTree(validationResults.validationTree);
  validationResults.isValid = validationResults.errors.length === 0;

  return validationResults;
};

export default validateExpression;
