// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import FieldType from 'views/logic/fieldtypes/FieldType';

import CustomPropTypes from './CustomPropTypes';
import FieldActions from './actions/FieldActions';
import InteractiveContext from './contexts/InteractiveContext';

type Props = {|
  children?: React.Node,
  disabled?: boolean,
  name: string,
  menuContainer: ?HTMLElement,
  queryId: string,
  type: FieldType,
|}

const Field = ({ children, disabled = false, menuContainer, name, queryId, type }: Props) => (
  <InteractiveContext.Consumer>
    {(interactive) => (interactive
      ? (
        <FieldActions element={children || name}
                      disabled={!interactive || disabled}
                      menuContainer={menuContainer}
                      name={name}
                      type={type}
                      queryId={queryId}>
          {name} = {type.type}
        </FieldActions>
      )
      : <span>{name}</span>)}
  </InteractiveContext.Consumer>
);

Field.propTypes = {
  children: PropTypes.node,
  disabled: PropTypes.bool,
  name: PropTypes.string.isRequired,
  menuContainer: PropTypes.object,
  queryId: PropTypes.string,
  type: CustomPropTypes.FieldType.isRequired,
};

Field.defaultProps = {
  children: null,
  disabled: false,
  menuContainer: document.body,
  queryId: undefined,
};

export default Field;
