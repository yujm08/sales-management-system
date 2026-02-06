// CustomErrorController.java - 에러 처리
package com.mynet.sales_management_system.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 전역 에러 처리 컨트롤러
 * - 404, 500 등 HTTP 에러 처리
 * - 사용자 친화적 에러 페이지 제공
 */
@Controller
@Slf4j
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                log.warn("404 Error: {}", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
                model.addAttribute("errorMessage", "요청하신 페이지를 찾을 수 없습니다.");
                return "error/404";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                log.error("500 Error: {}", request.getAttribute(RequestDispatcher.ERROR_MESSAGE));
                model.addAttribute("errorMessage", "서버 내부 오류가 발생했습니다.");
                return "error/500";
            }
        }

        model.addAttribute("errorMessage", "알 수 없는 오류가 발생했습니다.");
        return "error/general";
    }

    // 403 전용 엔드포인트 추가
    @RequestMapping("/error-403")
    public String handle403Error(HttpServletResponse response) {
        log.warn("403 페이지 접근");

        // 캐시 방지
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        return "error/403";
    }
}