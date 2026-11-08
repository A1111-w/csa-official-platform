<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { userService } from '@/services/user'
import { useAuthStore } from '@/store/useAuthStore'
import { getRoleLabel } from '@/lib/access'
import { notify } from '@/lib/notify'
import { downloadBlob, errorText } from './helpers'

const auth = useAuthStore()
const canExport = computed(() => (auth.user?.roleLevel ?? 0) >= 4)
const availableColumns = [
  ['realName', '姓名'], ['studentId', '学号'], ['college', '学院'], ['className', '班级'],
  ['phone', '手机号'], ['roleLevel', '角色等级'], ['username', '系统账号'], ['contact', '其他联系方式'],
  ['usedInviteCode', '已用邀请码'], ['createTime', '注册时间'], ['merchantNo', '支付单号'],
] as const
const defaultColumns = () => availableColumns.slice(0, 6).map(([key]) => key) as string[]
const columns = ref(defaultColumns())
const emptyFilters = () => ({ startDate: '', endDate: '', college: '', className: '', realName: '', studentId: '', roleLevel: '', inviteCode: '' })
const filters = reactive(emptyFilters())
const exporting = ref(false)
const textFields = [['college', '学院'], ['className', '班级'], ['realName', '姓名'], ['studentId', '学号'], ['inviteCode', '邀请码']] as const
function reset() { Object.assign(filters, emptyFilters()); columns.value = defaultColumns() }
async function exportMembers() {
  if (!canExport.value || exporting.value) return
  if (!columns.value.length) { notify.error('请至少选择一个导出列'); return }
  if (filters.startDate && filters.endDate && filters.startDate > filters.endDate) { notify.error('开始日期不能晚于结束日期'); return }
  exporting.value = true
  try {
    const file = await userService.exportMembers({
      columns: [...columns.value], startTime: filters.startDate ? `${filters.startDate} 00:00:00` : undefined,
      endTime: filters.endDate ? `${filters.endDate} 23:59:59` : undefined,
      college: filters.college.trim() || undefined, className: filters.className.trim() || undefined,
      realName: filters.realName.trim() || undefined, studentId: filters.studentId.trim() || undefined,
      roleLevel: filters.roleLevel === '' ? undefined : Number(filters.roleLevel), inviteCode: filters.inviteCode.trim() || undefined,
    })
    if (file.type.includes('json')) {
      const data = JSON.parse(await file.text()) as { message?: string }
      throw new Error(data.message || '成员导出失败')
    }
    downloadBlob(file, `CSA-members-${new Date().toISOString().slice(0, 10)}.xlsx`)
    notify.success('成员名单已导出')
  } catch (e) { notify.error(errorText(e)) }
  finally { exporting.value = false }
}
</script>

<template>
  <section class="page stack">
    <h1>成员名单导出</h1>
    <p v-if="!canExport" class="panel">该页面仅向会长及以上开放。</p>
    <form v-else class="stack" @submit.prevent="exportMembers">
      <p class="muted">导出操作会写入管理审计日志。密码、Token 和验证码不在可选列中。</p>
      <fieldset class="panel" :disabled="exporting"><legend>导出列（已选 {{ columns.length }} 列）</legend><div class="form-grid"><label v-for="[key, label] in availableColumns" :key="key"><input v-model="columns" type="checkbox" :value="key" /> {{ label }}</label></div></fieldset>
      <fieldset class="panel" :disabled="exporting"><legend>筛选条件</legend><div class="form-grid">
        <label class="field">开始日期<input v-model="filters.startDate" type="date" /></label><label class="field">结束日期<input v-model="filters.endDate" type="date" /></label>
        <label class="field">角色等级<select v-model="filters.roleLevel"><option value="">全部成员</option><option v-for="level in [0, 1, 2, 3, 4, 5, 99]" :key="level" :value="String(level)">{{ getRoleLabel(level) }} (Level {{ level }})</option></select></label>
        <label v-for="[key, label] in textFields" :key="key" class="field">{{ label }}<input v-model="filters[key]" /></label>
      </div></fieldset>
      <div class="actions"><button :disabled="exporting || !columns.length">{{ exporting ? '正在导出…' : '导出 Excel' }}</button><button type="button" class="secondary" :disabled="exporting" @click="reset">重置</button></div>
    </form>
  </section>
</template>
