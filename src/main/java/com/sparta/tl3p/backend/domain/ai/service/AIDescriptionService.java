package com.sparta.tl3p.backend.domain.ai.service;

import com.sparta.tl3p.backend.common.exception.BusinessException;
import com.sparta.tl3p.backend.common.type.ErrorCode;
import com.sparta.tl3p.backend.domain.ai.dto.AIDescriptionRequestDto;
import com.sparta.tl3p.backend.domain.ai.dto.AIDescriptionResponseDto;
import com.sparta.tl3p.backend.domain.ai.dto.GeminiApiRequestDto;
import com.sparta.tl3p.backend.domain.ai.dto.GeminiApiResponseDto;
import com.sparta.tl3p.backend.domain.ai.entity.AIDescription;
import com.sparta.tl3p.backend.domain.ai.repository.AIDescriptionRepository;
import com.sparta.tl3p.backend.domain.item.entity.Item;
import com.sparta.tl3p.backend.domain.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class AIDescriptionService {
    private final AIDescriptionRepository aiDescriptionRepository;
    private final ItemRepository          itemRepository;
    private final RestTemplate            restTemplate;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Transactional
    public AIDescriptionResponseDto generateDescription(AIDescriptionRequestDto request) {
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));

        String prompt = generatePrompt(item);

        String aiResponse = callAIApi(prompt);

        AIDescription aiDescription = AIDescription.builder()
                .prompt(prompt)
                .response(aiResponse)
                .item(item)

                // TODO: member_id


                .build();

        aiDescriptionRepository.save(aiDescription);

        return AIDescriptionResponseDto.from(aiDescription);
    }

    private String callAIApi(String prompt) {
        URI url = UriComponentsBuilder.fromUriString(apiUrl)
                .queryParam("key", apiKey)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        GeminiApiRequestDto geminiApiRequest = GeminiApiRequestDto.of(prompt);

        try {
            ResponseEntity<GeminiApiResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(geminiApiRequest, headers),
                    GeminiApiResponseDto.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException(ErrorCode.API_STATUS_ERROR);
            }

            return extractGeminiResponse(response.getBody());
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.API_UNEXPECTED_ERROR);
        }
    }

    private String generatePrompt(Item item) {
        // TODO: 입력 prompt 개선
        return String.format(
                "다음은 음식점 메뉴 상품 정보이다:\n" +
                        "상품명: %s\n" +
                        "가게이름: %s\n" +
                        //                        "음식 카테고리: %s\n " +
                        "음식 배송 서비스에 제품을 등록하기 위해 해당 메뉴에 대한 실제 입력할 설명을 가게이름과 상품이름의 특색을 살려 맛, 재료, 특징 등을 표현한 답변을 50자 이하로 작성.",
                item.getName(),
                item.getStore().getName()
                //                ,item.getStore().getStoreCategories().toString()
        );
    }

    private String extractGeminiResponse(GeminiApiResponseDto geminiApiResponse) {
        return geminiApiResponse.getCandidates().stream()
                .findFirst()
                .map(GeminiApiResponseDto.Candidate::getContent)
                .map(GeminiApiResponseDto.Content::getParts)
                .flatMap(parts -> parts.stream().findFirst())
                .map(GeminiApiResponseDto.Part::getText)
                .orElseThrow(() -> new BusinessException(ErrorCode.API_RESPONSE_PARSE_ERROR));
    }
}
