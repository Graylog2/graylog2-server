import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { Button, ButtonToolbar, Col, FormGroup } from 'components/graylog';
import { Icon } from 'components/common';

import { emptyBooleanExpressionConfig, emptyGroupExpressionConfig } from 'logic/alerts/AggregationExpressionConfig';

import { internalNodePropType } from 'logic/alerts/AggregationExpressionTypes';

import NumberExpression from './AggregationConditionExpressions/NumberExpression';
import NumberRefExpression from './AggregationConditionExpressions/NumberRefExpression';
/* eslint-disable import/no-cycle */
// We render the expression tree recursively, so complex expressions need to refer back to this component
import BooleanExpression from './AggregationConditionExpressions/BooleanExpression';
import ComparisonExpression from './AggregationConditionExpressions/ComparisonExpression';
import GroupExpression from './AggregationConditionExpressions/GroupExpression';
/* eslint-enable import/no-cycle */

import styles from './AggregationConditionExpression.css';

class AggregationConditionExpression extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    formattedFields: PropTypes.array.isRequired,
    aggregationFunctions: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
    expression: internalNodePropType.isRequired,
    parent: internalNodePropType,
    groupNodes: PropTypes.array.isRequired,
    level: PropTypes.number, // Internal use only
    renderLabel: PropTypes.bool,
  };

  static defaultProps = {
    level: 0,
    parent: undefined,
    renderLabel: true,
  };

  handleAddExpression = () => {
    const { expression, onChange, parent } = this.props;
    const prevOperator = lodash.get(parent, 'expr', '&&') === '&&' ? '&&' : '||';
    const nextExpression = emptyBooleanExpressionConfig({ operator: prevOperator, left: expression });
    onChange('conditions', nextExpression);
  };

  handleAddGroup = () => {
    const { expression, onChange, parent } = this.props;
    const prevOperator = lodash.get(parent, 'expr', '&&') === '&&' ? '&&' : '||';
    const groupOperator = prevOperator === '&&' ? '||' : '&&';
    const groupExpression = emptyGroupExpressionConfig({ operator: groupOperator });
    const nextExpression = emptyBooleanExpressionConfig({ operator: prevOperator, left: expression, right: groupExpression });
    onChange('conditions', nextExpression);
    onChange('groups', nextExpression.id);
  };

  handleDeleteExpression = () => {
    const { onChange } = this.props;
    onChange('conditions', null);
  };

  handleChildChange = (branch) => {
    return (key, update) => {
      const { expression, onChange } = this.props;

      let nextUpdate = update;
      if (key === 'conditions') {
        // A null update indicates that one of the branches got removed
        if (update === null) {
          if (branch === 'child') {
            // If this is the last branch of a group, remove the group altogether
            nextUpdate = null;
          } else {
            // Otherwise replace the current tree with the still existing branch
            nextUpdate = branch === 'child' ? null : expression[(branch === 'left' ? 'right' : 'left')];
          }
        } else {
          // Propagate the update in the expression tree.
          const nextExpression = lodash.cloneDeep(expression);
          nextExpression[branch] = update;
          nextUpdate = nextExpression;
        }
      }

      onChange(key, nextUpdate);
    };
  };

  render() {
    const { expression, groupNodes, parent, renderLabel } = this.props;

    switch (expression.expr) {
      case 'number-ref':
        return <NumberRefExpression {...this.props} parent={parent} />;
      case 'number':
        return <NumberExpression {...this.props} parent={parent} />;
      case 'group':
        return <GroupExpression {...this.props} onChildChange={this.handleChildChange} parent={parent} />;
      case '&&':
      case '||':
        return (
          <BooleanExpression {...this.props}
                             onChildChange={this.handleChildChange}
                             parent={parent}
                             groupNodes={groupNodes} />
        );
      case '<':
      case '<=':
      case '>':
      case '>=':
      case '==':
      default:
        return (
          <>
            <ComparisonExpression {...this.props} onChildChange={this.handleChildChange} parent={parent} />
            <Col md={2}>
              <FormGroup>
                <div className={renderLabel ? styles.formControlNoLabel : undefined}>
                  <ButtonToolbar>
                    <Button bsSize="sm" onClick={this.handleDeleteExpression}><Icon name="minus" fixedWidth /></Button>
                    <Button bsSize="sm" onClick={this.handleAddExpression}><Icon name="plus" fixedWidth /></Button>
                    <Button bsSize="sm" onClick={this.handleAddGroup}><Icon name="code-fork" fixedWidth /></Button>
                  </ButtonToolbar>
                </div>
              </FormGroup>
            </Col>
          </>
        );
    }
  }
}

export default AggregationConditionExpression;
