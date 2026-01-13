# Quick Start Guide

## 초기 프로젝트 설정 완료 사항

### ✅ 완료된 구현

1. **프로젝트 구조 생성**
   - Gradle 멀티 모듈 프로젝트 (Root + Backend)
   - Java 17, Spring Boot 3.4.1 설정

2. **Core 인터페이스**
   - `AIProvider` - Provider 추상 인터페이스
   - `UsageMetrics` - 사용량 메트릭
   - `PricingModel` - 가격 모델
   - `AIProviderResponse` - 표준화된 응답 형식

3. **어노테이션**
   - `@AIPrompt` - AI 함수 자동 추적
   - `@PromptParam` - 파라미터 메타데이터

4. **OpenAI Provider**
   - OpenAI API 연동
   - 토큰 사용량 계산
   - 비용 자동 계산
   - YAML 기반 가격 정보 로딩

5. **데이터베이스**
   - JPA 엔티티: `AICallRecord`, `DailyStats`
   - Spring Data JPA 리포지토리
   - Flyway 마이그레이션 스크립트 (V1, V2)

6. **AOP 자동 추적**
   - `AIPromptAspect` - 자동 추적 로직
   - `AIProviderDetector` - Provider 자동 감지
   - `TrackingService` - 호출 기록 저장

7. **설정 파일**
   - `application.yml` (기본)
   - `application-dev.yml` (개발)
   - `application-test.yml` (테스트)
   - `application-prod.yml` (운영)

8. **Docker 환경**
   - PostgreSQL 16.1
   - Redis 7.2
   - Docker Compose 설정

## 실행 방법

### 1. 데이터베이스 시작

```bash
# 개발 환경 시작 (PostgreSQL + Redis)
./start-dev.sh

# 또는 수동으로
cd docker
docker-compose up -d postgres redis
```

### 2. 환경 변수 설정

```bash
# OpenAI API Key 설정
export OPENAI_API_KEY=your-api-key-here

# (선택) 다른 Provider API Key
export ANTHROPIC_API_KEY=your-anthropic-key
export GOOGLE_API_KEY=your-google-key
```

### 3. 백엔드 실행

```bash
cd backend

# Gradle 빌드 및 실행
./gradlew bootRun

# 또는 빌드 후 JAR 실행
./gradlew build
java -jar build/libs/ai-prompt-tracker-1.0.0-SNAPSHOT.jar
```

서버가 시작되면: http://localhost:8080

### 4. 테스트 실행

```bash
cd backend
./gradlew test
```

## 사용 예시

### 1. AI 함수 작성

```java
import com.galoong.aiprompttracker.core.annotation.AIPrompt;
import com.galoong.aiprompttracker.core.annotation.PromptParam;
import com.galoong.aiprompttracker.core.provider.AIProviderResponse;
import com.galoong.aiprompttracker.providers.openai.OpenAIProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ExampleService {

    @Autowired
    private OpenAIProvider openAIProvider;

    @AIPrompt(
        provider = "OpenAI",
        model = "gpt-4",
        description = "텍스트 요약 기능",
        category = "content-generation",
        tags = {"summary", "nlp"}
    )
    public AIProviderResponse summarizeText(
        @PromptParam(name = "text", description = "요약할 텍스트") String text
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", 0.3);
        params.put("max_tokens", 500);

        String prompt = "다음 텍스트를 3문장으로 요약해주세요:\n\n" + text;

        return openAIProvider.execute("gpt-4", prompt, params);
    }
}
```

### 2. 자동 추적 확인

메서드 실행 시 자동으로:
- 호출 기록이 `ai_call_records` 테이블에 저장됩니다
- 토큰 사용량과 비용이 자동 계산됩니다
- 응답 시간이 측정됩니다

### 3. 데이터베이스 확인

```sql
-- 최근 호출 기록 조회
SELECT function_name, model_name, total_tokens, estimated_cost, created_at
FROM ai_call_records
ORDER BY created_at DESC
LIMIT 10;

-- 함수별 총 비용
SELECT function_name, SUM(estimated_cost) as total_cost
FROM ai_call_records
GROUP BY function_name
ORDER BY total_cost DESC;
```

## 다음 단계

### 1. Dashboard API 구현 (Step 6)
- REST API 컨트롤러 작성
- 통계 조회 서비스
- DTO 정의

### 2. Frontend 구축 (Step 7)
- React 컴포넌트
- Dashboard UI
- 차트 및 시각화

### 3. 추가 Provider 구현 (Step 8)
- Anthropic Claude Provider
- Google Gemini Provider

### 4. 고급 기능 (Step 9)
- 캐싱 구현
- 실시간 알림
- 비용 알림
- A/B 테스트

## 프로젝트 구조

```
ai-prompt-tracker/
├── backend/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── java/com/galoong/aiprompttracker/
│       │   │   ├── core/               ✅ 완료
│       │   │   ├── providers/          ✅ OpenAI 완료
│       │   │   ├── tracking/           ✅ 완료
│       │   │   ├── domain/             ✅ 완료
│       │   │   ├── dashboard/          ⏳ 다음 단계
│       │   │   └── config/             ✅ 완료
│       │   └── resources/
│       │       ├── application.yml     ✅ 완료
│       │       ├── db/migration/       ✅ 완료
│       │       └── providers/          ✅ OpenAI 완료
│       └── test/                       ✅ 기본 테스트 완료
├── frontend/                           ⏳ 다음 단계
├── docker/                             ✅ 완료
├── build.gradle.kts                    ✅ 완료
└── settings.gradle.kts                 ✅ 완료
```

## 트러블슈팅

### Gradle 빌드 에러
```bash
# Gradle wrapper가 없는 경우
gradle wrapper --gradle-version 8.11

# 의존성 다시 다운로드
./gradlew clean build --refresh-dependencies
```

### 데이터베이스 연결 실패
```bash
# PostgreSQL 상태 확인
docker ps | grep postgres

# 로그 확인
docker logs ai-tracker-postgres
```

### Redis 연결 실패
```bash
# Redis 상태 확인
docker ps | grep redis

# Redis CLI 접속 테스트
docker exec -it ai-tracker-redis redis-cli ping
```

## 참고 자료

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/3.4.1/reference/)
- [OpenAI API Reference](https://platform.openai.com/docs/api-reference)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/16/)
- [Redis Documentation](https://redis.io/docs/)
