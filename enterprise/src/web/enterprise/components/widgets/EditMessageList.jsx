// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Row, Col } from 'react-bootstrap';
import * as Immutable from 'immutable';

// $FlowFixMe: imports from core need to be fixed in flow
import Select from 'components/common/Select';

import CustomPropTypes from 'enterprise/components/CustomPropTypes';
import { defaultCompare } from 'enterprise/logic/DefaultCompare';
import MessagesWidgetConfig from 'enterprise/logic/widgets/MessagesWidgetConfig';
import FieldTypeMapping from 'enterprise/logic/fieldtypes/FieldTypeMapping';

import DescriptionBox from '../aggregationbuilder/DescriptionBox';

const _onFieldSelectionChanged = (fields, config, onChange) => {
  const newFields = fields.split(',');
  const newConfig = config.toBuilder().fields(newFields).build();
  return onChange(newConfig);
};

const _renderChildrenWithContainerHeight = (children, containerHeight) => React.Children.map(children, child => React.cloneElement(child, { containerHeight }));

type Props = {
  children: React.Node,
  config: MessagesWidgetConfig,
  containerHeight: number,
  fields: Immutable.Set<FieldTypeMapping>,
  onChange: (MessagesWidgetConfig) => void,
};

const EditMessageList = ({ children, config, containerHeight, fields, onChange }: Props) => {
  const fieldsForSelect = fields
    .map(fieldType => fieldType.name)
    .map(fieldName => ({ label: fieldName, value: fieldName }))
    .valueSeq()
    .toJS()
    .sort((v1, v2) => defaultCompare(v1.label, v2.label));
  const selectedFieldsForSelect = Immutable.Set(config.fields).join(',');

  return (
    <Row>
      <Col md={3}>
        <DescriptionBox description="Fields">
          <Select options={fieldsForSelect}
                  onChange={newFields => _onFieldSelectionChanged(newFields, config, onChange)}
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
  onChange: PropTypes.func.isRequired,
};

export default EditMessageList;
