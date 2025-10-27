// src/main/resources/static/js/common.js
// 마이넷 매출 관리 시스템 공통 JavaScript 기능

/**
 * 공통 유틸리티 객체
 */
const Utils = {
  /**
   * 숫자를 천단위 콤마 형식으로 포맷팅
   */
  formatNumber: function (num) {
    if (num === null || num === undefined || isNaN(num)) return "0";
    return new Intl.NumberFormat("ko-KR").format(num);
  },

  /**
   * 날짜를 YYYY-MM-DD 형식으로 포맷팅
   */
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

  /**
   * 오늘 날짜를 YYYY-MM-DD 형식으로 반환
   */
  getTodayString: function () {
    return this.formatDate(new Date());
  },

  /**
   * 달성률 계산 (실적/목표 * 100)
   */
  calculateAchievementRate: function (actual, target) {
    if (!target || target === 0) return 0;
    return Math.round((actual / target) * 100 * 100) / 100; // 소수점 2자리
  },

  /**
   * 금액 계산 (수량 × 단가)
   */
  calculateAmount: function (quantity, price) {
    if (!quantity || !price) return 0;
    return quantity * price;
  },
};

/**
 * AJAX 통신 관리
 */
const ApiClient = {
  /**
   * 기본 AJAX 요청
   */
  request: function (url, method, data, callback) {
    const xhr = new XMLHttpRequest();
    xhr.open(method, url, true);
    xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");

    // CSRF 토큰 설정
    const csrfToken = document.querySelector('meta[name="_csrf"]');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]');
    if (csrfToken && csrfHeader) {
      xhr.setRequestHeader(
        csrfHeader.getAttribute("content"),
        csrfToken.getAttribute("content")
      );
    }

    xhr.onreadystatechange = function () {
      if (xhr.readyState === 4) {
        if (xhr.status === 200) {
          callback(null, xhr.responseText);
        } else {
          callback(new Error(`HTTP ${xhr.status}: ${xhr.statusText}`), null);
        }
      }
    };

    if (data) {
      const formData = new URLSearchParams(data).toString();
      xhr.send(formData);
    } else {
      xhr.send();
    }
  },

  /**
   * GET 요청
   */
  get: function (url, callback) {
    this.request(url, "GET", null, callback);
  },

  /**
   * POST 요청
   */
  post: function (url, data, callback) {
    this.request(url, "POST", data, callback);
  },
};

/**
 * 알림 메시지 관리
 */
const AlertManager = {
  container: null,

  init: function () {
    this.container = document.getElementById("alert-container");
    if (!this.container) {
      this.container = document.createElement("div");
      this.container.id = "alert-container";
      document.body.insertBefore(this.container, document.body.firstChild);
    }
  },

  show: function (message, type = "info", duration = 5000) {
    if (!this.container) this.init();

    const alert = document.createElement("div");
    alert.className = `alert alert-${type}`;
    alert.textContent = message;

    // 닫기 버튼 추가
    const closeBtn = document.createElement("button");
    closeBtn.innerHTML = "×";
    closeBtn.style.cssText =
      "float: right; background: none; border: none; font-size: 1.2rem; cursor: pointer;";
    closeBtn.onclick = () => this.hide(alert);
    alert.appendChild(closeBtn);

    this.container.appendChild(alert);

    // 자동 숨김
    if (duration > 0) {
      setTimeout(() => this.hide(alert), duration);
    }

    return alert;
  },

  hide: function (alert) {
    if (alert && alert.parentNode) {
      alert.style.opacity = "0";
      setTimeout(() => {
        alert.parentNode.removeChild(alert);
      }, 300);
    }
  },

  success: function (message, duration) {
    return this.show(message, "success", duration);
  },

  error: function (message, duration) {
    return this.show(message, "error", duration);
  },

  warning: function (message, duration) {
    return this.show(message, "warning", duration);
  },
};

/**
 * 데이터 테이블 관리
 */
const TableManager = {
  /**
   * 수량 입력 필드 생성
   */
  createQuantityInput: function (value, isEditable, onSave) {
    const input = document.createElement("input");
    input.type = "number";
    input.value = value || 0;
    input.className = isEditable
      ? "form-input editable"
      : "form-input readonly";
    input.disabled = !isEditable;
    input.min = "0";
    input.step = "1";

    if (isEditable && onSave) {
      input.addEventListener("blur", function () {
        onSave(this.value);
      });
      input.addEventListener("keypress", function (e) {
        if (e.key === "Enter") {
          this.blur();
        }
      });
    }

    return input;
  },

  /**
   * 금액 표시 셀 생성
   */
  createAmountCell: function (amount, comparison) {
    const cell = document.createElement("td");
    cell.textContent = Utils.formatNumber(amount);

    if (comparison) {
      const arrow = document.createElement("span");
      arrow.className = `comparison-arrow ${
        comparison.isIncrease ? "arrow-up" : "arrow-down"
      }`;
      arrow.innerHTML = comparison.isIncrease ? " ↑" : " ↓";
      cell.appendChild(arrow);
    }

    return cell;
  },

  /**
   * 달성률 표시 셀 생성
   */
  createAchievementCell: function (actual, target) {
    const cell = document.createElement("td");
    const rate = Utils.calculateAchievementRate(actual, target);

    const rateSpan = document.createElement("span");
    rateSpan.textContent = rate + "%";

    const progressBar = document.createElement("div");
    progressBar.className = "progress-bar";

    const progressFill = document.createElement("div");
    progressFill.className = "progress-fill";
    progressFill.style.width = Math.min(rate, 100) + "%";

    progressBar.appendChild(progressFill);

    const container = document.createElement("div");
    container.className = "achievement-rate";
    container.appendChild(rateSpan);
    container.appendChild(progressBar);

    cell.appendChild(container);
    return cell;
  },
};

