// @flow strict
import * as React from 'react';
import uuid from 'uuid/v4';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { ButtonGroup, Button } from 'components/graylog';
import { ViewStore } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';

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
    <Button key={creator.title}
            onClick={this._createHandlerFor(creator)}
            disabled={creator.condition ? !creator.condition() : false}>
      {creator.title}
    </Button>
  );

  render() {
    const creators: Array<Creator> = PluginStore.exports('creators');
    const presets = creators.filter(c => (c.type === 'preset'))
      .map(this._createMenuItem);
    const generic = creators.filter(c => (c.type === 'generic'))
      .map(this._createMenuItem);
    const { overflowingComponents } = this.state;
    // $FlowFixMe: Object.value signature is in the way
    const components: Array<React.Node> = Object.values(overflowingComponents);
    return (
      <React.Fragment>
        <ButtonGroup vertical block>
          {presets}
          {generic}
        </ButtonGroup>
        {components}
      </React.Fragment>
    );
  }
}

export default AddWidgetButton;
