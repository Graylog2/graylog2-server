import React from 'react';
import PropTypes from 'prop-types';
import { Row, Col } from 'react-bootstrap';
import Immutable from 'immutable';

import connect from 'stores/connect';
import Select from 'components/common/Select';
import { WidgetActions } from 'enterprise/stores/WidgetStore';
import { SelectedFieldsActions, SelectedFieldsStore } from 'enterprise/stores/SelectedFieldsStore';
import CustomPropTypes from 'enterprise/components/CustomPropTypes';
import { defaultCompare} from 'enterprise/logic/DefaultCompare';
import DescriptionBox from '../aggregationbuilder/DescriptionBox';

const _onFieldSelectionChanged = (fields, config, id) => {
  const newFields = fields.split(',');
  if (config) {
    const newConfigBuilder = config.toBuilder();
    const newConfig = newConfigBuilder.fields(newFields).build();
    WidgetActions.updateConfig(id, newConfig);
  } else {
    SelectedFieldsActions.set(newFields);
  }
};

const _renderChildrenWithContainerHeight = (children, containerHeight) => React.Children.map(children, child => React.cloneElement(child, { containerHeight }));

const EditMessageList = ({ children, config, containerHeight, fields, id, selectedFields }) => {
  const fieldsForSelect = fields
    .map(fieldType => fieldType.name)
    .map(fieldName => ({ label: fieldName, value: fieldName }))
    .valueSeq()
    .toJS()
    .sort((v1, v2) => defaultCompare(v1.label, v2.label));
  const selectedFieldsForSelect = (config ? Immutable.Set(config.fields) : selectedFields).join(',');

  return (
    <Row>
      <Col md={3}>
        <DescriptionBox description="Fields">
          <Select options={fieldsForSelect}
                  onChange={newFields => _onFieldSelectionChanged(newFields, config, id)}
                  value={selectedFieldsForSelect}
                  multi
          />
        </DescriptionBox>
      </Col>
      <Col md={9}>
        {_renderChildrenWithContainerHeight(children, containerHeight)}
      </Col>
    </Row>
  );
};

EditMessageList.propTypes = {
  children: CustomPropTypes.OneOrMoreChildren.isRequired,
  config: PropTypes.shape({
    fields: PropTypes.arrayOf(PropTypes.string),
  }).isRequired,
  containerHeight: PropTypes.number.isRequired,
  fields: CustomPropTypes.FieldListType.isRequired,
  id: PropTypes.string,
  selectedFields: CustomPropTypes.FieldListType,
};

EditMessageList.defaultProps = {
  id: undefined,
  selectedFields: undefined,
};

export default connect(EditMessageList, { selectedFields: SelectedFieldsStore });
