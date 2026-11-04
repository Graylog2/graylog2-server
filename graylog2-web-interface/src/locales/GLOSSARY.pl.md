# Polish glossary

Terms used in the Polish translations (`pl/*.json`), so the whole interface uses the same words.

## Style

- Match the existing Polish translations: address the user directly in the second person where the English does ("możesz", "chcesz"); buttons and menu items use the imperative or infinitive ("Utwórz", "Usuń").
- Keep product and feature names in English: Graylog, Enterprise, Data Lake, Security, Stream, Dashboard, Widget, Input, Output, Extractor, Pipeline, Sidecar, Content Pack, Index Set, Lookup Table, Collector, Fleet, Event Definition, Forwarder, Illuminate, Sigma, Watchlist, MITRE ATT&CK, Investigation, Asset, Detection Chain, Data Tiering, Input Profile, Content Hub.
- Translate only the value. Keep every `{{placeholder}}`, `<tag>` / `</tag>` and `<0>`-style Trans markers exactly as they are, plus technical identifiers in `backticks`.
- Plural keys: `_one` = 1, `_few` = 2-4 (and 22-24 ...), `_many` = 5+ / 0, `_other` = fractions. The script gives the English plural text as hint for `_few/_many/_other`.

- Terminology follows the dominant usage of the catalog: Polish, correctly inflected, for Stream (strumień), Input (wejście), Output (wyjście), Node (węzeł), Fleet (flota), Index Set (zestaw indeksów), Event Definition (definicja zdarzenia), Widget (widżet), Extractor (ekstraktor), Lookup Table (tabela wyszukiwania), Notification (powiadomienie), Report (raport); English for Dashboard, Pipeline, Sidecar, Collector, Forwarder, Asset, Collection, Alert, Investigation and for fixed names like Data Node, Data Lake, Input Profile, Default Stream.

## Terms

| English | Polish |
| --- | --- |
| Alert | Alert |
| All time | Cały czas |
| Bulk actions | Akcje zbiorcze |
| Column | Kolumna |
| Create | Utwórz |
| Default | Domyślny |
| Delete | Usuń |
| Entity, item (in lists and tables) | Element |
| Filter (noun / verb) | Filtr / Filtruj |
| Hide / Show (columns, sections) | Ukryj / Pokaż |
| Preferences | Preferencje |
| Preset (e.g. time range) | Szablon |
| Preview | Podgląd |
| Refresh | Odśwież |
| Search (noun / verb) | Wyszukiwanie / Szukaj |
| Settings | Ustawienia |
| Slice | Wycinek |
| Slicing / Slice by | Podział / Podziel według |
| Submit | Wyślij |
| Time range | Zakres czasu |
| Update (save changes) | Zaktualizuj |
| Archive | Archiwum |
| Audit log | Dziennik audytu |
| Report | Raport |
| Retention | Retencja |
| Risk score | Wynik ryzyka |
| Anomaly | Anomalia |