/**
 * 날짜 네비게이션 관리
 */
const DateNavigation = {
  currentDate: new Date(),

  init: function (container, onDateChange) {
    this.container = container;
    this.onDateChange = onDateChange;
    this.render();
  },

  render: function () {
    if (!this.container) return;

    this.container.innerHTML = `
            <div class="date-navigation">
                <button class="date-nav-btn" onclick="DateNavigation.previousDay()">◀</button>
                <button class="date-picker-btn" onclick="DateNavigation.showDatePicker()">날짜 선택</button>
                <span class="current-date">${Utils.formatDate(
                  this.currentDate
                )}</span>
                <button class="date-nav-btn" onclick="DateNavigation.nextDay()">▶</button>
            </div>
        `;
  },

  previousDay: function () {
    this.currentDate.setDate(this.currentDate.getDate() - 1);
    this.update();
  },

  nextDay: function () {
    this.currentDate.setDate(this.currentDate.getDate() + 1);
    this.update();
  },

  setDate: function (dateString) {
    this.currentDate = new Date(dateString);
    this.update();
  },

  update: function () {
    this.render();
    if (this.onDateChange) {
      this.onDateChange(Utils.formatDate(this.currentDate));
    }
  },

  showDatePicker: function () {
    // 간단한 프롬프트로 날짜 입력 받기 (나중에 달력 위젯으로 교체)
    const dateStr = prompt(
      "날짜를 입력하세요 (YYYY-MM-DD):",
      Utils.formatDate(this.currentDate)
    );
    if (dateStr && /^\d{4}-\d{2}-\d{2}$/.test(dateStr)) {
      this.setDate(dateStr);
    }
  },
};

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
    if (loading) {
      loading.remove();
    }
  },
};

/**
 * 서브메뉴 토글
 */
function toggleSubMenu(menuId) {
  const menu = document.getElementById(menuId);
  if (menu) {
    menu.style.display = menu.style.display === "none" ? "block" : "none";
  }

  // 다른 서브메뉴는 닫기
  document.querySelectorAll(".sub-menu").forEach(function (otherMenu) {
    if (otherMenu.id !== menuId) {
      otherMenu.style.display = "none";
    }
  });
}

/**
 * 툴팁 표시
 */
function showTooltip(element, content) {
  const tooltip = document.createElement("div");
  tooltip.className = "tooltip-content";
  tooltip.textContent = content;

  element.appendChild(tooltip);
  element.classList.add("tooltip");
}

/**
 * 페이지 로드 시 초기화
 */
document.addEventListener("DOMContentLoaded", function () {
  // 알림 매니저 초기화
  AlertManager.init();

  // 숫자 입력 필드 포맷팅
  document.querySelectorAll('input[type="number"]').forEach(function (input) {
    input.addEventListener("blur", function () {
      if (this.value && !isNaN(this.value)) {
        // 음수 방지
        if (parseFloat(this.value) < 0) {
          this.value = 0;
        }
      }
    });
  });
});

/**
 * 데이터 저장 공통 함수
 */
function saveData(url, data, successCallback, errorCallback) {
  LoadingManager.show(document.body);

  // FormData 사용으로 변경
  const formData = new FormData();

  // 데이터 추가
  for (const key in data) {
    formData.append(key, data[key]);
  }

  // CSRF 토큰 추가
  const csrfToken = document.querySelector('meta[name="_csrf"]');
  if (csrfToken) {
    formData.append("_csrf", csrfToken.getAttribute("content"));
  }

  const xhr = new XMLHttpRequest();
  xhr.open("POST", url, true);
  // FormData 사용시 Content-Type 헤더를 설정하지 않음 (자동으로 설정됨)

  xhr.onreadystatechange = function () {
    if (xhr.readyState === 4) {
      LoadingManager.hide(document.body);

      if (xhr.status === 200) {
        if (xhr.responseText.startsWith("error:")) {
          AlertManager.error(xhr.responseText.substring(6));
        } else {
          const message = successCallback
            ? successCallback(xhr.responseText)
            : "저장되었습니다.";
          AlertManager.success(message);
        }
      } else {
        const message = errorCallback
          ? errorCallback(xhr)
          : "저장 중 오류가 발생했습니다.";
        AlertManager.error(message);
      }
    }
  };

  xhr.send(formData);
}
