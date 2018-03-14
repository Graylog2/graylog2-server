/* eslint-disable react/no-unescaped-entities */
import React from 'react';
import { Alert } from 'react-bootstrap';

class MaxmindAdapterDocumentation extends React.Component {
  render() {
    const cityFields = `{
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

    const countryFields = `{
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

    return (<div>
      <p>The GeoIP data adapter supports reading MaxMind's GeoIP2 databases.</p>

      <Alert style={{ marginBottom: 10 }} bsStyle="info">
        <h4 style={{ marginBottom: 10 }}>Limitations</h4>
        <p>Currently the city and country databases are supported.</p>
        <p>For support of additional database types, please visit our support channels.</p>
      </Alert>

      <hr />

      <h3 style={{ marginBottom: 10 }}>Country database fields</h3>

      <pre>{countryFields}</pre>

      <h3 style={{ marginBottom: 10 }}>City database fields</h3>

      <p>In addition to the fields provided by the country database, the city database also includes the following fields:</p>

      <pre>{cityFields}</pre>

      <p>For a complete documentation of the fields, please see MaxMind's <a href="http://maxmind.github.io/GeoIP2-java/" target="_blank" rel="noopener noreferrer">developer documentation</a></p>
    </div>);
  }
}

export default MaxmindAdapterDocumentation;
