// src/main/resources/static/js/datepicker.js
// 날짜 선택 위젯

class DatePicker {
  constructor() {
    this.currentDate = new Date();
    this.selectedDate = new Date();
    this.onSelectCallback = null;
    this.restrictions = {
      minYear: null,
      maxYear: null,
      currentYearOnly: false,
    };
  }

  /**
   * 날짜 선택 위젯 초기화
   */
  init(triggerElement, options = {}) {
    this.triggerElement = triggerElement;
    this.onSelectCallback = options.onSelect;

    // 제한사항 설정
    if (options.currentYearOnly) {
      this.restrictions.currentYearOnly = true;
      this.restrictions.minYear = new Date().getFullYear();
      this.restrictions.maxYear = new Date().getFullYear();
    }

    // 트리거 요소에 클릭 이벤트 추가
    triggerElement.addEventListener("click", (e) => {
      e.preventDefault();
      this.show();
    });

    this.createModal();
  }

  /**
   * 달력 모달 생성
   */
  createModal() {
    // 기존 모달이 있으면 제거
    const existingModal = document.getElementById("datepicker-modal");
    if (existingModal) {
      existingModal.remove();
    }

    const modal = document.createElement("div");
    modal.id = "datepicker-modal";
    modal.className = "datepicker-modal";
    modal.innerHTML = `
            <div class="datepicker-backdrop"></div>
            <div class="datepicker-container">
                <div class="datepicker-header">
                    <button class="datepicker-nav-btn" id="prev-month">◀</button>
                    <div class="datepicker-title">
                        <select id="month-select"></select>
                        <select id="year-select"></select>
                    </div>
                    <button class="datepicker-nav-btn" id="next-month">▶</button>
                </div>
                <div class="datepicker-weekdays">
                    <div>일</div><div>월</div><div>화</div><div>수</div><div>목</div><div>금</div><div>토</div>
                </div>
                <div class="datepicker-calendar" id="calendar-grid"></div>
                <div class="datepicker-footer">
                    <button class="btn btn-outline" id="datepicker-cancel">취소</button>
                    <button class="btn btn-primary" id="datepicker-confirm">확인</button>
                </div>
            </div>
        `;

    document.body.appendChild(modal);
    this.modal = modal;
    this.setupEventListeners();
  }

  /**
   * 이벤트 리스너 설정
   */
  setupEventListeners() {
    const modal = this.modal;

    // 배경 클릭 시 닫기
    modal
      .querySelector(".datepicker-backdrop")
      .addEventListener("click", () => {
        this.hide();
      });

    // 이전/다음 월 버튼
    modal.querySelector("#prev-month").addEventListener("click", () => {
      this.currentDate.setMonth(this.currentDate.getMonth() - 1);
      this.renderCalendar();
    });

    modal.querySelector("#next-month").addEventListener("click", () => {
      this.currentDate.setMonth(this.currentDate.getMonth() + 1);
      this.renderCalendar();
    });

    // 월/년도 선택
    modal.querySelector("#month-select").addEventListener("change", (e) => {
      this.currentDate.setMonth(parseInt(e.target.value));
      this.renderCalendar();
    });

    modal.querySelector("#year-select").addEventListener("change", (e) => {
      this.currentDate.setFullYear(parseInt(e.target.value));
      this.renderCalendar();
    });

    // 확인/취소 버튼
    modal.querySelector("#datepicker-confirm").addEventListener("click", () => {
      this.confirm();
    });

    modal.querySelector("#datepicker-cancel").addEventListener("click", () => {
      this.hide();
    });
  }

  /**
   * 달력 표시
   */
  show() {
    this.populateSelects();
    this.renderCalendar();
    this.modal.style.display = "block";
    setTimeout(() => {
      this.modal.classList.add("show");
    }, 10);
  }

  /**
   * 달력 숨기기
   */
  hide() {
    this.modal.classList.remove("show");
    setTimeout(() => {
      this.modal.style.display = "none";
    }, 300);
  }

  /**
   * 월/년도 선택 옵션 생성
   */
  populateSelects() {
    const monthSelect = this.modal.querySelector("#month-select");
    const yearSelect = this.modal.querySelector("#year-select");

    const currentMonth = this.currentDate.getMonth();
    const currentYear = this.currentDate.getFullYear();

    // 월 옵션
    monthSelect.innerHTML = "";
    const months = [
      "1월",
      "2월",
      "3월",
      "4월",
      "5월",
      "6월",
      "7월",
      "8월",
      "9월",
      "10월",
      "11월",
      "12월",
    ];
    months.forEach((month, index) => {
      const option = document.createElement("option");
      option.value = index;
      option.textContent = month;
      option.selected = index === currentMonth; // 생성할 때 바로 선택
      console.log("Appending month option:", month);
      monthSelect.appendChild(option);
    });

    // 년도 옵션
    yearSelect.innerHTML = "";
    const todayYear = new Date().getFullYear();

    // (현재 기준으로만 고정된 범위 설정)
    let startYear = this.restrictions.minYear || todayYear - 10;
    let endYear = this.restrictions.maxYear || todayYear + 10;

    if (this.restrictions.currentYearOnly) {
      startYear = todayYear;
      endYear = todayYear;
    }

    for (let year = startYear; year <= endYear; year++) {
      const option = document.createElement("option");
      option.value = year;
      option.textContent = year + "년";
      option.selected = year === currentYear;
      yearSelect.appendChild(option);
    }
  }

