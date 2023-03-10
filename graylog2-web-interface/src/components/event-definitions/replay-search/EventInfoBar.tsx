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

import React, { useCallback, useMemo, useState } from 'react';
import styled, { css } from 'styled-components';
import lodash from 'lodash';
import moment from 'moment';

import Routes from 'routing/Routes';
import { Button } from 'components/bootstrap';
import { extractDurationAndUnit } from 'components/common/TimeUnitInput';
import { ColorPickerPopover, FlatContentRow, HoverForHelp, Icon, Timestamp } from 'components/common';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import { TIME_UNITS } from 'components/event-definitions/event-definition-types/FilterForm';
import { useStore } from 'stores/connect';
import { EventNotificationsStore } from 'stores/event-notifications/EventNotificationsStore';
import { Link } from 'components/common/router';
import useAlertAndEventDefinitionData from 'hooks/useAlertAndEventDefinitionData';
import useAppSelector from 'stores/useAppSelector';
import { selectHighlightingRules } from 'views/logic/slices/highlightSelectors';
import useHighlightValuesForEventDefinition, {
  conditionToExprMapper,
  exprToConditionMapper,
} from 'hooks/useHighlightValuesForEventDefinition';
import { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';
import { DEFAULT_CUSTOM_HIGHLIGHT_RANGE } from 'views/Constants';
import useAppDispatch from 'stores/useAppDispatch';
import {
  createHighlightingRules,
  updateHighlightingRule,
} from 'views/logic/slices/highlightActions';
import type HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import { randomColor } from 'views/logic/views/formatting/highlighting/HighlightingRule';

const AlertTimestamp = styled(Timestamp)(({ theme }) => css`
  color: ${theme.colors.variant.darker.warning}
`);

const ColorComponent = styled.div`
  width: 13px;
  height: 13px;
  border-radius: 2px;
`;

const AggregationCondition = styled.div`
  display: flex;
  gap: 5px;
  align-items: center;
`;

const AggregationsList = styled.div`
  display: flex;
  gap: 5px;
`;

const Header = styled.div`
  display: flex;
  align-items: center;
  user-select: none;
  gap: 5px;
`;

const Item = styled.div`
  display: flex;
  gap: 5px;
  align-items: flex-end;
`;

const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: 5px;
`;

const Row = styled.div`
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
`;
const useHighlightingRules = () => useAppSelector(selectHighlightingRules);

const EventInfoBar = () => {
  useHighlightValuesForEventDefinition();
  const dispatch = useAppDispatch();
  const [open, setOpen] = useState<boolean>(true);
  const allNotifications = useStore(EventNotificationsStore, ({ all }) => {
    return all.reduce((res, cur) => {
      res[cur.id] = cur;

      return res;
    }, {});
  });
  const { eventData, eventDefinition, aggregations, isEventDefinition, isEvent, isAlert } = useAlertAndEventDefinitionData();

  const toggleOpen = (e) => {
    e.stopPropagation();
    setOpen((cur) => !cur);
  };

  const searchWithin = extractDurationAndUnit(eventDefinition.config.search_within_ms, TIME_UNITS);
  const executeEvery = extractDurationAndUnit(eventDefinition.config.execute_every_ms, TIME_UNITS);

  const notificationList = useMemo(() => {
    return eventDefinition.notifications.reduce((res, cur) => {
      if (allNotifications[cur.notification_id]) {
        res.push((allNotifications[cur.notification_id]));
      }

      return res;
    }, []);
  }, [eventDefinition, allNotifications]);
  const aggregationsMap = useMemo(() => new Map(aggregations.map((agg) => [`${agg.fnSeries}${agg.expr}${agg.value}`, agg])), [aggregations]);
  const isEDUpdatedAfterEvent = !isEventDefinition && moment(eventDefinition.updated_at).diff(eventData.timestamp) > 0;
  const highlightingRules = useHighlightingRules();
  const highlightedAggregations = useMemo<Map<string, HighlightingRule>>(() => {
    const initial = new Map<string, HighlightingRule>(aggregations.map(({ fnSeries, value, expr }) => [`${fnSeries}${expr}${value}`, undefined]));

    return highlightingRules.reduce((acc, rule) => {
      const { field, value, condition } = rule;
      const expr = conditionToExprMapper?.[condition];
      let result = acc;

      if (expr) {
        const key = `${field}${expr}${value}`;

        if (acc.has(key)) {
          result = result.set(key, rule);
        }
      }

      return result;
    }, initial);
  }, [aggregations, highlightingRules]);

  const changeColor = useCallback(({ rule, newColor, condition }) => {
    if (rule) {
      dispatch(updateHighlightingRule(rule, { color: StaticColor.create(newColor) }));
    } else {
      const { value, fnSeries, expr } = aggregationsMap.get(condition);

      dispatch(createHighlightingRules([
        {
          value,
          field: fnSeries,
          color: randomColor(),
          condition: exprToConditionMapper[expr],
        },
      ]));
    }
  }, [aggregationsMap, dispatch]);

  const items = [
    { title: 'Timestamp', content: <Timestamp dateTime={eventData?.timestamp} />, show: !isEventDefinition },
    {
      title: 'Event definition updated at',
      content: (
        <>
          <AlertTimestamp dateTime={eventDefinition.updated_at} />
          <HoverForHelp iconSize="xs">{
            `Event definition ${eventDefinition.title} was edited after this event happened.
                Some of aggregations widgets might not be representative for this event.`
          }
          </HoverForHelp>
        </>
      ),
      show: isEDUpdatedAfterEvent,
    },
    { title: 'Event definition', content: <Link target="_blank" to={Routes.ALERTS.DEFINITIONS.show(eventDefinition.id)}>{eventDefinition.title}</Link>, show: !isEventDefinition },
    { title: 'Priority', content: lodash.upperFirst(EventDefinitionPriorityEnum.properties[eventDefinition.priority].name) },
    { title: 'Execute search every', content: `${executeEvery.duration}${executeEvery.unit.toLowerCase()}` },
    { title: 'Search within', content: `${searchWithin.duration}${searchWithin.unit.toLowerCase()}` },
    { title: 'Description', content: eventDefinition.description },
    {
      title: 'Notifications',
      content: notificationList.map(({ id, title }, index) => {
        const prefix = index > 0 ? ', ' : '';

        return (
          <span key={id}>
            {prefix}
            <Link target="_blank" to={Routes.ALERTS.NOTIFICATIONS.show(id)}>{title}</Link>
          </span>
        );
      }),
    },
    {
      title: 'Aggregation conditions',
      content: (
        <AggregationsList>
          {
            Array.from(highlightedAggregations).map(([condition, rule]) => {
              const color = rule?.color as StaticColor;
              const hexColor = color?.color;

              return (
                // eslint-disable-next-line react/no-array-index-key
                <AggregationCondition title={condition} key={condition}>
                  <ColorPickerPopover id="formatting-rule-color"
                                      placement="right"
                                      color={hexColor}
                                      colors={DEFAULT_CUSTOM_HIGHLIGHT_RANGE.map((c) => [c])}
                                      triggerNode={(
                                        <ColorComponent style={{ backgroundColor: hexColor }}>
                                          {!hexColor && <Icon name="fill-drip" size="xs" />}
                                        </ColorComponent>
                                      )}
                                      onChange={(newColor, _, hidePopover) => {
                                        hidePopover();
                                        changeColor({ newColor, rule, condition });
                                      }} />
                  <span>{condition}</span>
                </AggregationCondition>
              );
            })
          }
        </AggregationsList>
      ),
    },
  ];

  const currentTypeText = useMemo(() => {
    if (isEventDefinition) return 'event definition';
    if (isAlert) return 'alert';
    if (isEvent) return 'event';

    return '';
  }, [isAlert, isEvent, isEventDefinition]);

  return (
    <FlatContentRow>
      <Header>
        <Button bsStyle="link" className="btn-text" bsSize="xsmall" onClick={toggleOpen}>
          <Icon name={`caret-${open ? 'down' : 'right'}`} />&nbsp;
          {open ? `Hide ${currentTypeText} details` : `Show ${currentTypeText} details`}
        </Button>
      </Header>
      {open && (
      <Container data-testid="info-container">
        <Row>
          {items.map(({ title, content, show }) => (show !== false) && (
            <Item key={title}>
              <b>{title}: </b>
              <span title={title}>{content || <i>`No ${title} provided`</i>}</span>
            </Item>
          ))}
        </Row>
      </Container>
      )}
    </FlatContentRow>
  );
};

export default EventInfoBar;
