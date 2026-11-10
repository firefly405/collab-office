<template>
  <div class="formula-overlay" v-if="visible" @click.self="$emit('update:visible', false)">
    <aside class="formula-panel">
      <!-- Header & Search -->
      <div class="panel-header">
        <div class="panel-title-row">
          <h3 class="panel-title">公式库与函数</h3>
          <button class="panel-close" @click="$emit('update:visible', false)">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="search-wrap">
          <span class="material-symbols-outlined search-icon">search</span>
          <input
            v-model="search"
            class="search-input"
            type="text"
            placeholder="输入函数名称搜索..."
          />
        </div>
      </div>

      <!-- Content Area -->
      <div class="panel-content">
        <!-- Categories -->
        <div class="categories-section">
          <h4 class="section-label">函数分类</h4>
          <div class="categories-grid">
            <button
              v-for="cat in categories"
              :key="cat.key"
              class="cat-btn"
              :class="{ active: activeCat === cat.key }"
              @click="activeCat = cat.key"
            >
              <span class="material-symbols-outlined cat-icon">{{ cat.icon }}</span>
              <span>{{ cat.label }}</span>
            </button>
          </div>
        </div>

        <!-- Function List -->
        <div class="function-list-section">
          <h4 class="section-label">函数列表</h4>
          <div class="function-list">
            <div
              v-for="fn in filteredFormulas"
              :key="fn.name"
              class="fn-item"
              :class="{ active: selectedFn?.name === fn.name }"
              @click="selectFunction(fn)"
            >
              <div class="fn-item-header">
                <span class="fn-name">{{ fn.name }}</span>
                <span class="material-symbols-outlined fn-info-icon">info</span>
              </div>
              <p class="fn-brief">{{ fn.description }}</p>
            </div>
            <div v-if="filteredFormulas.length === 0" class="fn-empty">未找到匹配的函数</div>
          </div>
        </div>

        <!-- Detail Card -->
        <div v-if="selectedFn" class="detail-card">
          <div class="detail-header">
            <span class="detail-icon-wrap">
              <span class="material-symbols-outlined">functions</span>
            </span>
            <span class="detail-fn-name">{{ selectedFn.name }}</span>
          </div>
          <div class="detail-body">
            <div class="detail-field">
              <label class="detail-label">语法</label>
              <div class="detail-syntax">{{ selectedFn.syntax }}</div>
            </div>
            <div class="detail-field">
              <label class="detail-label">功能描述</label>
              <p class="detail-desc">{{ selectedFn.description }}</p>
            </div>
            <div v-if="selectedFn.example" class="detail-field">
              <label class="detail-label">示例</label>
              <div class="detail-example">{{ selectedFn.example }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer Action -->
      <div class="panel-footer">
        <button class="insert-btn" @click="insertFormula">
          <span class="material-symbols-outlined">add_circle</span>
          插入函数到单元格
        </button>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

defineProps<{ visible: boolean }>()
const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void
  (e: 'insert', formula: string): void
}>()

const search = ref('')
const activeCat = ref('all')
const selectedFn = ref<any>(null)

const categories = [
  { key: 'all', label: '常用', icon: 'star' },
  { key: 'math', label: '数学', icon: 'calculate' },
  { key: 'text', label: '文本', icon: 'text_fields' },
  { key: 'logic', label: '逻辑', icon: 'account_tree' },
  { key: 'date', label: '日期', icon: 'calendar_today' },
  { key: 'lookup', label: '查找与引用', icon: 'manage_search' },
  { key: 'stat', label: '统计', icon: 'bar_chart' },
]

