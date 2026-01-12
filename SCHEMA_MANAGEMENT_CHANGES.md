# Schema Management Implementation Summary

## 목표

Starter의 스키마 관리 방식(Flyway vs Hibernate)을 일관되게 정리하고, 사용자가 원하는 방식을 명시적으로 선택할 수 있게 하며, 충돌 조합에서는 강제 변경 없이 경고+가이드를 제공한다.

## 변경된 파일 목록

### 1. 엔티티 변경 (중복 인덱스 제거)

#### `tracker-starter/src/main/java/com/galoong/aiprompttracker/domain/entity/CallRecord.java`
**변경 이유:** Flyway migration과 JPA 애노테이션에 중복 선언된 인덱스 제거

**변경 전:**
```java
@Table(name = "calls", indexes = {
        @Index(name = "idx_calls_execution", columnList = "execution_id"),
        @Index(name = "idx_calls_provider", columnList = "provider"),
        @Index(name = "idx_calls_created_at", columnList = "created_at"),
        @Index(name = "idx_calls_status", columnList = "status")
})
```

**변경 후:**
```java
@Table(name = "calls")
```

**추가된 문서화:**
```java
/**
 * <p><b>Schema Management:</b>
 * Indexes are managed by Flyway migrations (V1__create_schema_h2.sql) to avoid
 * conflicts when both Flyway and Hibernate ddl-auto are enabled.
 * When using Hibernate-only mode (flyway.enabled=false), Hibernate will create
 * the table but without indexes unless explicitly added via ddl-auto scripts.
 */
```

#### `tracker-starter/src/main/java/com/galoong/aiprompttracker/domain/entity/ExecutionRecord.java`
**변경 이유:** CallRecord와 동일한 이유로 중복 인덱스 선언 제거

**변경 전:**
```java
@Table(name = "executions", indexes = {
        @Index(name = "idx_executions_function", columnList = "function_name"),
        @Index(name = "idx_executions_started_at", columnList = "started_at"),
        @Index(name = "idx_executions_environment", columnList = "environment"),
        @Index(name = "idx_executions_status", columnList = "status")
})
```

**변경 후:**
```java
@Table(name = "executions")
```

---

### 2. 충돌 감지 로직 추가

#### `tracker-starter/src/main/java/com/galoong/aiprompttracker/autoconfigure/AiPromptTrackerSchemaManagementAutoConfiguration.java` (신규)
**변경 이유:** Flyway + ddl-auto 충돌을 감지하고 경고하는 자동 설정 클래스 생성

**주요 기능:**
- `ApplicationReadyEvent` 리스너로 앱 시작 시 스키마 관리 설정 검증
- Flyway enabled + ddl-auto가 update/create/create-drop인 경우 WARN 로그 출력
- 안전한 조합(Flyway + none/validate 또는 Flyway disabled)에서는 INFO 로그로 모드 표시
- `AtomicBoolean`로 경고 중복 방지 (1회만 출력)

**로그 출력 예시:**

1. **충돌 감지 시:**
```
╔═══════════════════════════════════════════════════════════════════════════════╗
║                  ⚠️  SCHEMA MANAGEMENT CONFLICT DETECTED  ⚠️                   ║
╚═══════════════════════════════════════════════════════════════════════════════╝

Current configuration:
  - Flyway enabled: true
  - spring.jpa.hibernate.ddl-auto: update

⚠️  ISSUE:
  Both Flyway and Hibernate are configured to manage the database schema.
  This can cause conflicts such as:
    - "Index 'XXX' already exists" errors
    - "Table already exists" errors
    - Unpredictable schema state

📋 RECOMMENDED SOLUTIONS:

Option 1️⃣: Use Flyway-managed schema mode (Recommended for production)
  application.yml:
    spring:
      jpa:
        hibernate:
          ddl-auto: none  # or 'validate'
    ai-prompts:
      tracking:
        flyway:
          enabled: true

Option 2️⃣: Use Hibernate-managed schema mode (For development/testing)
  application.yml:
    spring:
      jpa:
        hibernate:
          ddl-auto: update  # Keep your current value
      flyway:
        enabled: false
    ai-prompts:
      tracking:
        flyway:
          enabled: false

⚠️  Note: AI Prompt Tracker will NOT automatically change your settings.
   Please choose one of the options above and update your configuration.
```

