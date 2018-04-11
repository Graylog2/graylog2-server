import PropTypes from 'prop-types';
import React from 'react';

import { Row, Col, Button } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import DataTable from 'components/common/DataTable';
import FormsUtils from 'util/FormsUtils';
import ObjectUtils from 'util/ObjectUtils';

class ContentPackParameters extends React.Component {
  static propTypes = {
    contentPack: PropTypes.object.isRequired,
    onStateChange: PropTypes.func,
  };

  static defaultProps = {
    onStateChange: () => {},
  };

  static emptyParameter = {
    name: '',
    title: '',
    description: '',
    value_type: 'string',
    default_value: '',
  };

  constructor(props) {
    super(props);
    this.state = {
      newParameter: ObjectUtils.clone(ContentPackParameters.emptyParameter),
    };

    this._addNewParameter = this._addNewParameter.bind(this);
    this._bindValue = this._bindValue.bind(this);
  }

  _validateParameter() {
    const param = this.state.newParameter;
    if (!param.name || !param.title || !param.description) {
      return false;
    }
    return true;
  }

  _updateField(name, value) {
    const updatedParameter = ObjectUtils.clone(this.state.newParameter);
    updatedParameter[name] = value;
    this.setState({ newParameter: updatedParameter });
  }

  _bindValue(event) {
    this._updateField(event.target.name, FormsUtils.getValueFromInput(event.target));
  }

  _addNewParameter(e) {
    e.preventDefault();
    if (!this._validateParameter()) {
      return;
    }
    const newContentPack = ObjectUtils.clone(this.props.contentPack);
    newContentPack.parameters.push(this.state.newParameter);
    this.props.onStateChange({ contentPack: newContentPack });
    this.setState({ newParameter: ObjectUtils.clone(ContentPackParameters.emptyParameter) });
  }

  _deleteParameter = (parameter) => {
    const newContentPack = ObjectUtils.clone(this.props.contentPack);
    const indexToDelete = newContentPack.parameters.map(p => p.name).indexOf(parameter.name);
    if (indexToDelete < 0) {
      return;
    }
    newContentPack.parameters.splice(indexToDelete, 1);
    this.props.onStateChange(newContentPack);
  };


  _parameterRowFormater = (parameter) => {
    return (
      <tr key={parameter.name}>
        <td>{parameter.title}</td>
        <td>{parameter.name}</td>
        <td>{parameter.description}</td>
        <td>{parameter.value_type}</td>
        <td>{parameter.default_value}</td>
        <td><Button bsStyle="primary" onClick={() => { this._deleteParameter(parameter); }}>Delete</Button></td>
      </tr>
    );
  };

  render() {
    return (
      <div>
        <Row>
          <Col lg={8}>
            <h2>Create parameters</h2>
            <br />
            <form className="form-horizontal content-selection-form" id="content-selection-form" onSubmit={this._addNewParameter}>
              <fieldset>
                <Input name="title"
                       id="title"
                       type="text"
                       maxLength={250}
                       value={this.state.newParameter.title}
                       onChange={this._bindValue}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       label="Title"
                       help="Give a descriptive title for this content pack."
                       required />
                <Input name="name"
                       id="name"
                       type="text"
                       maxLength={250}
                       value={this.state.newParameter.name}
                       onChange={this._bindValue}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       label="Name"
                       help="This is used as the parameter reference and must not contain a space."
                       required />
                <Input name="description"
                       id="description"
                       type="text"
                       maxLength={250}
                       value={this.state.newParameter.description}
                       onChange={this._bindValue}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       label="Description"
                       help="Give a description explaining what will be done with this parameter."
                       required />
                <Input name="value_type"
                       id="value_type"
                       type="select"
                       maxLength={250}
                       value={this.state.newParameter.value_type}
                       onChange={this._bindValue}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       label="Value Type"
                       help="Give the type of the parameter."
                       required>
                  <option value="string">String</option>
                  <option value="integer">Integer</option>
                  <option value="ipv4Address">IPv4 Address</option>
                  <option value="ipv6Address">IPv6 Address</option>
                  <option value="port">port</option>
                </Input>
                <Input name="default_value"
                       id="default_value"
                       type="text"
                       maxLength={250}
                       value={this.state.newParameter.default_value}
                       onChange={this._bindValue}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       label="Default value"
                       help="Give a default value if the parameter is not optional." />
                <Button bsStyle="info" type="submit">Add Parameter</Button>
              </fieldset>
            </form>
          </Col>
        </Row>
        <Row>
          <Col>
            <h2>Parameters list</h2>
            <br />
            <DataTable
              id="parameter-list"
              headers={['Title', 'Name', 'Description', 'Value Type', 'Default Value']}
              headerCellFormatter={header => <th>{header}</th>}
              sortByKey="title"
              filterKeys={[]}
              rows={this.props.contentPack.parameters}
              dataRowFormatter={this._parameterRowFormater}
            />
          </Col>
        </Row>
      </div>
    );
  }
}

export default ContentPackParameters;
