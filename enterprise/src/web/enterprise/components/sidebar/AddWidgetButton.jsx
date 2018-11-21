// @flow
import * as React from 'react';
import uuid from 'uuid/v4';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { DropdownButton, MenuItem } from 'react-bootstrap';

const menuTitle = <React.Fragment><i className="fa fa-plus" />{' '}Create</React.Fragment>;

type Props = {};

type State = {
  overflowingComponents: { [string]: React.Node },
};

type CreatorType = 'preset' | 'generic';
type CreatorFunction = () => ?React.Node;

type FunctionalCreator = {|
  func: CreatorFunction,
  title: string,
  type: CreatorType,
|};

type CreatorComponentProps = {
  onClose: () => void,
};

type ComponentCreator = {|
  component: React.ComponentType<CreatorComponentProps>,
  title: string,
  type: CreatorType,
|};

type Creator = ComponentCreator | FunctionalCreator;

class AddWidgetButton extends React.Component<Props, State> {
  state = {
    overflowingComponents: {},
  };

  _createHandlerFor = (creator: Creator): CreatorFunction => {
    if (creator.func) {
      return creator.func;
    }
    if (creator.component) {
      const CreatorComponent = creator.component;
      return () => {
        const id = uuid();
        const onClose = () => this.setState((state) => {
          const { overflowingComponents } = state;
          delete overflowingComponents[id];
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
    throw new Error(`Invalid binding for creator: ${creator} - has neither 'func' nor 'component'.`);
  };

  _createMenuItem = (creator: Creator): React.Node => (
    <MenuItem key={creator.title} onSelect={this._createHandlerFor(creator)}>
      {creator.title}
    </MenuItem>
  );

  render() {
    const creators: Array<Creator> = PluginStore.exports('creators');
    const presets = creators.filter(c => (c.type === 'preset'))
      .map(this._createMenuItem);
    const generic = creators.filter(c => (c.type === 'generic'))
      .map(this._createMenuItem);
    const overflowingComponents = Object.values(this.state.overflowingComponents);
    return (
      <React.Fragment>
        <DropdownButton title={menuTitle} id="add-widget-button-dropdown" bsStyle="info" pullRight>
          {presets}
          <MenuItem divider />
          {generic}
        </DropdownButton>
        {overflowingComponents}
      </React.Fragment>
    );
  }
}

export default AddWidgetButton;
