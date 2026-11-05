# Korean glossary

Terms used in the Korean translations (`ko/*.json`), so the whole interface uses the same words. Based on
Microsoft's Korean terminology, which most Korean IT users know from Windows and Azure.

## Style

- Sentences use the formal polite style (합니다/하십시오체), as Microsoft and most enterprise software in Korea do:
  "저장되었습니다.", "다시 시도하십시오.". Avoid 해요체 and casual forms.
- Buttons, menu items, tabs and column titles are short nouns: "만들기", "삭제", "저장", "편집", "취소".
- Use standard spacing (띄어쓰기) between words. Put a space between Korean text and Latin words or numbers where
  natural ("Graylog 서버", "5분"). Keep ASCII punctuation; use "..." where the English has an ellipsis and a colon
  where the English has one.
- Common IT loanwords are written in Hangul, consistently (see the table). Do not mix the Hangul and the English
  spelling of the same term.
- Keep product and feature names in English: Graylog, Enterprise, Sidecar, Forwarder, Fleet, Illuminate, Sigma,
  Watchlist, MITRE ATT&CK, Data Node, Input Profile, Detection Chain, Data Tiering, Content Hub, Warm Tier,
  OpenSearch, Elasticsearch, AWS and other vendor and product names. Fixed names like "Default Stream" stay English.
- Translate only the value. Keep every `{{placeholder}}`, `<tag>` / `</tag>` and `<0>`-style Trans markers exactly
  as they are, plus technical identifiers in `backticks`, field names, query syntax and URLs. Korean word order
  may move a placeholder within the sentence; that is fine.
- Korean has no plural forms: only `_other` keys exist. Use a counter where natural ("{{count}}개", "{{count}}건"
  for events and messages).

## Terms

| English | Korean |
| --- | --- |
| Stream | 스트림 (Default Stream stays English) |
| Input | 입력 |
| Output | 출력 |
| Dashboard | 대시보드 |
| Widget | 위젯 |
| Pipeline | 파이프라인 |
| Extractor | 추출기 |
| Lookup Table | 조회 테이블 |
| Index Set | 인덱스 세트 |
| Index | 인덱스 |
| Node | 노드 |
| Cluster | 클러스터 |
| Collector | 수집기 |
| Content Pack | 콘텐츠 팩 |
| Data Lake | 데이터 레이크 |
| Event | 이벤트 |
| Event Definition | 이벤트 정의 |
| Alert | 경고 |
| Notification | 알림 |
| Report | 보고서 |
| Search (noun / verb) | 검색 / 검색 |
| Query | 쿼리 |
| Message | 메시지 |
| Field | 필드 |
| Source | 소스 |
| Timestamp | 타임스탬프 |
| Log | 로그 |
| Time range | 시간 범위 |
| All time | 전체 기간 |
| Throughput | 처리량 |
| Shard / Replica | 샤드 / 복제본 |
| Journal | 저널 |
| Buffer | 버퍼 |
| Processing | 처리 |
| Parser | 파서 |
| Rule / Stream rule | 규칙 / 스트림 규칙 |
| Archive | 아카이브 |
| Audit log | 감사 로그 |
| Retention | 보존 |
| Anomaly | 이상 징후 |
| Asset | 자산 |
| Investigation | 조사 |
| Security | 보안 (as a product name, e.g. "{{productName}} Security", stays English) |
| User / Role / Team | 사용자 / 역할 / 팀 |
| Permission | 권한 |
| Token | 토큰 |
| Certificate | 인증서 |
| Authentication | 인증 |
| Backend | 백엔드 |
| Configuration | 구성 |
| Settings | 설정 |
| Preferences | 환경 설정 |
| License | 라이선스 |
| Entity, item (in lists and tables) | 항목 |
| Column | 열 |
| Filter (noun / verb) | 필터 / 필터링 |
| Bulk actions | 일괄 작업 |
| Preview | 미리 보기 |
| Refresh | 새로 고침 |
| Create / Delete / Edit / Save / Cancel | 만들기 / 삭제 / 편집 / 저장 / 취소 |
| Default | 기본값 |
| Preset | 프리셋 |
| Aggregation / Histogram | 집계 / 히스토그램 |
| Slice / Slice by | 분할 / 분할 기준 |
| Backup | 백업 |
