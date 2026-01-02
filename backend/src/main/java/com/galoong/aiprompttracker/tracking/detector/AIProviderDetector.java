package com.galoong.aiprompttracker.tracking.detector;

import com.galoong.aiprompttracker.core.provider.AIProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * AI Provider 자동 감지기
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AIProviderDetector {

    private final List<AIProvider> providers;

    /**
     * Provider 이름으로 Provider 찾기
     */
    public Optional<AIProvider> findByName(String providerName) {
        if (providerName == null || providerName.isEmpty()) {
            return Optional.empty();
        }

        return providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(providerName))
                .findFirst();
    }

    /**
     * 모델명으로 Provider 찾기
     */
    public Optional<AIProvider> findByModel(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return Optional.empty();
        }

        return providers.stream()
                .filter(p -> supportsModel(p, modelName))
                .findFirst();
    }

    /**
     * 기본 Provider 반환 (첫 번째 Provider)
     */
    public Optional<AIProvider> getDefaultProvider() {
        return providers.isEmpty() ? Optional.empty() : Optional.of(providers.get(0));
    }

    /**
     * 사용 가능한 모든 Provider 목록
     */
    public List<AIProvider> getAllProviders() {
        return providers;
    }

    /**
     * Provider가 특정 모델을 지원하는지 확인
     */
    private boolean supportsModel(AIProvider provider, String modelName) {
        String[] supportedModels = provider.getSupportedModels();
        if (supportedModels == null || supportedModels.length == 0) {
            return false;
        }

        for (String supported : supportedModels) {
            if (modelName.equalsIgnoreCase(supported) ||
                modelName.startsWith(supported) ||
                supported.startsWith(modelName)) {
                return true;
            }
        }
        return false;
    }
}
