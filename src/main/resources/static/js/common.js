// src/main/resources/static/js/common.js
// 마이넷 매출 관리 시스템 공통 JavaScript 기능

/**
 * 공통 유틸리티 객체
 */
const Utils = {
  formatNumber: function (num) {
    if (num === null || num === undefined || isNaN(num)) return "0";
    return new Intl.NumberFormat("ko-KR").format(num);
  },

  formatDate: function (date) {
    if (!date) return "";
    const d = new Date(date);
    return (
      d.getFullYear() +
      "-" +
      String(d.getMonth() + 1).padStart(2, "0") +
      "-" +
      String(d.getDate()).padStart(2, "0")
    );
  },

  getTodayString: function () {
    return this.formatDate(new Date());
  },

  calculateAchievementRate: function (actual, target) {
    if (!target || target === 0) return 0;
    return Math.round((actual / target) * 100 * 100) / 100;
  },

  calculateAmount: function (quantity, price) {
    if (!quantity || !price) return 0;
    return quantity * price;
  },
};

/**
 * 알림 표시 — 모든 페이지 공통
 * type: 'success' | 'error' | 'warning' | 'info'
 */
function showAlert(message, type = "info", duration = 3000) {
  const container = document.getElementById("alert-container");
  if (!container) return;

  const div = document.createElement("div");
  div.className = `alert alert-${type}`;
  div.textContent = message;
  container.appendChild(div);

  if (duration > 0) {
    setTimeout(() => {
      if (div.parentNode) div.remove();
    }, duration);
  }
}

/**
 * 날짜 포맷 전역 래퍼 — YYYY-MM-DD
 */
function formatDate(date) {
  return Utils.formatDate(date);
}

/**
 * 숫자 포맷 전역 래퍼 — 천단위 콤마
 */
function formatNumber(value) {
  return Utils.formatNumber(value);
}

/**
 * 증감률 포맷 — 비교 페이지 공통
 */
function formatGrowthRate(rate) {
  if (rate === null || rate === undefined) return "0.0%";
  const formatted = rate.toFixed(1);
  return rate > 0 ? "+" + formatted + "%" : formatted + "%";
}

/**
 * 증감률 CSS 클래스 — 비교 페이지 공통
 */
function getGrowthClass(rate) {
  if (rate > 0) return "growth-positive";
  if (rate < 0) return "growth-negative";
  return "growth-zero";
}

/**
 * 데이터 저장 공통 함수
 */
function saveData(url, data, successCallback, errorCallback) {
  LoadingManager.show(document.body);

  const formData = new FormData();
  for (const key in data) {
    formData.append(key, data[key]);
  }

  const csrfToken = document.querySelector('meta[name="_csrf"]');
  if (csrfToken) {
    formData.append("_csrf", csrfToken.getAttribute("content"));
  }

  const xhr = new XMLHttpRequest();
  xhr.open("POST", url, true);

  xhr.onreadystatechange = function () {
    if (xhr.readyState === 4) {
      LoadingManager.hide(document.body);

      if (xhr.status === 200) {
        if (xhr.responseText.startsWith("error:")) {
          showAlert(xhr.responseText.substring(6), "error");
        } else {
          const message = successCallback
            ? successCallback(xhr.responseText)
            : "저장되었습니다.";
          if (message) showAlert(message, "success");
        }
      } else {
        const message = errorCallback
          ? errorCallback(xhr)
          : "저장 중 오류가 발생했습니다.";
        showAlert(message, "error");
      }
    }
  };

  xhr.send(formData);
}

/**
 * 로딩 상태 관리
 */
const LoadingManager = {
  show: function (container) {
    if (!container) return;
    const loading = document.createElement("div");
    loading.className = "loading";
    loading.innerHTML = '<div class="spinner"></div>';
    loading.id = "loading-indicator";
    container.appendChild(loading);
  },

  hide: function (container) {
    if (!container) return;
    const loading = container.querySelector("#loading-indicator");
    if (loading) loading.remove();
  },
};
