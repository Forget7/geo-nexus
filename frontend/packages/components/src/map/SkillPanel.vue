<template>
  <div class="skill-panel">
    <div class="panel-header">
      <h3>⚡ {{ t('skill.title') }}</h3>
    </div>

    <!-- 技能列表 -->
    <div class="section">
      <div class="section-actions">
        <button @click="showRegister = true" class="create-btn">+ {{ t('skill.register') }}</button>
      </div>
      <div class="skill-list">
        <div v-for="skill in skills" :key="skill.id" class="skill-item">
          <div class="skill-info">
            <strong>{{ skill.name }}</strong>
            <div class="skill-meta">
              <span class="category-tag">{{ skill.category }}</span>
              <span v-if="skill.enabled" class="status-on">ON</span>
              <span v-else class="status-off">OFF</span>
            </div>
          </div>
          <div class="skill-actions">
            <button @click="unregister(skill.id)" class="del-btn">{{ t('skill.delete') }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 注册弹窗 -->
    <div v-if="showRegister" class="modal-overlay" @click.self="showRegister = false">
      <div class="modal">
        <h3>{{ t('skill.registerSkill') }}</h3>
        <input v-model="newSkill.name" :placeholder="t('skill.name')" class="input" />
        <input v-model="newSkill.category" :placeholder="t('skill.category')" class="input" />
        <textarea v-model="newSkill.description" :placeholder="t('skill.description')" class="input" rows="2"></textarea>
        <div class="modal-actions">
          <button @click="register">{{ t('skill.register') }}</button>
          <button @click="showRegister = false" class="cancel-btn">{{ t('skill.cancel') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const t = (key: string) => key
const skills = ref<any[]>([])
const showRegister = ref(false)
const newSkill = ref({ name: '', category: '', description: '' })

onMounted(() => fetchSkills())

async function fetchSkills() {
  try {
    const { data } = await apiClient.get('/skills')
    skills.value = data
  } catch {}
}

async function register() {
  try {
    await apiClient.post('/skills', newSkill.value)
    newSkill.value = { name: '', category: '', description: '' }
    showRegister.value = false
    await fetchSkills()
  } catch {}
}

async function unregister(id: string) {
  if (!confirm(t('skill.confirmDelete'))) return
  await apiClient.delete(`/skills/${id}`)
  await fetchSkills()
}
</script>

<style scoped>
.skill-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); max-height: 80vh; overflow: hidden; display: flex; flex-direction: column; }
.panel-header { padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; }
.section { flex: 1; overflow-y: auto; padding: 12px 16px; }
.section-actions { margin-bottom: 12px; }
.create-btn { padding: 8px 16px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; }
.skill-list { display: flex; flex-direction: column; gap: 8px; }
.skill-item { display: flex; justify-content: space-between; align-items: center; padding: 10px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; }
.skill-info { flex: 1; }
.skill-info strong { font-size: 13px; }
.skill-meta { display: flex; gap: 6px; margin-top: 4px; }
.category-tag { padding: 2px 8px; background: #f0fdf4; color: #16a34a; border-radius: 4px; font-size: 11px; }
.status-on { padding: 2px 8px; background: #dcfce7; color: #16a34a; border-radius: 4px; font-size: 10px; font-weight: 700; }
.status-off { padding: 2px 8px; background: #f1f5f9; color: #94a3b8; border-radius: 4px; font-size: 10px; font-weight: 700; }
.skill-actions { display: flex; gap: 6px; }
.del-btn { padding: 4px 10px; border: 1px solid #fecaca; border-radius: 4px; background: white; color: #dc2626; cursor: pointer; font-size: 11px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: white; border-radius: 12px; padding: 24px; width: 400px; }
.modal h3 { font-size: 16px; margin-bottom: 16px; }
.input { width: 100%; padding: 10px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; margin-bottom: 8px; box-sizing: border-box; }
.modal-actions { display: flex; gap: 8px; justify-content: flex-end; }
.modal-actions button { padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 13px; border: none; }
.modal-actions button:first-child { background: #2563eb; color: white; }
.cancel-btn { background: #f1f5f9; color: #64748b; }
</style>