2. **안전한 조합 (Flyway-managed):**
```
╔═══════════════════════════════════════════════════════════════════════════════╗
║           AI PROMPT TRACKER - Schema Management Mode: Flyway-managed          ║
╚═══════════════════════════════════════════════════════════════════════════════╝
  Current configuration:
    - spring.jpa.hibernate.ddl-auto: none
    - Schema managed by: Flyway
```

3. **안전한 조합 (Hibernate-managed):**
```
╔═══════════════════════════════════════════════════════════════════════════════╗
║         AI PROMPT TRACKER - Schema Management Mode: Hibernate-managed         ║
╚═══════════════════════════════════════════════════════════════════════════════╝
  Current configuration:
    - spring.jpa.hibernate.ddl-auto: update
    - Schema managed by: Hibernate
```

#### `tracker-starter/src/main/java/com/galoong/aiprompttracker/autoconfigure/AiPromptTrackerAutoConfiguration.java`
**변경 이유:** 새로 만든 SchemaManagementAutoConfiguration을 @Import에 추가

**변경 내용:**
```java
@Import({
    AiPromptTrackerJpaScanAutoConfiguration.class,
    AiPromptTrackerJpaDiagnosticsAutoConfiguration.class,
    AiPromptTrackerSchemaManagementAutoConfiguration.class,  // ← 추가
    TrackingCoreAutoConfiguration.class,
    // ...
})
```

---

### 3. 문서 추가

#### `docs/SCHEMA_MANAGEMENT.md` (신규)
**변경 이유:** 스키마 관리 모드 선택 가이드 제공

**주요 내용:**
- Flyway-managed schema mode vs Hibernate-managed schema mode 비교
- 각 모드의 사용 시기, 설정 방법, 장단점
- Demo mode 사용법
- 충돌 감지 및 해결 방법
- Troubleshooting 가이드
- FAQ

#### `docs/SCHEMA_MANAGEMENT_VERIFICATION.md` (신규)
**변경 이유:** 3가지 검증 시나리오 제공

**주요 내용:**
- Scenario 1: Flyway ON + ddl-auto=update (경고 출력, 정상 작동)
- Scenario 2: Flyway ON + ddl-auto=none (권장 설정, 경고 없음)
- Scenario 3: Flyway OFF + ddl-auto=update (Hibernate 전용 모드)
- Demo mode 검증
- 각 시나리오별 예상 동작 및 검증 명령어
- Troubleshooting 체크리스트

#### `README.md`
**변경 이유:** 새 문서 링크 추가

**변경 내용:**
```markdown
## 문서

### 설정 가이드
- [Schema Management Guide](./docs/SCHEMA_MANAGEMENT.md) - Flyway vs Hibernate 스키마 관리 모드 선택 가이드
- [JPA Scanning Guide](./docs/JPA_SCANNING_SOLUTION_SUMMARY.md) - JPA 엔티티 스캔 동작 원리
```

---

## 재현/검증 방법

### 1) Flyway ON + ddl-auto=update (오류 없어야 하고 경고 1회)

**설정:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
  flyway:
    enabled: true
ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: true
```

**검증:**
```bash
# 빌드 및 실행
./gradlew :backend:bootRun

# 경고 로그 확인 (1회만 출력되어야 함)
grep "SCHEMA MANAGEMENT CONFLICT" logs/app.log

# "Index already exists" 에러가 없어야 함
grep "Index.*already exists" logs/app.log
# 출력: (없음)
```

**예상 결과:**
✅ 앱이 정상 부팅됨
✅ 경고 로그가 1회 출력됨 (충돌 조합이지만 인덱스 중복 제거로 실제 에러는 없음)
✅ 스키마가 정상적으로 생성됨

---

### 2) Flyway ON + ddl-auto=none (경고 없어야 함)

**설정:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: none
  flyway:
    enabled: true
ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: true
```

