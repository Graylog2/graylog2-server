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
import { PluginStore } from 'graylog-web-plugin/plugin';
import styled from 'styled-components';
import sortBy from 'lodash/sortBy';
import upperCase from 'lodash/upperCase';
import type { Location } from 'history';

import { Button } from 'components/bootstrap';
import type View from 'views/logic/views/View';
import generateId from 'logic/generateId';
import type { AppDispatch } from 'stores/useAppDispatch';
import useAppDispatch from 'stores/useAppDispatch';
import type { GetState } from 'views/types';
import withTelemetry from 'logic/telemetry/withTelemetry';
import type { EventType } from 'logic/telemetry/Constants';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { TelemetryEventType, TelemetryEvent } from 'logic/telemetry/TelemetryContext';
import { getPathnameWithoutId } from 'util/URLUtils';
import withLocation from 'routing/withLocation';

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

type Props = {
  onClick: () => void,
  sendTelemetry: (eventType: TelemetryEventType | EventType, event: TelemetryEvent) => void,
  location: Location
};

type State = {
  overflowingComponents: { [key: string]: React.ReactNode },
};

export type CreatorProps = {
  view: View,
};
type CreatorType = 'preset' | 'generic';
type CreatorFunction = () => (dispatch: AppDispatch, getState: GetState) => unknown;

type FunctionalCreator = {
  func: CreatorFunction,
  title: string,
  type: CreatorType,
  condition?: () => boolean,
};

type CreatorComponentProps = {
  onClose: () => void,
};

type ComponentCreator = {
  component: React.ComponentType<CreatorComponentProps>,
  condition?: () => boolean,
  title: string,
  type: CreatorType,
};

export type Creator = ComponentCreator | FunctionalCreator;

export const isCreatorFunc = (creator: Creator): creator is FunctionalCreator => ('func' in creator);

const WithDispatch = ({ children }: { children: (dispatch: AppDispatch) => JSX.Element }) => {
  const dispatch = useAppDispatch();

  return children(dispatch);
};

class AddWidgetButton extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);

    this.state = {
      overflowingComponents: {},
    };
  }

  _createHandlerFor = (dispatch: AppDispatch, creator: Creator): () => void => {
    const { onClick, sendTelemetry, location } = this.props;

    if (isCreatorFunc(creator)) {
      return () => {
        sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_WIDGET_CREATE[upperCase(creator.title).replace(/ /g, '_')], {
          app_pathname: getPathnameWithoutId(location.pathname),
          app_section: 'search-sidebar',
          event_details: {
            widgetType: creator.type,
          },
        });

        onClick();

        dispatch(creator.func());
      };
    }

    if (creator.component) {
      const CreatorComponent = creator.component;

      return () => {
        const id = generateId();
        const onClose = () => this.setState((state) => {
          const { overflowingComponents } = state;

          delete overflowingComponents[id];

          onClick();

          return { overflowingComponents };
        });
        const renderedComponent = <CreatorComponent key={creator.title} onClose={onClose} />;

        this.setState((state) => {
          const { overflowingComponents } = state;

          overflowingComponents[id] = renderedComponent;

          return { overflowingComponents };
        });
      };
    }

    throw new Error(`Invalid binding for creator: ${JSON.stringify(creator)} - has neither 'func' nor 'component'.`);
  };

  _createMenuItem = (creator: Creator): React.ReactNode => {
    const disabled = creator.condition?.() === false;

    return (
      <WithDispatch key={creator.title}>
        {(dispatch) => (
          <CreateButton key={creator.title}
                        onClick={this._createHandlerFor(dispatch, creator)}
                        disabled={disabled}>
            {creator.title}
          </CreateButton>
        )}
      </WithDispatch>
    );
  };

  _createGroup = (creators: Array<Creator>, type: 'preset' | 'generic' | 'investigations'): React.ReactNode => {
    const typeCreators = creators.filter((c) => (c.type === type));
    const sortedCreators = sortBy(typeCreators, 'title');

    return sortedCreators.map(this._createMenuItem);
  };

  render() {
    const { overflowingComponents } = this.state;
    const creators = PluginStore.exports('creators');
    const presets = this._createGroup(creators, 'preset');
    const generic = this._createGroup(creators, 'generic');
    const investigations = this._createGroup(creators, 'investigations');
    console.log({ creators });
    const components: Array<React.ReactNode> = Object.values(overflowingComponents);

    return (
      <>
        <SectionInfo>Use the following options to add an aggregation, log view (enterprise feature) or parameters
          (enterprise feature) to your search.
        </SectionInfo>
        <Group>
          <SectionSubheadline>Generic</SectionSubheadline>
          {generic}
        </Group>
        <Group>
          <SectionSubheadline>Predefined Aggregation</SectionSubheadline>
          {presets}
        </Group>
        {!!React.Children.count(investigations) && (
          <Group>
            <SectionSubheadline>Investigations</SectionSubheadline>
            {investigations}
          </Group>
        )}
        {components}
      </>
    );
  }
}

export default withLocation(withTelemetry(AddWidgetButton));
