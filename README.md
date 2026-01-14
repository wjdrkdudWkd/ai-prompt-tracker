# AI Prompt Tracker

모든 AI API(OpenAI, Anthropic Claude, Google Gemini 등)를 Swagger처럼 관리하는 통합 개발자 도구

## 기술 스택

### Backend
- Java 17
- Spring Boot 3.4.1
- PostgreSQL 16.1
- Redis 7.2
- Flyway (DB Migration)

### Frontend
- React 18.2.0
- Vite 5.0.8
- TailwindCSS 3.4.0

## 주요 기능

### 1. 자동 AI 호출 추적
`@AIPrompt` 어노테이션을 사용하여 모든 AI API 호출을 자동으로 추적합니다.

```java
@AIPrompt(
    provider = "OpenAI",
    model = "gpt-4",
    description = "고객 질문에 대한 답변 생성",
    category = "customer-service"
)
public AIProviderResponse answerQuestion(String question) {
    // AI API 호출 로직
}
```

### 2. 비용 및 성능 모니터링
- 실시간 비용 추적 (Provider별, 모델별, 함수별)
- 응답 시간 분석
- 토큰 사용량 통계
- 일별/월별 집계

### 3. Multi-Provider 지원
- OpenAI (GPT-4, GPT-3.5 등)
- Anthropic Claude (Claude 3 등)
- Google Gemini
- 쉬운 Provider 확장 구조

### 4. Dashboard UI
- 함수별 사용량 통계
- Provider 비용 비교
- 실시간 모니터링
- Live Testing 기능

## 프로젝트 구조

```
ai-prompt-tracker/
├── backend/                 # Spring Boot 백엔드
│   └── src/main/java/com/galoong/aiprompttracker/
│       ├── core/           # 핵심 인터페이스
│       ├── providers/      # AI Provider 구현체
│       ├── tracking/       # 자동 추적 로직 (AOP)
│       ├── domain/         # 엔티티 & 리포지토리
│       ├── dashboard/      # REST API
│       └── config/         # 설정
├── frontend/               # React 프론트엔드
└── docker/                 # Docker 설정
```

## 시작하기

### 📦 라이브러리 사용자 (소비자)

**필수 요구사항**
- ✅ Java 17 이상
- ✅ PostgreSQL 16+ 또는 H2 (테스트용)
- ❌ Node.js **불필요** (React 대시보드가 JAR에 이미 포함됨)

**의존성 추가**

Gradle:
```kotlin
dependencies {
    implementation("com.galoong:ai-prompt-tracker-starter:1.0.0")
}
```

Maven:
```xml
<dependency>
    <groupId>com.galoong</groupId>
    <artifactId>ai-prompt-tracker-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

Spring Boot 앱을 실행하면 자동으로 대시보드가 `/aiprompt-tracker/`에서 제공됩니다.

---

### 🛠️ 저장소 개발자 (Contributor)

**필수 요구사항**
- Java 17 이상
- PostgreSQL 16+ 또는 H2
- Node.js 20+ (**CI 빌드 및 로컬 프론트엔드 개발 시 필요**)

**로컬 개발 - Node.js 없이 (Backend만)**

```bash
# Backend 빌드 및 실행 (vanilla fallback UI 제공)
./gradlew :backend:bootRun
```

이 경우 `/aiprompt-tracker/`에서 기본 HTML 대시보드(vanilla fallback)가 제공됩니다.

**로컬 개발 - React 대시보드 포함**

```bash
# 1. 프론트엔드 빌드
cd frontend
npm ci
npm run build
cd ..

# 2. 프론트엔드를 starter 리소스로 복사
./gradlew :tracker-starter:copyFrontend

# 3. Backend 빌드 및 실행
./gradlew :tracker-starter:build
./gradlew :backend:bootRun
```

이제 `/aiprompt-tracker/`에서 전체 React 대시보드를 사용할 수 있습니다.

---

### 1. 데이터베이스 설정

```bash
# PostgreSQL 실행
docker run -d \
  --name postgres \
  -e POSTGRES_DB=ai_prompt_tracker \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine

# Redis 실행
docker run -d \
  --name redis \
  -p 6379:6379 \
  redis:7.2-alpine
```

### 2. 백엔드 실행

```bash
cd backend

# 환경 변수 설정
export OPENAI_API_KEY=your-api-key

# Gradle 빌드 및 실행
./gradlew bootRun
```

### 3. 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
```

## 사용 예시

### 1. AI Provider 설정

```yaml
# application.yml
ai:
  providers:
    openai:
      api-key: ${OPENAI_API_KEY}
      enabled: true
```

### 2. AI 함수 작성

```java
@Service
public class CustomerService {

    @Autowired
    private OpenAIProvider openAIProvider;

    @AIPrompt(
        provider = "OpenAI",
        model = "gpt-4",
        description = "고객 문의 응답",
        category = "customer-service",
        tags = {"support", "automated"}
    )
    public AIProviderResponse answerCustomerQuestion(
        @PromptParam(name = "question", description = "고객 질문") String question
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", 0.7);
        params.put("max_tokens", 500);

        return openAIProvider.execute("gpt-4", question, params);
    }
}
```

### 3. 자동 추적 확인

메서드가 실행되면 자동으로:
- 호출 기록이 데이터베이스에 저장됩니다
- 비용이 자동 계산됩니다
- Dashboard에서 실시간 확인 가능합니다

## CI/CD 및 릴리즈

### GitHub Actions 워크플로우

저장소는 자동화된 CI/CD 파이프라인을 사용합니다:

**CI (Pull Request & Push)**
- Node.js 20.x 설치
- 프론트엔드 빌드 (`npm ci && npm run build`)
- 프론트엔드를 starter에 복사 (`copyFrontend`)
- Backend 빌드 및 테스트
- **Strict JAR 검증** (`verifyJarContentsStrict`) - React 대시보드 누락 시 빌드 실패

**Publish (Tags: v*.*.*)**
- 위와 동일한 빌드 프로세스
- GitHub Packages에 JAR 배포
- GitHub Release 생성

### 로컬 JAR 검증

```bash
# 빌드 후 JAR에 React 대시보드가 포함되었는지 확인 (경고만)
./gradlew :tracker-starter:verifyJarContents

# CI와 동일한 strict 검증 (누락 시 실패)
./gradlew :tracker-starter:verifyJarContentsStrict
```

**Expected Output (성공 시)**:
```
✅ Found embedded UI files:
   - META-INF/resources/aiprompt-tracker/index.html
   - META-INF/resources/aiprompt-tracker/_next/
   - META-INF/resources/aiprompt-tracker/dashboard-mvp.html

✅ JAR verification passed: React dashboard is embedded

Total UI files in JAR: 72
```

---

## 문서

### 설정 가이드
- [Schema Management Guide](./docs/SCHEMA_MANAGEMENT.md) - Flyway vs Hibernate 스키마 관리 모드 선택 가이드
- [JPA Scanning Guide](./docs/JPA_SCANNING_SOLUTION_SUMMARY.md) - JPA 엔티티 스캔 동작 원리
- [CI Workflow Guide](./CI_WORKFLOW_GUIDE.md) - CI/CD 워크플로우 상세 가이드 (GitLab CI, Jenkins 예제 포함)

### API 문서

#### 주요 엔드포인트

- `GET /api/dashboard/stats` - 전체 통계
- `GET /api/dashboard/functions` - 함수별 통계
- `GET /api/dashboard/providers/compare` - Provider 비교
- `GET /api/calls/recent` - 최근 호출 기록

## 라이선스

MIT License

## 기여

이슈 및 PR은 언제나 환영합니다!
