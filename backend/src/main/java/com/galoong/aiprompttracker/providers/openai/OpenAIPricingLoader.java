package com.galoong.aiprompttracker.providers.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.galoong.aiprompttracker.core.provider.PricingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenAI 가격 정보 로더
 */
@Slf4j
@Component
public class OpenAIPricingLoader {

    private final Map<String, PricingModel> pricingMap = new HashMap<>();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @PostConstruct
    public void loadPricing() {
        try {
            ClassPathResource resource = new ClassPathResource("providers/openai/pricing.yml");
            if (resource.exists()) {
                Map<String, Object> pricingData = yamlMapper.readValue(
                        resource.getInputStream(),
                        Map.class
                );
                parsePricingData(pricingData);
                log.info("Loaded pricing for {} OpenAI models", pricingMap.size());
            } else {
                loadDefaultPricing();
            }
        } catch (IOException e) {
            log.warn("Failed to load OpenAI pricing from YAML, using defaults", e);
            loadDefaultPricing();
        }
    }

    private void parsePricingData(Map<String, Object> data) {
        if (data.containsKey("models")) {
            Map<String, Map<String, Object>> models = (Map<String, Map<String, Object>>) data.get("models");
            models.forEach((modelName, pricing) -> {
                PricingModel model = PricingModel.builder()
                        .modelName(modelName)
                        .promptTokenPrice(new BigDecimal(pricing.get("prompt_token_price").toString()))
                        .completionTokenPrice(new BigDecimal(pricing.get("completion_token_price").toString()))
                        .currency("USD")
                        .build();
                pricingMap.put(modelName, model);
            });
        }
    }

    private void loadDefaultPricing() {
        // GPT-4 가격 (2024년 1월 기준)
        pricingMap.put("gpt-4", PricingModel.builder()
                .modelName("gpt-4")
                .promptTokenPrice(new BigDecimal("0.03"))
                .completionTokenPrice(new BigDecimal("0.06"))
                .currency("USD")
                .build());

        pricingMap.put("gpt-4-turbo", PricingModel.builder()
                .modelName("gpt-4-turbo")
                .promptTokenPrice(new BigDecimal("0.01"))
                .completionTokenPrice(new BigDecimal("0.03"))
                .currency("USD")
                .build());

        // GPT-3.5 가격
        pricingMap.put("gpt-3.5-turbo", PricingModel.builder()
                .modelName("gpt-3.5-turbo")
                .promptTokenPrice(new BigDecimal("0.0015"))
                .completionTokenPrice(new BigDecimal("0.002"))
                .currency("USD")
                .build());

        log.info("Loaded default pricing for {} models", pricingMap.size());
    }

    public PricingModel getPricing(String modelName) {
        return pricingMap.getOrDefault(modelName, getDefaultPricing());
    }

    private PricingModel getDefaultPricing() {
        return PricingModel.builder()
                .modelName("unknown")
                .promptTokenPrice(BigDecimal.ZERO)
                .completionTokenPrice(BigDecimal.ZERO)
                .currency("USD")
                .build();
    }
}
