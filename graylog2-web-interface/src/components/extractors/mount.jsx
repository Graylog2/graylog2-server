import React from 'react';
import ReactDOM from 'react-dom';
import ExtractorExampleMessage from './ExtractorExampleMessage';
import AddExtractor from './AddExtractor';

const extractorExampleMessage = document.getElementById('react-extractor-example-message');
if (extractorExampleMessage) {
  const example = extractorExampleMessage.getAttribute('data-example');
  const field = extractorExampleMessage.getAttribute('data-field');
  ReactDOM.render(<ExtractorExampleMessage field={field} example={example}/>, extractorExampleMessage);
}

const addExtractorElement = document.getElementById('react-add-extractor');
if (addExtractorElement) {
  const inputId = addExtractorElement.getAttribute('data-input-id');
  ReactDOM.render(<AddExtractor inputId={inputId}/>, addExtractorElement);
}
