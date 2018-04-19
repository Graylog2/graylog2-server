import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import { Row, Col, Button } from 'react-bootstrap';

import { BootstrapModalForm, Input } from 'components/bootstrap';
import ObjectUtils from 'util/ObjectUtils';
import FormsUtils from 'util/FormsUtils';

const PipelineForm = createReactClass({
  displayName: 'PipelineForm',

  propTypes: {
    pipeline: PropTypes.object,
    create: PropTypes.bool,
    modal: PropTypes.bool,
    save: PropTypes.func.isRequired,
    validatePipeline: PropTypes.func.isRequired,
    onCancel: PropTypes.func,
  },

  getDefaultProps() {
    return {
      modal: true,
      pipeline: {
        id: undefined,
        title: '',
        description: '',
        stages: [{ stage: 0, rules: [] }],
      },
    };
  },

  getInitialState() {
    const pipeline = ObjectUtils.clone(this.props.pipeline);
    return {
      // when editing, take the pipeline that's been passed in
      pipeline: {
        id: pipeline.id,
        title: pipeline.title,
        description: pipeline.description,
        stages: pipeline.stages,
      },
    };
  },

  openModal() {
    this.modal.open();
  },

  _onChange(event) {
    const pipeline = ObjectUtils.clone(this.state.pipeline);
    pipeline[event.target.name] = FormsUtils.getValueFromInput(event.target);
    this.setState({ pipeline });
  },

  _closeModal() {
    this.modal.close();
  },

  _saved() {
    if (this.props.modal) {
      this._closeModal();
    }

    if (this.props.create) {
      this.setState(this.getInitialState());
    }
  },

  _save(event) {
    if (event) {
      event.preventDefault();
    }

    this.props.save(this.state.pipeline, this._saved);
  },

  render() {
    let triggerButtonContent;
    if (this.props.create) {
      triggerButtonContent = 'Add new pipeline';
    } else {
      triggerButtonContent = 'Edit pipeline details';
    }

    const content = (
      <fieldset>
        <Input type="text"
               id="title"
               name="title"
               label="Title"
               autoFocus
               required
               onChange={this._onChange}
               help="Pipeline name."
               value={this.state.pipeline.title} />

        <Input type="text"
               id="description"
               name="description"
               label="Description"
               onChange={this._onChange}
               help="Pipeline description."
               value={this.state.pipeline.description} />
      </fieldset>
    );

    if (this.props.modal) {
      return (
        <span>
          <Button onClick={this.openModal}
                  bsStyle="success">
            {triggerButtonContent}
          </Button>
          <BootstrapModalForm ref={(modal) => { this.modal = modal; }}
                              title={`${this.props.create ? 'Add new' : 'Edit'} pipeline ${this.state.pipeline.title}`}
                              onSubmitForm={this._save}
                              submitButtonText="Save">
            {content}
          </BootstrapModalForm>
        </span>
      );
    }

    return (
      <form onSubmit={this._save}>
        {content}
        <Row>
          <Col md={12}>
            <Button type="submit" bsStyle="primary" style={{ marginRight: 10 }}>Save</Button>
            <Button type="button" onClick={this.props.onCancel}>Cancel</Button>
          </Col>
        </Row>
      </form>
    );
  },
});

export default PipelineForm;