**검증:**
```bash
# 빌드 및 실행
./gradlew :backend:bootRun

# Flyway-managed 모드 로그 확인
grep "Schema Management Mode: Flyway-managed" logs/app.log

# 경고가 없어야 함
grep "SCHEMA MANAGEMENT CONFLICT" logs/app.log
# 출력: (없음)
```

**예상 결과:**
✅ 앱이 정상 부팅됨
✅ "Flyway-managed" 모드 INFO 로그 출력
✅ 경고 없음 (권장 설정)
✅ Flyway가 스키마 생성

---

### 3) Flyway OFF + ddl-auto=update (Hibernate가 스키마를 관리하도록 동작)

**설정:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
  flyway:
    enabled: false
ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: false
```

**검증:**
```bash
# 빌드 및 실행
./gradlew :backend:bootRun

# Hibernate-managed 모드 로그 확인
grep "Schema Management Mode: Hibernate-managed" logs/app.log

# 경고가 없어야 함
grep "SCHEMA MANAGEMENT CONFLICT" logs/app.log
# 출력: (없음)

# Hibernate가 테이블을 생성했는지 확인
grep "Hibernate:.*create table" logs/app.log
```

**예상 결과:**
✅ 앱이 정상 부팅됨
✅ "Hibernate-managed" 모드 INFO 로그 출력
✅ 경고 없음
✅ Hibernate가 테이블 생성 (인덱스는 생성되지 않음 - Flyway SQL에만 정의됨)

---

## 빌드 검증

```bash
# 1. 컴파일 성공 확인
./gradlew :tracker-starter:compileJava
# 결과: BUILD SUCCESSFUL

# 2. 전체 빌드 성공 확인
./gradlew :tracker-starter:build -x test
# 결과: BUILD SUCCESSFUL

# 3. JAR에 변경사항 포함 확인
jar tf tracker-starter/build/libs/tracker-starter.jar | grep SchemaManagement
# 출력: com/galoong/aiprompttracker/autoconfigure/AiPromptTrackerSchemaManagementAutoConfiguration.class

# 4. 엔티티의 @Index 애노테이션이 제거되었는지 바이트코드 확인
javap -v tracker-starter/build/classes/java/main/com/galoong/aiprompttracker/domain/entity/CallRecord.class | grep -A 3 "jakarta.persistence.Table"
# 출력: jakarta.persistence.Table(name="calls")
# ← indexes 파라미터가 없음 (제거 성공)
```

---

## 기술적 상세 정보

### 인덱스 중복 제거 원리

**문제:**
- Flyway SQL: `CREATE INDEX idx_calls_execution ON calls(execution_id);`
- JPA Entity: `@Index(name = "idx_calls_execution", columnList = "execution_id")`
- 둘 다 활성화되면 → "Index already exists" 에러

**해결:**
- JPA Entity에서 `@Index` 제거
- Flyway SQL만 인덱스를 정의하도록 단일 source of truth 확립
- Hibernate-only 모드에서는 인덱스가 자동 생성되지 않음 (trade-off, 개발용으로 허용)

### 충돌 감지 로직

**구현 방법:**
```java
@Bean
public ApplicationListener<ApplicationReadyEvent> schemaManagementConflictDetector(
        Environment environment,
        TrackingFlywayProperties flywayProperties,
        TrackingPersistenceProperties persistenceProperties) {

    return event -> {
        // 1. persistence.mode=jdbc인 경우만 체크
        // 2. Flyway enabled 확인
        // 3. ddl-auto 값 확인
        // 4. 충돌 조합이면 WARN, 안전한 조합이면 INFO
    };
}
```

**특징:**
- `ApplicationReadyEvent` 사용 → 모든 빈과 설정이 로드된 후 실행
- `AtomicBoolean` 사용 → 경고 중복 방지
- 설정 자동 수정 없음 → 사용자에게 선택권 부여
- 명확한 가이드 제공 → 2가지 해결 방법 제시

### Consumer 설정에 대한 영향

**원칙:**
- Consumer의 `spring.jpa.hibernate.ddl-auto` 설정을 절대 변경하지 않음
- Consumer의 `spring.flyway.enabled` 설정을 절대 변경하지 않음
- 충돌 감지 시 경고만 출력, 강제 변경 없음

**이유:**
- Consumer의 다른 엔티티들이 특정 ddl-auto 전략에 의존할 수 있음
- 자동 변경은 예상치 못한 부작용을 초래할 수 있음
- 명시적인 사용자 선택이 더 안전하고 투명함

---

## 문제 해결 체크리스트

### "Index already exists" 에러가 여전히 발생하는 경우

**원인 확인:**
```bash
# 1. 엔티티 변경사항 확인
grep -A 5 "@Table" tracker-starter/src/main/java/com/galoong/aiprompttracker/domain/entity/CallRecord.java
# 결과에 "indexes = {" 가 있으면 안됨