const formulas = [
  { name: 'SUM', category: '数学', syntax: 'SUM(value1, [value2], ...)', description: '计算单元格区域中所有数字的和。', example: '=SUM(A1:A5) 将 A1 到 A5 单元格的所有数值相加。' },
  { name: 'AVERAGE', category: '统计', syntax: 'AVERAGE(number1, number2, ...)', description: '返回其参数的平均值（算术平均值）。', example: '=AVERAGE(B1:B5) 返回 3.4' },
  { name: 'MAX', category: '统计', syntax: 'MAX(number1, number2, ...)', description: '返回所有参数中的最大值。', example: '=MAX(C1:C20) 返回 89' },
  { name: 'MIN', category: '统计', syntax: 'MIN(number1, number2, ...)', description: '返回所有参数中的最小值。', example: '=MIN(D1:D20) 返回 12' },
  { name: 'COUNT', category: '统计', syntax: 'COUNT(value1, value2, ...)', description: '计算包含数字的单元格个数。', example: '=COUNT(E1:E20) 返回 15' },
  { name: 'IF', category: '逻辑', syntax: 'IF(condition, value_if_true, value_if_false)', description: '根据条件是否成立返回不同的值。', example: '=IF(A1>60, "合格", "不合格")' },
  { name: 'AND', category: '逻辑', syntax: 'AND(condition1, condition2, ...)', description: '所有条件均为真时返回 TRUE。', example: '=AND(A1>0, B1<100)' },
  { name: 'OR', category: '逻辑', syntax: 'OR(condition1, condition2, ...)', description: '任一条件为真时返回 TRUE。', example: '=OR(A1="是", B1="是")' },
  { name: 'NOT', category: '逻辑', syntax: 'NOT(condition)', description: '取反条件值。', example: '=NOT(A1=0)' },
  { name: 'CONCAT', category: '文本', syntax: 'CONCAT(text1, text2, ...)', description: '拼接多个文本字符串。', example: '=CONCAT("你好", A1) 返回 "你好世界"' },
  { name: 'LEFT', category: '文本', syntax: 'LEFT(text, num_chars)', description: '返回文本左侧指定数量的字符。', example: '=LEFT("Hello", 2) 返回 "He"' },
  { name: 'RIGHT', category: '文本', syntax: 'RIGHT(text, num_chars)', description: '返回文本右侧指定数量的字符。', example: '=RIGHT("Hello", 3) 返回 "llo"' },
  { name: 'MID', category: '文本', syntax: 'MID(text, start, num_chars)', description: '返回文本中间指定位置的字符。', example: '=MID("ABCDE", 2, 3) 返回 "BCD"' },
  { name: 'TODAY', category: '日期', syntax: 'TODAY()', description: '返回当前日期。', example: '=TODAY() 返回 2026-05-21' },
  { name: 'NOW', category: '日期', syntax: 'NOW()', description: '返回当前日期和时间。', example: '=NOW() 返回 2026-05-21 14:30:00' },
  { name: 'DATE', category: '日期', syntax: 'DATE(year, month, day)', description: '根据年月日构造日期。', example: '=DATE(2026, 5, 21)' },
  { name: 'DATEDIF', category: '日期', syntax: 'DATEDIF(start, end, unit)', description: '计算两个日期的差值。', example: '=DATEDIF(A1, B1, "D") 返回 30' },
  { name: 'VLOOKUP', category: '查找', syntax: 'VLOOKUP(key, range, col, exact)', description: '在表格的首列查找指定值，并返回所在行中指定列处的值。', example: '=VLOOKUP(A1, B:C, 2, FALSE)' },
  { name: 'HLOOKUP', category: '查找', syntax: 'HLOOKUP(key, range, row, exact)', description: '水平查找匹配值。', example: '=HLOOKUP(A1, 2:5, 3, FALSE)' },
  { name: 'INDEX', category: '查找', syntax: 'INDEX(range, row, col)', description: '按行列索引返回值。', example: '=INDEX(A1:C10, 2, 3)' },
  { name: 'MATCH', category: '查找', syntax: 'MATCH(key, range, type)', description: '返回匹配值的位置。', example: '=MATCH("X", A1:A10, 0)' },
  { name: 'ROUND', category: '数学', syntax: 'ROUND(number, digits)', description: '四舍五入到指定位数。', example: '=ROUND(3.14159, 2) 返回 3.14' },
  { name: 'ABS', category: '数学', syntax: 'ABS(number)', description: '返回绝对值。', example: '=ABS(-5) 返回 5' },
  { name: 'SQRT', category: '数学', syntax: 'SQRT(number)', description: '返回平方根。', example: '=SQRT(16) 返回 4' },
]