  /**
   * 달력 렌더링
   */
  renderCalendar() {
    const monthSelect = this.modal.querySelector("#month-select");
    const yearSelect = this.modal.querySelector("#year-select");

    const year = this.currentDate.getFullYear();
    const month = this.currentDate.getMonth();

    // 연도 옵션 자동 확장 (앞뒤 2년 추가)
    const existingYears = [...yearSelect.options].map((opt) =>
      parseInt(opt.value)
    );
    const minYear = year - 2;
    const maxYear = year + 2;

    for (let y = minYear; y <= maxYear; y++) {
      if (!existingYears.includes(y)) {
        const option = document.createElement("option");
        option.value = y;
        option.textContent = y + "년";
        yearSelect.appendChild(option);
      }
    }

    // 연도 옵션 정렬 (숫자 기준 오름차순)
    const sortedOptions = [...yearSelect.options].sort(
      (a, b) => parseInt(a.value) - parseInt(b.value)
    );
    yearSelect.innerHTML = "";
    sortedOptions.forEach((opt) => yearSelect.appendChild(opt));

    // 값만 설정
    monthSelect.value = month;
    yearSelect.value = year;

    const calendarGrid = this.modal.querySelector("#calendar-grid");
    calendarGrid.innerHTML = "";

    // 첫 번째 날과 마지막 날 계산
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const startDate = new Date(firstDay);
    startDate.setDate(startDate.getDate() - firstDay.getDay());

    // 달력 그리드 생성 (6주)
    for (let week = 0; week < 6; week++) {
      const weekRow = document.createElement("div");
      weekRow.className = "datepicker-week";

      for (let day = 0; day < 7; day++) {
        const currentDate = new Date(startDate);
        currentDate.setDate(startDate.getDate() + week * 7 + day);

        const dayElement = document.createElement("div");
        dayElement.className = "datepicker-day";
        dayElement.textContent = currentDate.getDate();

        // 현재 월이 아닌 날짜는 비활성화
        if (currentDate.getMonth() !== month) {
          dayElement.classList.add("other-month");
        }

        // 오늘 날짜 표시
        if (this.isToday(currentDate)) {
          dayElement.classList.add("today");
        }

        // 선택된 날짜 표시
        if (this.isSameDay(currentDate, this.selectedDate)) {
          dayElement.classList.add("selected");
        }

        // 클릭 이벤트
        dayElement.addEventListener("click", () => {
          if (!dayElement.classList.contains("other-month")) {
            this.selectDate(currentDate);
          }
        });

        weekRow.appendChild(dayElement);
      }

      calendarGrid.appendChild(weekRow);
    }
  }

  /**
   * 날짜 선택
   */
  selectDate(date) {
    this.selectedDate = new Date(date);

    // 기존 선택된 날짜 스타일 제거
    this.modal.querySelectorAll(".datepicker-day.selected").forEach((el) => {
      el.classList.remove("selected");
    });

    // 새로 선택된 날짜에 스타일 적용
    const dayElements = this.modal.querySelectorAll(".datepicker-day");
    dayElements.forEach((el) => {
      if (
        parseInt(el.textContent) === date.getDate() &&
        !el.classList.contains("other-month")
      ) {
        el.classList.add("selected");
      }
    });
  }

  /**
   * 날짜 확인
   */
  confirm() {
    if (this.onSelectCallback) {
      const dateString = this.formatDate(this.selectedDate);
      this.onSelectCallback(dateString);
    }
    this.hide();
  }

  /**
   * 날짜 포맷팅 (YYYY-MM-DD)
   */
  formatDate(date) {
    return (
      date.getFullYear() +
      "-" +
      String(date.getMonth() + 1).padStart(2, "0") +
      "-" +
      String(date.getDate()).padStart(2, "0")
    );
  }

  /**
   * 오늘 날짜인지 확인
   */
  isToday(date) {
    const today = new Date();
    return this.isSameDay(date, today);
  }

  /**
   * 같은 날짜인지 확인
   */
  isSameDay(date1, date2) {
    return (
      date1.getFullYear() === date2.getFullYear() &&
      date1.getMonth() === date2.getMonth() &&
      date1.getDate() === date2.getDate()
    );
  }

  /**
   * 날짜 제한 설정
   */
  setRestrictions(options) {
    Object.assign(this.restrictions, options);
  }
}

/**
 * 글로벌 날짜 선택기 인스턴스
 */
window.globalDatePicker = new DatePicker();
