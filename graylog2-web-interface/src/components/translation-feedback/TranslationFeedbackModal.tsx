/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React, { useState } from 'react';
import styled, { css } from 'styled-components';
import { useTranslation } from 'react-i18next';

import i18n from 'i18n';
import { BootstrapModalConfirm, Input } from 'components/bootstrap';
import UserNotification from 'util/UserNotification';
import type { Candidate } from 'util/i18n/renderedTranslations';

import { createTranslationFeedback } from './api';
import type { Category } from './types';
import { CATEGORIES } from './types';

export type FeedbackTarget = {
  text: string;
  candidates: Array<Candidate>;
  language: string;
  path: string;
  domPath: string;
};

const NO_KEY = 'none';

const Source = styled.code(
  ({ theme }) => css`
    display: block;
    white-space: pre-wrap;
    word-break: break-word;
    margin-top: ${theme.spacings.xxs};
  `,
);

const Hint = styled.p(
  ({ theme }) => css`
    color: ${theme.colors.gray[50]};
    font-size: ${theme.fonts.size.small};
  `,
);

const candidateId = ({ namespace, key }: Candidate) => `${namespace}:${key}`;

const resource = (language: string, namespace: string, key: string) => {
  const value = i18n.getResource(language, namespace, key);

  return typeof value === 'string' ? value : undefined;
};

const isMissingTranslation = (candidate: Candidate | undefined, language: string) =>
  !candidate || candidate.usedLanguage !== language;

const defaultCategory = (candidate: Candidate | undefined, language: string): Category =>
  isMissingTranslation(candidate, language) && language !== 'en' ? 'untranslated' : 'wrong';

type Props = {
  target: FeedbackTarget;
  onClose: () => void;
};

const TranslationFeedbackModal = ({ target, onClose }: Props) => {
  const { t } = useTranslation('translation_feedback');
  const { candidates, language } = target;
  // Texts which only partly match a translation are often a coincidence, so the reporter has to pick those.
  const preselected = candidates.find(({ exact }) => exact);
  const [selected, setSelected] = useState<string>(preselected ? candidateId(preselected) : NO_KEY);
  const candidate = candidates.find((other) => candidateId(other) === selected);
  const [text, setText] = useState(target.text);
  const [category, setCategory] = useState<Category>(defaultCategory(preselected, language));
  const [suggestion, setSuggestion] = useState('');
  const [comment, setComment] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const englishText = candidate ? resource('en', candidate.namespace, candidate.key) : undefined;
  const currentTranslation =
    candidate && language !== 'en' ? resource(language, candidate.namespace, candidate.key) : undefined;

  const onSelectCandidate = (id: string) => {
    setSelected(id);
    setCategory(
      defaultCategory(
        candidates.find((other) => candidateId(other) === id),
        language,
      ),
    );
  };

  const onSubmit = () => {
    setIsSubmitting(true);

    createTranslationFeedback({
      language,
      namespace: candidate?.namespace,
      key: candidate?.key,
      text: text.trim() || target.text,
      english_text: englishText,
      suggestion,
      comment,
      category,
      path: target.path,
      dom_path: target.domPath,
    })
      .then(() => {
        UserNotification.success(t('notifications.created'), t('notifications.created_title'));
        onClose();
      })
      .catch((error) => {
        UserNotification.error(t('notifications.create_failed', { error }), t('notifications.create_failed_title'));
        setIsSubmitting(false);
      });
  };

  return (
    <BootstrapModalConfirm
      showModal
      title={t('modal.title')}
      onCancel={onClose}
      onConfirm={onSubmit}
      confirmButtonText={t('modal.submit')}
      confirmButtonDisabled={!text.trim() && !target.text}
      isAsyncSubmit
      isSubmitting={isSubmitting}
      submitLoadingText={t('modal.submitting')}>
      <div data-translation-feedback>
        <Input
          id="translation-feedback-text"
          type="textarea"
          label={t('modal.text')}
          help={t('modal.text_help')}
          rows={2}
          value={text}
          onChange={(event: React.ChangeEvent<HTMLInputElement>) => setText(event.target.value)}
        />

        <Input id="translation-feedback-key" label={t('modal.key')}>
          <>
            {candidates.map((option) => (
              <Input
                key={candidateId(option)}
                id={`translation-feedback-key-${candidateId(option)}`}
                type="radio"
                name="translation-feedback-key"
                value={candidateId(option)}
                checked={selected === candidateId(option)}
                onChange={() => onSelectCandidate(candidateId(option))}
                label={
                  <span>
                    <code>{candidateId(option)}</code>
                    {isMissingTranslation(option, language) && language !== 'en'
                      ? ` (${t('modal.not_translated')})`
                      : ''}
                    {!option.exact ? ` (${t('modal.partial_match')})` : ''}
                  </span>
                }
              />
            ))}
            <Input
              id="translation-feedback-key-none"
              type="radio"
              name="translation-feedback-key"
              value={NO_KEY}
              checked={selected === NO_KEY}
              onChange={() => onSelectCandidate(NO_KEY)}
              label={candidates.length > 0 ? t('modal.none_of_these') : t('modal.no_key_found')}
            />
          </>
        </Input>

        {englishText && (
          <Hint>
            {t('modal.english')}
            <Source>{englishText}</Source>
            {currentTranslation && (
              <>
                {t('modal.current_translation')}
                <Source>{currentTranslation}</Source>
              </>
            )}
          </Hint>
        )}

        <Input
          id="translation-feedback-category"
          type="select"
          label={t('modal.category')}
          value={category}
          onChange={(event: React.ChangeEvent<HTMLInputElement>) => setCategory(event.target.value as Category)}>
          {CATEGORIES.map((option) => (
            <option key={option} value={option}>
              {t(`categories.${option}`)}
            </option>
          ))}
        </Input>

        <Input
          id="translation-feedback-suggestion"
          type="textarea"
          label={t('modal.suggestion')}
          help={t('modal.suggestion_help')}
          rows={2}
          value={suggestion}
          onChange={(event: React.ChangeEvent<HTMLInputElement>) => setSuggestion(event.target.value)}
        />

        <Input
          id="translation-feedback-comment"
          type="textarea"
          label={t('modal.comment')}
          rows={2}
          value={comment}
          onChange={(event: React.ChangeEvent<HTMLInputElement>) => setComment(event.target.value)}
        />
      </div>
    </BootstrapModalConfirm>
  );
};

export default TranslationFeedbackModal;
