import React, {PropTypes} from 'react';
import {Input, Button, Panel} from 'react-bootstrap';
import {LinkContainer} from 'react-router-bootstrap';

import DocumentationLink from 'components/support/DocumentationLink';
import CopyInputExtractorConfiguration from './extractors_configuration/CopyInputExtractorConfiguration';

import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';

import ExtractorUtils from 'util/ExtractorUtils';

const EditExtractorConfiguration = React.createClass({
  propTypes: {
    extractorType: PropTypes.oneOf(ExtractorUtils.EXTRACTOR_TYPES).isRequired,
    configuration: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  },
  render() {
    let control;
    const controls = [];
    let helpMessage;

    switch (this.props.extractorType) {
    case 'copy_input':
      control = <CopyInputExtractorConfiguration/>;
      break;
    case 'grok':
      helpMessage = (
        <span>
          Matches the field against the current Grok pattern list, use <b>{'%{PATTERN-NAME}'}</b> to refer to a{' '}
          <LinkContainer to={Routes.SYSTEM.GROKPATTERNS}><a>stored pattern</a></LinkContainer>.
        </span>
      );
      controls.push(
        <div key="grokControls">
          <Input type="text" id="grok_pattern" label="Grok pattern" labelClassName="col-md-2"
                 wrapperClassName="col-md-10"
                 defaultValue={this.props.configuration.grok_pattern}
                 onChange={this.props.onChange('grok_pattern')}
                 help={helpMessage}/>
        </div>
      );

      // TODO: try
      break;
    case 'json':
      controls.push(
        <div key="jsonControls">
          <Input type="checkbox"
                 id="flatten"
                 label="Flatten structures"
                 wrapperClassName="col-md-offset-2 col-md-10"
                 defaultChecked={this.props.configuration.flatten}
                 onChange={this.props.onChange('flatten')}
                 help="Whether to flatten JSON objects into a single message field or to expand into multiple fields."/>

          <Input type="text"
                 id="list_separator"
                 label="List item separator"
                 labelClassName="col-md-2"
                 wrapperClassName="col-md-10"
                 defaultValue={this.props.configuration.list_separator}
                 required
                 onChange={this.props.onChange('list_separator')}
                 help="What string to use to concatenate items of a JSON list."/>

          <Input type="text"
                 id="key_separator"
                 label="Key separator"
                 labelClassName="col-md-2"
                 wrapperClassName="col-md-10"
                 defaultValue={this.props.configuration.key_separator}
                 required
                 onChange={this.props.onChange('key_separator')}
                 help={<span>What string to use to concatenate different keys of a nested JSON object (only used if <em>not</em> flattened).</span>}/>

          <Input type="text"
                 id="kv_separator"
                 label="Key/value separator"
                 labelClassName="col-md-2"
                 wrapperClassName="col-md-10"
                 defaultValue={this.props.configuration.kv_separator}
                 required
                 onChange={this.props.onChange('kv_separator')}
                 help="What string to use when concatenating key/value pairs of a JSON object (only used if flattened)."/>
        </div>
      );

      // TODO: try
      break;
    case 'regex':
      helpMessage = (
        <span>
          The regular expression used for extraction. First matcher group is used.{' '}
          Learn more in the <DocumentationLink page={DocsHelper.PAGES.EXTRACTORS} text="documentation"/>.
        </span>
      );
      controls.push(
        <div key="regexControls">
          <Input type="text"
                 id="regex_value"
                 label="Regular expression"
                 labelClassName="col-md-2"
                 placeholder="^.*string(.+)$"
                 onChange={this.props.onChange('regex_value')}
                 wrapperClassName="col-md-10"
                 defaultValue={this.props.configuration.regex_value}
                 required
                 help={helpMessage}/>
        </div>
      );

      // TODO: try
      break;
    case 'regex_replace':
      helpMessage = (
        <span>
          The regular expression used for extraction.{' '}
          Learn more in the <DocumentationLink page={DocsHelper.PAGES.EXTRACTORS} text="documentation"/>.
        </span>
      );
      controls.push(
        <div key="regexReplaceControls">
          <Input type="text"
                 id="regex"
                 label="Regular expression"
                 labelClassName="col-md-2"
                 placeholder="^.*string(.+)$"
                 onChange={this.props.onChange('regex')}
                 wrapperClassName="col-md-10"
                 defaultValue={this.props.configuration.regex}
                 required
                 help={helpMessage}/>

          <Input type="text"
                 id="replacement"
                 label="Replacement"
                 labelClassName="col-md-2"
                 placeholder="$1"
                 onChange={this.props.onChange('replacement')}
                 wrapperClassName="col-md-10"
                 defaultValue={this.props.configuration.replacement}
                 required
                 help={<span>The replacement used for the matching text. Please refer to the{' '}
                 <a target="_blank" href="https://docs.oracle.com/javase/7/docs/api/java/util/regex/Matcher.html#replaceAll(java.lang.String)">Matcher</a>{' '}
                  API documentation for the possible options.</span>}/>

          <Input type="checkbox"
                 id="replace_all"
                 label="Replace all occurrences of the pattern"
                 wrapperClassName="col-md-offset-2 col-md-10"
                 defaultChecked={this.props.configuration.replace_all}
                 onChange={this.props.onChange('replace_all')}
                 help="Whether to replace all occurrences of the given pattern or only the first occurrence."/>
        </div>
      );

      // TODO: try
      break;
    case 'substring':
      controls.push(
        <div key="substringControls">
          <Input type="number"
                 id="begin_index"
                 label="Begin index"
                 labelClassName="col-md-2"
                 wrapperClassName="col-md-10"
                 defaultValue={this.props.configuration.begin_index}
                 onChange={this.props.onChange('begin_index')}
                 min="0"
                 required
                 help="Character position from where to start extracting. (Inclusive)"/>

          <Input type="number"
                 id="end_index"
                 label="End index"
                 labelClassName="col-md-2"
                 wrapperClassName="col-md-10"
                 defaultValue={this.props.configuration.end_index}
                 onChange={this.props.onChange('end_index')}
                 required
                 help={<span>Where to end extracting. (Exclusive) <strong>Example:</strong> <em>1,5</em> cuts <em>love</em> from the string <em>ilovelogs</em>.</span>}/>
        </div>
      );

      // TODO: try
      break;
    case 'split_and_index':
      controls.push(
        <div key="splitAndIndexControls">
          <Input type="text"
                 id="split_by"
                 label="Split by"
                 labelClassName="col-md-2"
                 wrapperClassName="col-md-10"
                 defaultValue={this.props.configuration.split_by}
                 onChange={this.props.onChange('split_by')}
                 required
                 help={<span>What character to split on. <strong>Example:</strong> A whitespace character will split <em>foo bar baz</em> to <em>[foo,bar,baz]</em>.</span>}/>

          <Input type="number"
                 id="index"
                 label="Target index"
                 labelClassName="col-md-2"
                 wrapperClassName="col-md-10"
                 defaultValue={this.props.configuration.index}
                 onChange={this.props.onChange('index')}
                 required
                 help={<span>What part of the split string to you want to use? <strong>Example:</strong> <em>2</em> selects <em>bar</em> from <em>foo bar baz</em> when split by whitespace.</span>}/>
        </div>
      );

      // TODO: try
      break;
    default:
      console.warn(`Unsupported extractor type ${this.props.extractorType}`);
    }

    return <div>{control || controls}</div>;
  },
});

export default EditExtractorConfiguration;
