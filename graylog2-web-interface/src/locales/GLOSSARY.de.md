# German glossary

Terms used in the German translations (`de/*.json`), so the whole interface uses the same words.

## Style

- Address users formally with "Sie".
- Keep product and feature names in English: Graylog, Enterprise, Data Lake, Security, Stream, Dashboard, Widget, Input, Output, Extractor, Pipeline, Sidecar, Content Pack. Compounds use a hyphen, e.g. "Extractor-Typ", "Output-Typ". "Event" is treated as a German word and is never translated as "Ereignis": compounds are written together ("Eventdetails", "Eventprozedur", "Systemevent"); "Event Definition" is a two-word term (plural "Event Definitionen"); compounds of it keep a hyphen ("Event-Definitionstyp").
- Use German keyboard names in shortcuts: "Strg" instead of "Ctrl", "Umschalt" instead of "Shift".
- Use "..." for ongoing actions like the English texts, e.g. "Wird geladen...".

## Terms

| English                            | German                        |
| ---------------------------------- | ----------------------------- |
| Alert                              | Alarm                         |
| All time                           | Gesamter Zeitraum             |
| Bulk actions                       | Massenaktionen                |
| Column                             | Spalte                        |
| Create                             | Erstellen                     |
| Default                            | Standard                      |
| Delete                             | Löschen                       |
| Entity, item (in lists and tables) | Eintrag                       |
| Event                              | Event (not "Ereignis")        |
| Event definition                   | Event Definition / Event Definitionen |
| Event notification                 | Event Benachrichtigung / Event Benachrichtigungen |
| Collection                         | Collection / Collections (not "Sammlung") |
| Notification                       | Benachrichtigung              |
| Report                             | Report (never "Bericht"); compounds: "Report-Verlauf", "Health-Report" |
| Filter (noun / verb)               | Filter / Filtern              |
| Hide / Show (columns, sections)    | Ausblenden / Einblenden       |
| Preferences                        | Einstellungen                 |
| Preset (e.g. time range)           | Vorlage                       |
| Preview                            | Vorschau                      |
| Refresh                            | Aktualisierung                |
| Search (noun / verb)               | Suche / Durchsuchen           |
| Settings                           | Einstellungen                 |
| Slice                              | Gruppe                        |
| Slicing / Slice by                 | Gruppierung / Gruppieren nach |
| Submit                             | Absenden                      |
| Time range                         | Zeitraum                      |
| Update (save changes)              | Aktualisieren                 |

## Enterprise

Enterprise texts live in `graylog-plugin-enterprise/enterprise/src/web/locales` (namespaces `enterprise_*`) and use the same rules.

- Keep in English: Threat Coverage, Security, Investigation, Asset, Sigma, Detection Chain, Data Lake, Data Tiering, Forwarder, Input Profile, Illuminate, Content Hub, Watchlist, MITRE ATT&CK.

| English         | German           |
| --------------- | ---------------- |
| Anomaly         | Anomalie         |
| Archive         | Archiv           |
| Audit log       | Audit-Log        |
| Report          | Report           |
| Retention       | Aufbewahrung     |
| Risk score      | Risikobewertung  |
| Warm / hot tier | Warm- / Hot-Tier |
