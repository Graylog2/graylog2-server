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
import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { Button, ButtonToolbar, Clearfix, Col, FormGroup } from 'components/graylog';
import { Icon } from 'components/common';
import { emptyBooleanExpressionConfig, emptyGroupExpressionConfig, replaceBooleanExpressionOperatorInGroup } from 'logic/alerts/AggregationExpressionConfig';
import { internalNodePropType } from 'logic/alerts/AggregationExpressionTypes';

import NumberExpression from './AggregationConditionExpressions/NumberExpression';
import NumberRefExpression from './AggregationConditionExpressions/NumberRefExpression';
import BooleanOperatorSelector from './AggregationConditionExpressions/BooleanOperatorSelector';
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
    validation: PropTypes.object,
    formattedFields: PropTypes.array.isRequired,
    aggregationFunctions: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
    expression: internalNodePropType.isRequired,
    parent: internalNodePropType,
    level: PropTypes.number, // Internal use only
    renderLabel: PropTypes.bool,
  };

  static defaultProps = {
    level: 0,
    parent: undefined,
    renderLabel: true,
    validation: {},
  };

  state = {
    globalGroupOperator: undefined,
  };

  getEffectiveGlobalGroupOperator = () => {
    const { globalGroupOperator } = this.state;

    if (globalGroupOperator) {
      return globalGroupOperator;
    }

    const { expression } = this.props;

    return (expression.expr === '&&' || expression.expr === '||' ? expression.expr : '&&');
  };

  getBooleanOperator = (expression, defaultOperator) => {
    if (!expression) {
      return defaultOperator;
    }

    const key = expression.expr === 'group' ? 'operator' : 'expr';

    return lodash.get(expression, key, defaultOperator) === '&&' ? '&&' : '||';
  };

  handleAddExpression = () => {
    const { expression, onChange, parent } = this.props;
    const defaultOperator = this.getEffectiveGlobalGroupOperator();
    const prevOperator = this.getBooleanOperator(parent, defaultOperator);
    const nextExpression = emptyBooleanExpressionConfig({ operator: prevOperator, left: expression });

    onChange({ conditions: nextExpression });
  };

  handleAddGroup = () => {
    const { expression, onChange, parent } = this.props;
    const defaultOperator = this.getEffectiveGlobalGroupOperator();
    const prevOperator = this.getBooleanOperator(parent, defaultOperator);
    const groupOperator = prevOperator === '&&' ? '||' : '&&';
    const groupExpression = emptyGroupExpressionConfig({ operator: groupOperator });
    const nextExpression = emptyBooleanExpressionConfig({ operator: prevOperator, left: expression, right: groupExpression });

    onChange({ conditions: nextExpression });
  };

  handleDeleteExpression = () => {
    const { onChange } = this.props;

    onChange({ conditions: null });
  };

  handleChildChange = (branch) => {
    return (changes) => {
      const { expression, onChange } = this.props;

      if (!Object.keys(changes).includes('conditions')) {
        onChange(changes);

        return;
      }

      const update = changes.conditions;
      let nextUpdate;

      // A null update indicates that one of the branches got removed
      if (update === null) {
        if (branch === 'child') {
          // If this is the last branch of a group, remove the group altogether
          nextUpdate = null;
        } else {
          // Otherwise replace the current tree with the still existing branch
          nextUpdate = expression[(branch === 'left' ? 'right' : 'left')];
        }
      } else if (branch === 'child' && update.expr === 'group') {
        // Avoid that a group's child is another group. Groups should at least have one expression
        nextUpdate = update;
      } else {
        // Propagate the update in the expression tree.
        const nextExpression = lodash.cloneDeep(expression);

        nextExpression[branch] = update;
        nextUpdate = nextExpression;
      }

      onChange({ ...changes, conditions: nextUpdate });
    };
  };

  handleOperatorChange = (nextOperator) => {
    const { expression, onChange } = this.props;

    this.setState({ globalGroupOperator: nextOperator });
    const nextExpression = replaceBooleanExpressionOperatorInGroup(nextOperator, expression);

    onChange({ conditions: nextExpression });
  };

  render() {
    const { expression, parent, renderLabel } = this.props;
    let expressionComponent;

    switch (expression.expr) {
      case 'number-ref':
        expressionComponent = <NumberRefExpression {...this.props} parent={parent} />;
        break;
      case 'number':
        expressionComponent = <NumberExpression {...this.props} parent={parent} />;
        break;
      case 'group':
        expressionComponent = <GroupExpression {...this.props} onChildChange={this.handleChildChange} parent={parent} />;
        break;
      case '&&':
      case '||':
        expressionComponent = (
          <BooleanExpression {...this.props}
                             onChildChange={this.handleChildChange}
                             parent={parent} />
        );

        break;
      case '<':
      case '<=':
      case '>':
      case '>=':
      case '==':
      default:
        expressionComponent = (
          <>
            <ComparisonExpression {...this.props} onChildChange={this.handleChildChange} parent={parent} />
            <Col md={2}>
              <FormGroup>
                <div className={renderLabel ? styles.formControlNoLabel : undefined}>
                  <ButtonToolbar>
                    <Button bsSize="sm" onClick={this.handleDeleteExpression}><Icon name="minus" fixedWidth /></Button>
                    <Button bsSize="sm" onClick={this.handleAddExpression}><Icon name="plus" fixedWidth /></Button>
                    <Button bsSize="sm" onClick={this.handleAddGroup}>Add Group</Button>
                  </ButtonToolbar>
                </div>
              </FormGroup>
            </Col>
          </>
        );
    }

    if (!parent && expression.expr !== 'group') {
      return (
        <>
          <BooleanOperatorSelector initialText="Messages must meet"
                                   operator={this.getEffectiveGlobalGroupOperator()}
                                   onOperatorChange={this.handleOperatorChange} />
          <Clearfix />
          {expressionComponent}
        </>
      );
    }

    return expressionComponent;
  }
}

export default AggregationConditionExpression;
