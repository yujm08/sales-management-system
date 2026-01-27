package com.mynet.sales_management_system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class GeoLocationService {

    private final RestTemplate restTemplate = new RestTemplate();

    // IP 캐시 (1시간 동안 저장)
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = TimeUnit.HOURS.toMillis(1);

    /**
     * IP 주소가 한국인지 확인
     */
    public boolean isKoreanIP(String ipAddress) {
        // localhost 체크
        if (ipAddress.equals("127.0.0.1") ||
                ipAddress.equals("0:0:0:0:0:0:0:1") ||
                ipAddress.equals("::1") ||
                ipAddress.startsWith("192.168.") ||
                ipAddress.startsWith("10.") ||
                ipAddress.startsWith("172.")) {
            return true;
        }

        // 캐시 확인
        CacheEntry cached = cache.get(ipAddress);
        if (cached != null && !cached.isExpired()) {
            log.debug("캐시 히트: {} (한국: {})", ipAddress, cached.isKorean);
            return cached.isKorean;
        }

        // API 호출
        try {
            String url = String.format(
                    "http://ip-api.com/json/%s?fields=status,countryCode,country",
                    ipAddress);

            log.debug("IP API 호출: {}", ipAddress);

            @SuppressWarnings("unchecked")
            Map<String, String> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !"success".equals(response.get("status"))) {
                log.warn("IP API 조회 실패: {}", ipAddress);
                return true; // 실패 시 접근 허용 (서비스 중단 방지)
            }

            String countryCode = response.get("countryCode");
            String country = response.get("country");
            boolean isKorea = "KR".equals(countryCode);

            // 캐시에 저장
            cache.put(ipAddress, new CacheEntry(isKorea, countryCode, country));

            if (!isKorea) {
                log.warn("외국 IP 감지: {} (국가: {} - {})", ipAddress, countryCode, country);
            }

            return isKorea;

        } catch (RestClientException e) {
            log.error("IP API 호출 오류: {}", ipAddress, e);
            return true; // 에러 시 접근 허용
        }
    }

    /**
     * IP 주소의 국가 정보 가져오기
     */
    public String getCountryInfo(String ipAddress) {
        CacheEntry cached = cache.get(ipAddress);
        if (cached != null && !cached.isExpired()) {
            return cached.country + " (" + cached.countryCode + ")";
        }
        return "Unknown";
    }

    // 캐시 엔트리 클래스
    private static class CacheEntry {
        final boolean isKorean;
        final String countryCode;
        final String country;
        final long timestamp;

        CacheEntry(boolean isKorean, String countryCode, String country) {
            this.isKorean = isKorean;
            this.countryCode = countryCode;
            this.country = country;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION;
        }
    }
}