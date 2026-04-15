<template>
  <div class="profile-panel">
    <div class="panel-header">
      <h3>👤 {{ t('user.profile') }}</h3>
    </div>

    <!-- 用户信息 -->
    <div class="section" v-if="user">
      <div class="user-info">
        <div class="avatar">{{ user.username?.charAt(0).toUpperCase() || '?' }}</div>
        <div class="user-details">
          <strong>{{ user.username }}</strong>
          <span class="user-email">{{ user.email }}</span>
          <span :class="['user-status', user.status]">{{ user.status }}</span>
        </div>
      </div>
    </div>

    <!-- 角色 -->
    <div class="section">
      <h4>{{ t('user.roles') }}</h4>
      <div class="role-list">
        <span v-for="role in user?.roles" :key="role" class="role-tag">{{ role }}</span>
        <span v-if="!user?.roles?.length" class="no-data">{{ t('user.noRoles') }}</span>
      </div>
    </div>

    <!-- 操作 -->
    <div class="section">
      <h4>{{ t('user.actions') }}</h4>
      <div class="action-list">
        <button @click="showChangePwd = true" class="action-btn">{{ t('user.changePassword') }}</button>
        <button @click="logout" class="action-btn danger">{{ t('user.logout') }}</button>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <div v-if="showChangePwd" class="modal-overlay" @click.self="showChangePwd = false">
      <div class="modal">
        <h3>{{ t('user.changePassword') }}</h3>
        <input v-model="pwd.old" type="password" :placeholder="t('user.oldPassword')" class="input" />
        <input v-model="pwd.new" type="password" :placeholder="t('user.newPassword')" class="input" />
        <div class="modal-actions">
          <button @click="changePassword">{{ t('user.confirm') }}</button>
          <button @click="showChangePwd = false" class="cancel-btn">{{ t('user.cancel') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const t = (key: string) => key
const user = ref<any>(null)
const showChangePwd = ref(false)
const pwd = ref({ old: '', new: '' })

onMounted(async () => {
  try {
    const token = localStorage.getItem('token')
    if (token) {
      const { data } = await apiClient.get('/auth/validate', {
        headers: { Authorization: 'Bearer ' + token }
      })
      user.value = data
    }
  } catch {}
})

async function changePassword() {
  try {
    await apiClient.post(`/users/${user.value.id}/password`, {
      oldPassword: pwd.value.old,
      newPassword: pwd.value.new
    })
    showChangePwd.value = false
    pwd.value = { old: '', new: '' }
  } catch {}
}

async function logout() {
  const token = localStorage.getItem('token')
  if (token) await apiClient.post('/auth/logout', { accessToken: token })
  localStorage.removeItem('token')
  user.value = null
}
</script>

<style scoped>
.profile-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); width: 320px; }
.panel-header { padding: 14px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; }
.section { padding: 14px 16px; border-bottom: 1px solid #f1f5f9; }
.section h4 { font-size: 12px; font-weight: 600; color: #64748b; margin-bottom: 8px; text-transform: uppercase; }
.user-info { display: flex; gap: 12px; align-items: center; }
.avatar { width: 48px; height: 48px; background: linear-gradient(135deg, #2563eb, #7c3aed); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-size: 20px; font-weight: 700; flex-shrink: 0; }
.user-details { display: flex; flex-direction: column; gap: 2px; }
.user-details strong { font-size: 15px; }
.user-email { font-size: 12px; color: #64748b; }
.user-status { display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600; width: fit-content; }
.user-status.ACTIVE { background: #dcfce7; color: #16a34a; }
.user-status.INACTIVE { background: #f1f5f9; color: #94a3b8; }
.role-list { display: flex; flex-wrap: wrap; gap: 6px; }
.role-tag { padding: 4px 10px; background: #e0e7ff; color: #3730a3; border-radius: 6px; font-size: 12px; font-weight: 500; }
.no-data { font-size: 12px; color: #94a3b8; }
.action-list { display: flex; flex-direction: column; gap: 8px; }
.action-btn { width: 100%; padding: 9px; border: 1px solid #e2e8f0; border-radius: 6px; background: white; cursor: pointer; font-size: 13px; text-align: center; }
.action-btn:hover { background: #f8fafc; }
.action-btn.danger { color: #dc2626; border-color: #fecaca; }
.action-btn.danger:hover { background: #fef2f2; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: white; border-radius: 12px; padding: 24px; width: 360px; }
.modal h3 { font-size: 16px; margin-bottom: 16px; }
.input { width: 100%; padding: 10px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; margin-bottom: 8px; box-sizing: border-box; }
.modal-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 8px; }
.modal-actions button { padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 13px; border: none; }
.modal-actions button:first-child { background: #2563eb; color: white; }
.cancel-btn { background: #f1f5f9; color: #64748b; }
</style>
