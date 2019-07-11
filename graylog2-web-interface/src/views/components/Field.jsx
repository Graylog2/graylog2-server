// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import FieldType from 'views/logic/fieldtypes/FieldType';

import CustomPropTypes from './CustomPropTypes';
import FieldActions from './actions/FieldActions';

type Props = {
  children?: React.Node,
  disabled?: boolean,
  name: string,
  menuContainer: ?HTMLElement,
  queryId: string,
  type: FieldType,
}

type State = {
  open: boolean,
}

export default class Field extends React.Component<Props, State> {
  static propTypes = {
    children: PropTypes.node,
    disabled: PropTypes.bool,
    name: PropTypes.string.isRequired,
    menuContainer: PropTypes.object,
    queryId: PropTypes.string.isRequired,
    type: CustomPropTypes.FieldType.isRequired,
  };

  static defaultProps = {
    children: null,
    disabled: false,
    menuContainer: document.body,
  };

  render() {
    const { children, disabled = false, menuContainer, name, queryId, type } = this.props;
    const element = children || name;

    return (
      <FieldActions element={element}
                    disabled={disabled}
                    menuContainer={menuContainer}
                    name={name}
                    type={type}
                    queryId={queryId}>
        {name} = {type.type}
      </FieldActions>
    );
  }
}
