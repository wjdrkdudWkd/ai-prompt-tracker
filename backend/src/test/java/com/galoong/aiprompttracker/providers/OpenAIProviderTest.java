package com.galoong.aiprompttracker.providers;

import com.galoong.aiprompttracker.core.provider.AIProviderResponse;
import com.galoong.aiprompttracker.core.provider.PricingModel;
import com.galoong.aiprompttracker.providers.openai.OpenAIPricingLoader;
import com.galoong.aiprompttracker.providers.openai.OpenAIProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * OpenAI Provider 테스트
 */
@ExtendWith(MockitoExtension.class)
class OpenAIProviderTest {

    @Mock
    private OpenAIPricingLoader pricingLoader;

    private OpenAIProvider provider;

    @BeforeEach
    void setUp() {
        provider = new OpenAIProvider("test-api-key", pricingLoader);

        // Mock pricing data
        PricingModel pricing = PricingModel.builder()
                .modelName("gpt-4")
                .promptTokenPrice(new BigDecimal("0.03"))
                .completionTokenPrice(new BigDecimal("0.06"))
                .currency("USD")
                .build();

        when(pricingLoader.getPricing(anyString())).thenReturn(pricing);
    }

    @Test
    void testGetProviderName() {
        assertEquals("OpenAI", provider.getProviderName());
    }

    @Test
    void testGetSupportedModels() {
        String[] models = provider.getSupportedModels();
        assertNotNull(models);
        assertTrue(models.length > 0);
        assertTrue(containsModel(models, "gpt-4"));
        assertTrue(containsModel(models, "gpt-3.5-turbo"));
    }

    @Test
    void testGetPricing() {
        PricingModel pricing = provider.getPricing("gpt-4");
        assertNotNull(pricing);
        assertEquals("gpt-4", pricing.getModelName());
        assertEquals(new BigDecimal("0.03"), pricing.getPromptTokenPrice());
        assertEquals(new BigDecimal("0.06"), pricing.getCompletionTokenPrice());
    }

    @Test
    void testCalculateCost() {
        PricingModel pricing = provider.getPricing("gpt-4");

        // 1000 prompt tokens + 500 completion tokens
        BigDecimal cost = pricing.calculateCost(1000, 500);

        // Expected: (1000 * 0.03 / 1000) + (500 * 0.06 / 1000) = 0.03 + 0.03 = 0.06
        assertEquals(0, new BigDecimal("0.06").compareTo(cost));
    }

    @Test
    void testExecuteWithInvalidApiKey() {
        // Note: This test will fail in actual API call
        // In real scenario, you should mock WebClient
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", 0.7);

        AIProviderResponse response = provider.execute(
                "gpt-3.5-turbo",
                "Hello, world!",
                params
        );

        // With invalid API key, it should return error response
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertNotNull(response.getErrorMessage());
    }

    private boolean containsModel(String[] models, String target) {
        for (String model : models) {
            if (model.equals(target)) {
                return true;
            }
        }
        return false;
    }
}
