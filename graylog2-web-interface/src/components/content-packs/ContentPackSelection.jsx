import PropTypes from 'prop-types';
import React from 'react';

import { Row, Col } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import FormsUtils from 'util/FormsUtils';
import ObjectUtils from 'util/ObjectUtils';

class ContentPackSelection extends React.Component {
  static propTypes = {
    contentPack: PropTypes.object.isRequired,
    onStateChange: PropTypes.func,
  };

  static defaultProps = {
    onStateChange: () => {},
  };

  constructor(props) {
    super(props);
    this._bindValue = this._bindValue.bind(this);
    this.state = {
      contentPack: ObjectUtils.clone(this.props.contentPack),
    };
  }

  _updateField(name, value) {
    const updatedPack = ObjectUtils.clone(this.state.contentPack);
    updatedPack[name] = value;
    this.props.onStateChange(updatedPack);
    this.setState({ contentPack: updatedPack });
  }

  _bindValue(event) {
    this._updateField(event.target.name, FormsUtils.getValueFromInput(event.target));
  }

  render() {
    return (
      <div>
        <Row>
          <Col lg={8}>
            <h2>General Information</h2>
            <br />
            <form className="form-horizontal content-selection-form" id="content-selection-form" onSubmit={(e) => { e.preventDefault(); }}>
              <fieldset>
                <Input name="name"
                       id="name"
                       type="text"
                       maxLength={250}
                       value={this.state.contentPack.name}
                       onChange={this._bindValue}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       label="Name"
                       help="Give a descriptive name for this content pack."
                       required />
                <Input name="summary"
                       id="summary"
                       type="text"
                       maxLength={250}
                       value={this.state.contentPack.summary}
                       onChange={this._bindValue}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       label="Summary"
                       help="Give a short summary of the content pack."
                       required />
                <Input name="description"
                       id="description"
                       type="textarea"
                       value={this.state.contentPack.description}
                       onChange={this._bindValue}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       label="Description"
                       help="Give a long description of the content pack in markdown."
                       required />
                <Input name="vendor"
                       id="vendor"
                       type="text"
                       maxLength={250}
                       value={this.state.contentPack.vendor}
                       onChange={this._bindValue}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       label="Vendor"
                       help="Who did this content pack and how can he be reached. e.g Name and eMail"
                       required />
                <Input name="url"
                       id="url"
                       type="text"
                       maxLength={250}
                       value={this.state.contentPack.url}
                       onChange={this._bindValue}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       label="URL"
                       help="Where can I find the content pack. e.g. github url"
                       required />
              </fieldset>
            </form>
          </Col>
        </Row>
      </div>
    );
  }
}

export default ContentPackSelection;