const filteredFormulas = computed(() => {
  let list = formulas
  if (activeCat.value !== 'all') {
    const cat = categories.find(c => c.key === activeCat.value)
    if (cat) {
      list = list.filter(f => f.category === cat.label)
    }
  }
  if (search.value.trim()) {
    const kw = search.value.trim().toLowerCase()
    list = list.filter(f =>
      f.name.toLowerCase().includes(kw) ||
      f.description.toLowerCase().includes(kw)
    )
  }
  return list
})

function selectFunction(fn: typeof formulas[0]) {
  selectedFn.value = fn
}

function insertFormula() {
  if (selectedFn.value) {
    emit('insert', selectedFn.value.name + '(')
    emit('update:visible', false)
  }
}
</script>

<style lang="scss" scoped>
// ---------- CSS Variables ----------
$c-surface: #f8f9fa;
$c-surface-container-lowest: #ffffff;
$c-surface-container-low: #f3f4f5;
$c-surface-container-high: #e7e8e9;
$c-border-subtle: #E5E6EB;
$c-outline: #737687;
$c-outline-variant: #c3c6d8;
$c-primary: #0050d6;
$c-primary-container: #2a6af9;
$c-selection-blue: rgba(51, 112, 255, 0.08);
$c-on-surface: #191c1d;
$c-on-surface-variant: #424655;
$c-secondary: #5b5e65;
$c-on-primary: #ffffff;
$c-on-tertiary: #ffffff;
$c-tertiary-container: #6f757e;
$c-text: #191c1d;
$c-text-secondary: #424655;
$c-text-muted: #737687;
$c-bg: #f8f9fa;
$c-border: #E5E6EB;
$c-brand: #0050d6;
$radius: 4px;
$radius-lg: 8px;
$radius-xl: 12px;
$gutter: 16px;
$ease-out: cubic-bezier(0.16, 1, 0.3, 1);

// ---------- Overlay ----------
.formula-overlay {
  position: fixed;
  inset: 0;
  z-index: 90;
  display: flex;
  justify-content: flex-end;
  background: rgba(0, 0, 0, 0.15);
}

// ---------- Panel ----------
.formula-panel {
  width: 380px;
  height: 100vh;
  background: $c-surface-container-lowest;
  border-left: 1px solid $c-border-subtle;
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 12px rgba(0,0,0,0.03);
  font-family: Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  color: $c-on-surface;
}

// ---------- Header ----------
.panel-header {
  padding: 20px;
  border-bottom: 1px solid $c-border-subtle;
}

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.panel-title {
  font-size: 20px;
  font-weight: 600;
  color: $c-on-surface;
  margin: 0;
}

.panel-close {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  border: none;
  background: transparent;
  color: $c-outline;
  cursor: pointer;
  border-radius: $radius;
  &:hover { color: $c-on-surface; }
  .material-symbols-outlined { font-size: 20px; }
}

.search-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 18px;
  color: $c-outline;
  pointer-events: none;
}

.search-input {
  width: 100%;
  background: $c-surface-container-low;
  border: 1px solid $c-border-subtle;
  border-radius: $radius-lg;
  padding: 10px 12px 10px 40px;
  font-size: 14px;
  outline: none;
  color: $c-on-surface;
  transition: border-color .15s, box-shadow .15s;
  box-sizing: border-box;
  &::placeholder { color: $c-outline-variant; }
  &:focus {
    border-color: $c-primary;
    box-shadow: 0 0 0 2px rgba(0, 80, 214, 0.1);
  }
}

