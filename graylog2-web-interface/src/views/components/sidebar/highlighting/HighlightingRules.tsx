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
import * as React from 'react';
import { useCallback, useContext, useState, forwardRef } from 'react';

import { DEFAULT_HIGHLIGHT_COLOR } from 'views/Constants';
import HighlightingRulesContext from 'views/components/contexts/HighlightingRulesContext';
import IconButton from 'components/common/IconButton';
import { SortableList } from 'components/common';
import type {
  Value,
  Condition,
  Color,
} from 'views/logic/views/formatting/highlighting/HighlightingRule';
import HighlightingRuleClass from 'views/logic/views/formatting/highlighting/HighlightingRule';
import type { DraggableProps, DragHandleProps } from 'components/common/SortableList';

import HighlightingRule, { Container, RuleContainer } from './HighlightingRule';
import ColorPreview from './ColorPreview';
import HighlightForm from './HighlightForm';

import SectionInfo from '../SectionInfo';
import SectionSubheadline from '../SectionSubheadline';

type SortableHighlightingRuleProps = {
  item: { id: string, rule: HighlightingRuleClass },
  draggableProps: DraggableProps,
  dragHandleProps: DragHandleProps,
  className?: string,
  onUpdate: (existingRule: HighlightingRuleClass, field: string, value: Value, condition: Condition, color: Color) => Promise<void>,
  onDelete: (rule: HighlightingRuleClass) => Promise<void>,
}

const SortableHighlightingRule = forwardRef<HTMLDivElement, SortableHighlightingRuleProps>(({
  item: { id, rule }, draggableProps, dragHandleProps, className = undefined,
  onUpdate, onDelete,
}, ref) => (
  <HighlightingRule key={id}
                    rule={rule}
                    onUpdate={onUpdate}
                    onDelete={onDelete}
                    dragHandleProps={dragHandleProps}
                    draggableProps={draggableProps}
                    className={className}
                    ref={ref} />
));

type Props = {
  description: string,
  onUpdateRules: (newRules: Array<HighlightingRuleClass>) => Promise<void>,
  onCreateRule: (newRule: HighlightingRuleClass) => Promise<void>,
  onUpdateRule: (targetRule: HighlightingRuleClass, field: string, value: Value, condition: Condition, color: Color) => Promise<void>,
  onDeleteRule: (rule: HighlightingRuleClass) => Promise<void>,
  showSearchHighlightInfo?: boolean,
}

const HighlightingRules = ({ description, onUpdateRules, onCreateRule: onCreateRuleProp, onUpdateRule, onDeleteRule, showSearchHighlightInfo = true }: Props) => {
  const [showForm, setShowForm] = useState(false);
  const rules = useContext(HighlightingRulesContext) ?? [];
  const rulesWithId = rules.map((rule) => ({ rule, id: `${rule.field}-${rule.value}-${rule.color}-${rule.condition}` }));

  const updateRules = useCallback((newRulesWithId: Array<{ id: string, rule: HighlightingRuleClass }>) => {
    const newRules = newRulesWithId.map(({ rule }) => rule);

    return onUpdateRules(newRules);
  }, [onUpdateRules]);

  const onCreateRule = useCallback((field: string, value: Value, condition: Condition, color: Color) => (
    onCreateRuleProp(HighlightingRuleClass.create(field, value, condition, color))
  ), [onCreateRuleProp]);

  const listItemRender = useCallback((props: {
    item: { id: string, rule: HighlightingRuleClass },
    draggableProps: DraggableProps,
    dragHandleProps: DragHandleProps,
    className?: string,
  }) => (
    <SortableHighlightingRule {...props} onUpdate={onUpdateRule} onDelete={onDeleteRule} />
  ), [onDeleteRule, onUpdateRule]);

  return (
    <>
      <SectionInfo>
        {description}
      </SectionInfo>
      <SectionSubheadline>
        Active highlights <IconButton className="pull-right"
                                      name="add"
                                      onClick={() => setShowForm(!showForm)}
                                      title="Add highlighting rule" />
      </SectionSubheadline>
      {showForm && <HighlightForm onClose={() => setShowForm(false)} onSubmit={onCreateRule} />}

      {showSearchHighlightInfo && (
        <Container $displayBorder={!!rulesWithId?.length}>
          <ColorPreview color={DEFAULT_HIGHLIGHT_COLOR} />
          <RuleContainer>Search terms</RuleContainer>
        </Container>
      )}
      <SortableList items={rulesWithId}
                    onMoveItem={updateRules}
                    customListItemRender={listItemRender} />
    </>
  );
};

export default HighlightingRules;
