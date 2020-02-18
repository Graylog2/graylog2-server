// @flow strict
import * as React from 'react';
import uuid from 'uuid/v4';
import { PluginStore } from 'graylog-web-plugin/plugin';
import styled from 'styled-components';
import { sortBy, isEmpty } from 'lodash';

import { Button } from 'components/graylog';
import { ViewStore } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';

const Group = styled.div`
  margin-bottom: 20px;

  :last-child {
    margin-bottom: 0;
  }
`;

const GroupHeadline = styled.h4`
  margin-bottom: 10px;
`;

const CreateButton = styled(Button)`
  display: block;
  margin: 5px 0;
`;

type Props = {
  onClick: () => void,
  toggleAutoClose: () => void,
};

type State = {
  overflowingComponents: { [string]: React.Node },
};

export type CreatorProps = {
  view: View,
};
type CreatorType = 'preset' | 'generic';
type CreatorFunction = (CreatorProps) => ?React.Node;

type FunctionalCreator = {|
  func: CreatorFunction,
  title: string,
  type: CreatorType,
  condition?: () => boolean,
|};

type CreatorComponentProps = {
  onClose: () => void,
};

type ComponentCreator = {|
  component: React.ComponentType<CreatorComponentProps>,
  title: string,
  type: CreatorType,
  condition?: () => boolean,
|};

type Creator = ComponentCreator | FunctionalCreator;

class AddWidgetButton extends React.Component<Props, State> {
  state = {
    overflowingComponents: {},
  };

  _createHandlerFor = (creator: Creator): CreatorFunction => {
    const { onClick, toggleAutoClose } = this.props;
    const { view } = ViewStore.getInitialState();
    if (creator.func) {
      return () => {
        onClick();
        toggleAutoClose();
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
          toggleAutoClose();
          return { overflowingComponents };
        });
        const renderedComponent = <CreatorComponent key={creator.title} onClose={onClose} />;
        this.setState((state) => {
          const { overflowingComponents } = state;
          overflowingComponents[id] = renderedComponent;
          return { overflowingComponents };
        }, () => {
          toggleAutoClose();
        });
      };
    }
    throw new Error(`Invalid binding for creator: ${JSON.stringify(creator)} - has neither 'func' nor 'component'.`);
  };

  _createMenuItem = (creator: Creator): React.Node => (
    <CreateButton key={creator.title}
                  onClick={this._createHandlerFor(creator)}
                  disabled={creator.condition ? !creator.condition() : false}>
      {creator.title}
    </CreateButton>
  );

  _createGroup = (creators: Array<Creator>, type: 'preset' | 'generic'): React.Node => {
    const typeCreators = creators.filter(c => (c.type === type));
    const sortedCreators = sortBy(typeCreators, 'title');
    return sortedCreators.map(this._createMenuItem);
  }

  render() {
    const { overflowingComponents } = this.state;
    const creators: Array<Creator> = PluginStore.exports('creators');
    const presets = this._createGroup(creators, 'preset');
    const generic = this._createGroup(creators, 'generic');
    // $FlowFixMe: Object.value signature is in the way
    const components: Array<React.Node> = Object.values(overflowingComponents);
    return (
      <React.Fragment>
        <Group>
          <GroupHeadline>Generic</GroupHeadline>
          {generic}
        </Group>
        <Group>
          <GroupHeadline>Predefined Aggregation</GroupHeadline>
          {presets}
        </Group>
        {!isEmpty(components) && (
          <Group>
            <GroupHeadline>Other</GroupHeadline>
            {components}
          </Group>
        )}
      </React.Fragment>
    );
  }
}

export default AddWidgetButton;
