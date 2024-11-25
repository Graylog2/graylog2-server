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
import { useCallback, useContext, useState } from 'react';

import { DEFAULT_HIGHLIGHT_COLOR } from 'views/Constants';
import HighlightingRulesContext from 'views/components/contexts/HighlightingRulesContext';
import IconButton from 'components/common/IconButton';
import { SortableList } from 'components/common';
import { updateHighlightingRules } from 'views/logic/slices/highlightActions';
import useAppDispatch from 'stores/useAppDispatch';
import type HighlightingRuleType from 'views/logic/views/formatting/highlighting/HighlightingRule';
import type { DraggableProps, DragHandleProps } from 'components/common/SortableList';

import HighlightingRule, { Container, RuleContainer } from './HighlightingRule';
import ColorPreview from './ColorPreview';
import HighlightForm from './HighlightForm';

import SectionInfo from '../SectionInfo';
import SectionSubheadline from '../SectionSubheadline';

type SortableHighlightingRuleProps = {
  item: { id: string, rule: HighlightingRuleType },
  draggableProps: DraggableProps,
  dragHandleProps: DragHandleProps,
  className?: string,
  ref: React.Ref<HTMLDivElement>
}
const SortableHighlightingRule = ({ item: { id, rule }, draggableProps, dragHandleProps, className, ref }: SortableHighlightingRuleProps) => (
  <HighlightingRule key={id}
                    rule={rule}
                    dragHandleProps={dragHandleProps}
                    draggableProps={draggableProps}
                    className={className}
                    ref={ref} />
);

const HighlightingRules = () => {
  const [showForm, setShowForm] = useState(false);
  const rules = useContext(HighlightingRulesContext) ?? [];
  const rulesWithId = rules.map((rule) => ({ rule, id: `${rule.field}-${rule.value}-${rule.color}-${rule.condition}` }));
  const dispatch = useAppDispatch();

  const updateRules = useCallback((newRulesWithId: Array<{ id: string, rule: HighlightingRuleType }>) => {
    const newRules = newRulesWithId.map(({ rule }) => rule);

    return dispatch(updateHighlightingRules(newRules));
  }, [dispatch]);

  return (
    <>
      <SectionInfo>
        Search terms and field values can be highlighted. Highlighting your search query in the results can be enabled/disabled in the graylog server config.
        Any field value can be highlighted by clicking on the value and selecting &quot;Highlight this value&quot;.
        If a term or a value has more than one rule, the first matching rule is used.
      </SectionInfo>
      <SectionSubheadline>
        Active highlights <IconButton className="pull-right"
                                      name="add"
                                      onClick={() => setShowForm(!showForm)}
                                      title="Add highlighting rule" />
      </SectionSubheadline>
      {showForm && <HighlightForm onClose={() => setShowForm(false)} />}
      <Container $displayBorder={!!rulesWithId?.length}>
        <ColorPreview color={DEFAULT_HIGHLIGHT_COLOR} />
        <RuleContainer>Search terms</RuleContainer>
      </Container>
      <SortableList items={rulesWithId}
                    onMoveItem={updateRules}
                    customListItemRender={SortableHighlightingRule} />
    </>
  );
};

export default HighlightingRules;
