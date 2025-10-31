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
import { useContext, useMemo, useState } from 'react';
import styled from 'styled-components';
import sortBy from 'lodash/sortBy';
import upperCase from 'lodash/upperCase';
import groupBy from 'lodash/groupBy';

import { Button } from 'components/bootstrap';
import type View from 'views/logic/views/View';
import generateId from 'logic/generateId';
import type { ViewsDispatch } from 'views/stores/useViewsDispatch';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import type { GetState } from 'views/types';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import usePluginEntities from 'hooks/usePluginEntities';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';

import SectionInfo from '../SectionInfo';
import SectionSubheadline from '../SectionSubheadline';

const Group = styled.div`
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }
`;

const CreateButton = styled(Button)`
  display: block;
  margin: 5px 0;
  width: 100%;
`;

export type CreatorProps = {
  view: View;
};
type CreatorType = 'preset' | 'generic' | 'investigations' | 'events';
type CreatorFunction = () => (dispatch: ViewsDispatch, getState: GetState) => Promise<string>;

type FunctionalCreator = {
  func: CreatorFunction;
  title: string;
  type: CreatorType;
  useCondition?: () => boolean;
};

type CreatorComponentProps = {
  onClose: () => void;
};

type ComponentCreator = {
  component: React.ComponentType<CreatorComponentProps>;
  useCondition?: () => boolean;
  title: string;
  type: CreatorType;
};

export type Creator = ComponentCreator | FunctionalCreator;

type OverflowingComponents = { [key: string]: React.ReactNode };

export const isCreatorFunc = (creator: Creator): creator is FunctionalCreator => 'func' in creator;

const CreateMenuItem = ({
  creator,
  onClick,
  setOverflowingComponents,
}: {
  creator: Creator;
  onClick: () => void;
  setOverflowingComponents: React.Dispatch<React.SetStateAction<OverflowingComponents>>;
}) => {
  const sendTelemetry = useSendTelemetry();
  const dispatch = useViewsDispatch();
  const disabled = creator.useCondition?.() === false;
  const { setWidgetEditing } = useContext(WidgetFocusContext);

  const createHandlerFor = () => {
    const telemetry = () =>
      sendTelemetry(
        TELEMETRY_EVENT_TYPE.SEARCH_WIDGET_CREATE[upperCase(creator.title).replace(/ /g, '_')] ??
          `Search Widget ${creator.title} Created`,
        {
          app_section: 'search-sidebar',
          event_details: {
            widgetType: creator.type,
            widgetTitle: creator.title,
          },
        },
      );

    if (isCreatorFunc(creator)) {
      return () => {
        telemetry();

        onClick();

        dispatch(creator.func()).then((widgetId) => {
          setWidgetEditing(widgetId);
        });
      };
    }

    if (creator.component) {
      const CreatorComponent = creator.component;

      return () => {
        telemetry();

        const id = generateId();

        const onClose = () => {
          setOverflowingComponents((cur) => {
            const newState = { ...cur };

            delete newState[id];

            return newState;
          });

          onClick();
        };

        const renderedComponent = <CreatorComponent key={creator.title} onClose={onClose} />;

        setOverflowingComponents((cur) => {
          const newState = { ...cur };

          newState[id] = renderedComponent;

          return newState;
        });
      };
    }

    throw new Error(`Invalid binding for creator: ${JSON.stringify(creator)} - has neither 'func' nor 'component'.`);
  };

  return (
    <CreateButton key={creator.title} onClick={createHandlerFor()} disabled={disabled}>
      {creator.title}
    </CreateButton>
  );
};

const GroupCreateMenuItems = ({
  creators,
  onClick,
  setOverflowingComponents,
}: {
  creators: Array<Creator>;
  onClick: () => void;
  setOverflowingComponents: React.Dispatch<React.SetStateAction<OverflowingComponents>>;
}) => (
  <>
    {sortBy(creators, 'title').map((creator) => (
      <CreateMenuItem
        creator={creator}
        key={creator.title}
        onClick={onClick}
        setOverflowingComponents={setOverflowingComponents}
      />
    ))}
  </>
);

type Props = {
  onClick: () => void;
};

const AddWidgetButton = ({ onClick }: Props) => {
  const [overflowingComponents, setOverflowingComponents] = useState<OverflowingComponents>({});
  const creators = usePluginEntities('creators');
  const {
    preset: presets,
    generic,
    events: eventsCreator,
    investigations: investigationsCreator,
  } = useMemo(() => groupBy(creators, (creator) => creator.type), [creators]);
  const components: Array<React.ReactNode> = Object.values(overflowingComponents);

  return (
    <>
      <SectionInfo>
        Use the following options to add an aggregation, log view (enterprise feature) or parameters (enterprise
        feature) to your search.
      </SectionInfo>
      <Group>
        <SectionSubheadline>Generic</SectionSubheadline>
        <GroupCreateMenuItems
          creators={generic}
          onClick={onClick}
          setOverflowingComponents={setOverflowingComponents}
        />
      </Group>
      <Group>
        <SectionSubheadline>Predefined Aggregation</SectionSubheadline>
        <GroupCreateMenuItems
          creators={presets}
          onClick={onClick}
          setOverflowingComponents={setOverflowingComponents}
        />
      </Group>
      {!!investigationsCreator?.length && (
        <Group>
          <SectionSubheadline>Investigations</SectionSubheadline>
          <GroupCreateMenuItems
            creators={investigationsCreator}
            onClick={onClick}
            setOverflowingComponents={setOverflowingComponents}
          />
        </Group>
      )}
      {!!eventsCreator?.length && (
        <Group>
          <SectionSubheadline>Events</SectionSubheadline>
          <GroupCreateMenuItems
            creators={eventsCreator}
            onClick={onClick}
            setOverflowingComponents={setOverflowingComponents}
          />
        </Group>
      )}
      {components}
    </>
  );
};

export default AddWidgetButton;
