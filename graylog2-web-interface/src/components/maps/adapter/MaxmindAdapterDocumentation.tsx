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
/* eslint-disable react/no-unescaped-entities */
import React from 'react';
import { Trans, useTranslation } from 'react-i18next';

import { Alert } from 'components/bootstrap';

const CITY_FIELDS = `{
    "city": { "geoname_id": 5375480, "names": { "en": "Mountain View" } },
    "location": {
      "accuracy_radius": 1000,
      "average_income": null,
      "latitude": 37.386,
      "longitude": -122.0838,
      "metro_code": 807,
      "population_density": null,
      "time_zone": "America/Los_Angeles"
    },
    "postal": { "code": "94035" },
    "subdivisions": [ { "geoname_id": 5332921, "iso_code": "CA", "names": { "en": "California" } } ],
}`;

const COUNTRY_FIELDS = `{
    "continent": { "code": "NA", "geoname_id": 6255149, "names": { "en": "North America" } },
    "country": { "geoname_id": 6252001, "iso_code": "US", "names": { "en": "United States" } },
    "registered_country": { "geoname_id": 6252001, "iso_code": "US", "names": { } },
    "represented_country": { "geoname_id": null, "iso_code": "US", "names": { } },
    "traits": {
      "ip_address": "8.8.8.8",
      "is_anonymous_proxy": false,
      "is_legitimate_proxy": false,
      "is_satellite_provider": false,
      "isp": null,
      "organization": null,
    }
}`;

const ASN_FIELDS = `{
    "as_number": 15169,
    "as_organization": "Google LLC"
}`;

const MaxmindAdapterDocumentation = () => {
  const { t } = useTranslation('common');

  return (
    <div>
      <p>{t('maxmind_adapter.documentation.intro')}</p>

      <Alert style={{ marginBottom: 10 }} bsStyle="info" title={t('maxmind_adapter.documentation.limitations_title')}>
        <p>{t('maxmind_adapter.documentation.limitations_supported')}</p>
        <p>{t('maxmind_adapter.documentation.limitations_more')}</p>
      </Alert>

      <hr />

      <h3 style={{ marginBottom: 10 }}>{t('maxmind_adapter.documentation.asn_fields')}</h3>

      <pre>{ASN_FIELDS}</pre>

      <h3 style={{ marginBottom: 10 }}>{t('maxmind_adapter.documentation.country_fields')}</h3>

      <pre>{COUNTRY_FIELDS}</pre>

      <h3 style={{ marginBottom: 10 }}>{t('maxmind_adapter.documentation.city_fields')}</h3>

      <p>{t('maxmind_adapter.documentation.city_additional_fields')}</p>

      <pre>{CITY_FIELDS}</pre>

      <p>
        <Trans
          t={t}
          i18nKey="maxmind_adapter.documentation.complete_documentation"
          components={{
            docsLink: <a href="http://maxmind.github.io/GeoIP2-java/" target="_blank" rel="noopener noreferrer" />,
          }}
        />
      </p>
    </div>
  );
};

export default MaxmindAdapterDocumentation;