// ---------- Content ----------
.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: $c-border-subtle; border-radius: 10px; }
}

// ---------- Section Label ----------
.section-label {
  font-size: 11px;
  font-weight: 700;
  color: $c-outline;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  padding: 8px 12px;
  margin: 0;
}

// ---------- Categories ----------
.categories-section {
  padding: 0 4px;
}

.categories-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  padding: 0 4px;
}

.cat-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: none;
  background: transparent;
  border-radius: $radius-lg;
  font-size: 12px;
  font-weight: 500;
  color: $c-secondary;
  cursor: pointer;
  transition: all .15s $ease-out;
  .cat-icon { font-size: 18px; }
  &:hover { background: $c-surface-container-high; }
  &.active {
    background: rgba(42, 106, 249, 0.1);
    color: $c-primary;
  }
}

// ---------- Function List ----------
.function-list-section {
  margin-top: 4px;
  border-top: 1px solid $c-border-subtle;
  padding-top: 16px;
}

.function-list {
  display: flex;
  flex-direction: column;
}

.fn-item {
  padding: 12px 20px;
  cursor: pointer;
  transition: background .15s;
  border-right: 3px solid transparent;
  &:hover { background: $c-surface-container-low; }
  &.active {
    background: $c-selection-blue;
    border-right-color: $c-primary;
  }
}

.fn-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 2px;
}

.fn-name {
  font-family: 'JetBrains Mono Variable', monospace;
  font-size: 14px;
  font-weight: 700;
  color: $c-primary;
}

.fn-item:not(.active) .fn-name {
  color: $c-on-surface;
}

.fn-info-icon {
  font-size: 16px;
  color: $c-outline;
  opacity: 0;
  transition: opacity .15s;
}

.fn-item:hover .fn-info-icon {
  opacity: 1;
}

.fn-brief {
  font-size: 12px;
  color: $c-secondary;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fn-empty {
  text-align: center;
  color: $c-text-muted;
  font-size: 13px;
  padding: 24px;
}

// ---------- Detail Card ----------
.detail-card {
  margin: 20px;
  padding: 20px;
  background: $c-surface-container-low;
  border-radius: $radius-xl;
  border: 1px solid $c-border-subtle;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.detail-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: $radius;
  background: $c-primary;
  display: flex;
  align-items: center;
  justify-content: center;
  .material-symbols-outlined {
    font-size: 20px;
    color: $c-on-primary;
  }
}

.detail-fn-name {
  font-size: 20px;
  font-weight: 600;
  color: $c-on-surface;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-label {
  font-size: 11px;
  font-weight: 700;
  color: $c-outline;
  text-transform: uppercase;
}

.detail-syntax {
  background: $c-surface-container-lowest;
  padding: 12px;
  border-radius: $radius-lg;
  border: 1px solid $c-border-subtle;
  font-family: 'JetBrains Mono Variable', monospace;
  font-size: 13px;
  color: $c-primary;
  line-height: 1.6;
}

.detail-desc {
  font-size: 14px;
  color: $c-on-surface-variant;
  line-height: 1.6;
  margin: 0;
}

.detail-example {
  background: $c-surface;
  padding: 12px;
  border-radius: $radius-lg;
  border: 1px solid $c-border-subtle;
  font-size: 13px;
  font-style: italic;
  color: $c-secondary;
}

// ---------- Footer ----------
.panel-footer {
  padding: 20px;
  border-top: 1px solid $c-border-subtle;
  background: $c-surface-container-lowest;
}

.insert-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  background: $c-primary;
  color: $c-on-primary;
  border: none;
  border-radius: $radius-lg;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 80, 214, 0.2);
  transition: all .15s $ease-out;
  .material-symbols-outlined { font-size: 20px; }
  &:hover { transform: scale(1.01); }
  &:active { transform: scale(0.98); }
}
</style>