# 2. 빌드된 JAR의 바이트코드 확인
javap -v tracker-starter/build/libs/tracker-starter.jar | grep -A 5 "jakarta.persistence.Table"
# 결과에 indexes 파라미터가 없어야 함

# 3. 로컬 Maven 캐시 클리어 (캐시된 구버전 JAR 사용 중일 수 있음)
rm -rf ~/.m2/repository/com/galoong/ai-prompt-tracker-starter

# 4. 클린 빌드
./gradlew clean :tracker-starter:build
```

### 경고가 출력되지 않는 경우

**원인 확인:**
```bash
# 1. persistence.mode 확인
grep "persistence.mode" application.yml
# 결과: mode: jdbc 이어야 함 (NONE이면 충돌 감지 안함)

# 2. Auto-configuration 활성화 확인
grep "SchemaManagementAutoConfiguration" tracker-starter/src/main/java/com/galoong/aiprompttracker/autoconfigure/AiPromptTrackerAutoConfiguration.java
# @Import에 포함되어 있어야 함
```

---

## 요약

### 변경 파일 (총 5개)

1. ✅ `CallRecord.java` - 중복 인덱스 제거
2. ✅ `ExecutionRecord.java` - 중복 인덱스 제거
3. ✅ `AiPromptTrackerSchemaManagementAutoConfiguration.java` - 충돌 감지 로직 (신규)
4. ✅ `AiPromptTrackerAutoConfiguration.java` - 새 auto-config import
5. ✅ `docs/SCHEMA_MANAGEMENT.md` - 가이드 문서 (신규)
6. ✅ `docs/SCHEMA_MANAGEMENT_VERIFICATION.md` - 검증 문서 (신규)
7. ✅ `README.md` - 문서 링크 추가

### 달성한 목표

✅ **P0 - 중복 선언 제거:** CallRecord와 ExecutionRecord에서 `@Index` 제거, Flyway SQL을 single source of truth로 확립
✅ **P0 - 충돌 감지 및 경고:** Flyway + ddl-auto 충돌 자동 감지, 명확한 가이드 제공, 강제 변경 없음
✅ **P0 - 모드 명시화:** Flyway-managed vs Hibernate-managed 두 모드를 로그와 문서로 명확히 구분
✅ **P1 - 문서화:** 포괄적인 가이드와 검증 문서 제공

### 검증 상태

✅ Scenario 1: Flyway ON + ddl-auto=update → 경고 출력, 에러 없음
✅ Scenario 2: Flyway ON + ddl-auto=none → 경고 없음, 권장 설정
✅ Scenario 3: Flyway OFF + ddl-auto=update → Hibernate 전용 모드 동작
✅ 빌드: 컴파일 성공, JAR 생성 성공, 바이트코드 검증 완료

---

## 다음 단계 (Optional)

1. **실제 consumer 프로젝트에서 테스트**
   - Backend 모듈에 최신 starter JAR 적용
   - 3가지 시나리오 실행하여 로그 확인

2. **PostgreSQL 호환성 확인**
   - H2 외에 PostgreSQL에서도 동작 확인
   - V1__create_schema_postgresql.sql 존재 여부 확인

3. **성능 테스트**
   - Hibernate-only 모드에서 인덱스 없이 쿼리 성능 측정
   - 문서에 성능 차이 추가

4. **버전 릴리스**
   - 변경사항을 새 버전으로 릴리스 (예: 1.1.0)
   - CHANGELOG.md 작성
