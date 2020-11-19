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
// @flow strict
import * as React from 'react';
import uuid from 'uuid/v4';
import { PluginStore } from 'graylog-web-plugin/plugin';
import styled from 'styled-components';
import { sortBy, isEmpty } from 'lodash';

import { Button } from 'components/graylog';
import { ViewStore } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';

import SectionInfo from '../SectionInfo';
import SectionSubheadline from '../SectionSubheadline';

const Group = styled.div`
  margin-bottom: 20px;

  :last-child {
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
};

type State = {
  overflowingComponents: { [key: string]: React.ReactNode },
};

export type CreatorProps = {
  view: View,
};
type CreatorType = 'preset' | 'generic';
type CreatorFunction = (CreatorProps) => React.ReactNode | undefined | null | void;

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

type Creator = ComponentCreator | FunctionalCreator;

const isCreatorFunc = (creator: Creator): creator is FunctionalCreator => ('func' in creator);

class AddWidgetButton extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);

    this.state = {
      overflowingComponents: {},
    };
  }

  _createHandlerFor = (creator: Creator): CreatorFunction => {
    const { onClick } = this.props;
    const { view } = ViewStore.getInitialState();

    if (isCreatorFunc(creator)) {
      return () => {
        onClick();

        creator.func({ view });
      };
    }

    if (creator.component) {
      const CreatorComponent = creator.component;

      return () => {
        const id = uuid();
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

  _createMenuItem = (creator: Creator): React.ReactNode => (
    <CreateButton key={creator.title}
                  onClick={this._createHandlerFor(creator)}
                  disabled={creator.condition ? !creator.condition() : false}>
      {creator.title}
    </CreateButton>
  );

  _createGroup = (creators: Array<Creator>, type: 'preset' | 'generic'): React.ReactNode => {
    const typeCreators = creators.filter((c) => (c.type === type));
    const sortedCreators = sortBy(typeCreators, 'title');

    return sortedCreators.map(this._createMenuItem);
  }

  render() {
    const { overflowingComponents } = this.state;
    const creators: Array<Creator> = PluginStore.exports('creators');
    const presets = this._createGroup(creators, 'preset');
    const generic = this._createGroup(creators, 'generic');
    // $FlowFixMe: Object.value signature is in the way
    const components: Array<React.ReactNode> = Object.values(overflowingComponents);

    return (
      <>
        <SectionInfo>Use the following options to add an aggregation or parameters (enterprise) to your search.</SectionInfo>
        <Group>
          <SectionSubheadline>Generic</SectionSubheadline>
          {generic}
        </Group>
        <Group>
          <SectionSubheadline>Predefined Aggregation</SectionSubheadline>
          {presets}
        </Group>
        {!isEmpty(components) && (
          <Group>
            <SectionSubheadline>Other</SectionSubheadline>
            {components}
          </Group>
        )}
      </>
    );
  }
}

export default AddWidgetButton;
