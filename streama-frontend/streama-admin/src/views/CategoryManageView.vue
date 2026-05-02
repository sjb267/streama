<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { delCategory, loadCategory, saveCategory } from '@/api/category'
import { uploadImage } from '@/api/file'

const loading = ref(false)
const saving = ref(false)
const uploadingIcon = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('create')
const dialogLevel = ref(1)
const selectedLevelOneId = ref('')

const categoryTree = ref([])
const formRef = ref(null)
const iconInputRef = ref(null)

const form = reactive({
  categoryId: '',
  pCategoryId: '0',
  categoryCode: '',
  categoryName: '',
  icon: '',
})

const rules = {
  categoryCode: [{ required: true, message: '请输入分类编码', trigger: ['blur', 'change'] }],
  categoryName: [{ required: true, message: '请输入分类名称', trigger: ['blur', 'change'] }],
  icon: [{ required: true, message: '请上传分类图标', trigger: ['change', 'blur'] }],
}

const levelOneCategories = computed(() => {
  return Array.isArray(categoryTree.value) ? categoryTree.value : []
})

const selectedLevelOne = computed(() => {
  return levelOneCategories.value.find(
    (item) => String(item.categoryId) === String(selectedLevelOneId.value),
  )
})

const levelTwoCategories = computed(() => {
  return Array.isArray(selectedLevelOne.value?.children) ? selectedLevelOne.value.children : []
})

const dialogTitle = computed(() => {
  const text = dialogLevel.value === 1 ? '一级分类' : '二级分类'
  return dialogMode.value === 'edit' ? `编辑${text}` : `新建${text}`
})

const parentLabel = computed(() => {
  if (form.pCategoryId === '0') {
    return '无（一级分类）'
  }
  const parent = levelOneCategories.value.find(
    (item) => String(item.categoryId) === String(form.pCategoryId),
  )
  return parent?.categoryName || `ID：${form.pCategoryId}`
})

onMounted(() => {
  loadCategoryTree()
})

function normalizeTree(data) {
  if (!Array.isArray(data)) {
    return []
  }

  return data.map((item) => ({
    ...item,
    children: normalizeTree(item.children || []),
  }))
}

async function loadCategoryTree() {
  loading.value = true
  try {
    const data = await loadCategory()
    categoryTree.value = normalizeTree(data)
    syncSelectedLevelOne()
  } finally {
    loading.value = false
  }
}

function syncSelectedLevelOne() {
  if (!levelOneCategories.value.length) {
    selectedLevelOneId.value = ''
    return
  }
  const exists = levelOneCategories.value.some(
    (item) => String(item.categoryId) === String(selectedLevelOneId.value),
  )
  if (!exists) {
    selectedLevelOneId.value = String(levelOneCategories.value[0].categoryId)
  }
}

function handleSelectLevelOne(row) {
  if (!row?.categoryId) {
    return
  }
  selectedLevelOneId.value = String(row.categoryId)
}

function resetForm() {
  form.categoryId = ''
  form.pCategoryId = '0'
  form.categoryCode = ''
  form.categoryName = ''
  form.icon = ''
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

function openCreateLevelOne() {
  dialogMode.value = 'create'
  dialogLevel.value = 1
  resetForm()
  form.pCategoryId = '0'
  dialogVisible.value = true
}

function openCreateLevelTwo() {
  if (!selectedLevelOne.value?.categoryId) {
    ElMessage.warning('请先选择一个一级分类')
    return
  }
  dialogMode.value = 'create'
  dialogLevel.value = 2
  resetForm()
  form.pCategoryId = String(selectedLevelOne.value.categoryId)
  dialogVisible.value = true
}

function openEdit(row, level) {
  dialogMode.value = 'edit'
  dialogLevel.value = level
  resetForm()
  form.categoryId = String(row.categoryId)
  form.pCategoryId = String(row.pCategoryId ?? 0)
  form.categoryCode = row.categoryCode || ''
  form.categoryName = row.categoryName || ''
  form.icon = row.icon || ''

  if (Number(row.pCategoryId) > 0) {
    selectedLevelOneId.value = String(row.pCategoryId)
  } else {
    selectedLevelOneId.value = String(row.categoryId)
  }

  dialogVisible.value = true
}

async function handleDelete(row) {
  const name = row.categoryName || row.categoryId
  await ElMessageBox.confirm(`确认删除分类“${name}”吗？`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })

  await delCategory(row.categoryId)
  ElMessage.success('分类删除成功')
  await loadCategoryTree()
}

function openIconPicker() {
  iconInputRef.value?.click()
}

async function handleIconFileChange(event) {
  const input = event?.target
  const file = input?.files?.[0]
  if (input) {
    input.value = ''
  }

  if (!file) {
    return
  }
  if (!file.type || !file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }

  uploadingIcon.value = true
  try {
    const sourceName = await uploadImage(file, false)
    form.icon = String(sourceName || '').trim()
    ElMessage.success('图标上传成功')
  } finally {
    uploadingIcon.value = false
  }
}

function clearIcon() {
  form.icon = ''
}

function getIconUrl(sourceName) {
  const source = String(sourceName || '').trim()
  if (!source) {
    return ''
  }
  if (/^(https?:\/\/|data:)/i.test(source)) {
    return source
  }
  const normalized = source.replace(/^\/+/, '')
  return `/admin/file/getResource?sourceName=${encodeURIComponent(normalized)}`
}

async function handleSave() {
  if (!formRef.value) {
    return
  }

  if (!form.icon.trim()) {
    ElMessage.warning('请先上传分类图标')
    return
  }

  await formRef.value.validate()

  saving.value = true
  try {
    const payload = {
      pCategoryId: Number(form.pCategoryId),
      categoryCode: form.categoryCode.trim(),
      categoryName: form.categoryName.trim(),
      icon: form.icon.trim() || undefined,
    }

    if (dialogMode.value === 'edit') {
      payload.categoryId = Number(form.categoryId)
    }

    await saveCategory(payload)
    ElMessage.success(dialogMode.value === 'edit' ? '分类更新成功' : '分类创建成功')
    dialogVisible.value = false
    await loadCategoryTree()
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="category-page">
    <div class="split-panels">
      <section class="panel-card">
        <div class="panel-header">
          <h3>一级分类</h3>
          <el-button type="primary" @click="openCreateLevelOne">新建一级分类</el-button>
        </div>

        <el-table
          v-loading="loading"
          :data="levelOneCategories"
          row-key="categoryId"
          border
          height="560"
          highlight-current-row
          :row-class-name="
            ({ row }) =>
              String(row.categoryId) === String(selectedLevelOneId) ? 'is-selected-row' : ''
          "
          @row-click="handleSelectLevelOne"
        >
          <el-table-column label="图标" width="90" align="center">
            <template #default="{ row }">
              <img
                v-if="row.icon"
                :src="getIconUrl(row.icon)"
                class="table-icon"
                alt="分类图标"
              />
              <span v-else>--</span>
            </template>
          </el-table-column>
          <el-table-column prop="categoryCode" label="编码" min-width="140" />
          <el-table-column prop="categoryName" label="名称" min-width="160" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="openEdit(row, 1)">编辑</el-button>
              <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="panel-card">
        <div class="panel-header">
          <h3>二级分类</h3>
          <el-button type="primary" @click="openCreateLevelTwo">新建二级分类</el-button>
        </div>

        <div class="selected-tip">
          <span v-if="selectedLevelOne">当前父分类：{{ selectedLevelOne.categoryName }}</span>
          <span v-else>请先创建并选择一个一级分类</span>
        </div>

        <el-table
          v-loading="loading"
          :data="levelTwoCategories"
          row-key="categoryId"
          border
          height="520"
        >
          <el-table-column label="图标" width="90" align="center">
            <template #default="{ row }">
              <img
                v-if="row.icon"
                :src="getIconUrl(row.icon)"
                class="table-icon"
                alt="分类图标"
              />
              <span v-else>--</span>
            </template>
          </el-table-column>
          <el-table-column prop="categoryCode" label="编码" min-width="140" />
          <el-table-column prop="categoryName" label="名称" min-width="160" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="openEdit(row, 2)">编辑</el-button>
              <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="父级分类">
          <el-input :model-value="parentLabel" disabled />
        </el-form-item>

        <el-form-item label="分类编码" prop="categoryCode">
          <el-input
            v-model="form.categoryCode"
            maxlength="50"
            show-word-limit
            placeholder="请输入分类编码"
          />
        </el-form-item>

        <el-form-item label="分类名称" prop="categoryName">
          <el-input
            v-model="form.categoryName"
            maxlength="50"
            show-word-limit
            placeholder="请输入分类名称"
          />
        </el-form-item>

        <el-form-item label="分类图标" prop="icon">
          <div class="icon-uploader">
            <div class="icon-preview">
              <img v-if="form.icon" :src="getIconUrl(form.icon)" alt="图标预览" />
              <span v-else>暂无图标</span>
            </div>
            <div class="icon-actions">
              <el-button type="primary" plain :loading="uploadingIcon" @click="openIconPicker">
                {{ uploadingIcon ? '上传中...' : '上传图片' }}
              </el-button>
              <el-button :disabled="!form.icon" @click="clearIcon">清空</el-button>
            </div>
          </div>
          <input
            ref="iconInputRef"
            type="file"
            accept="image/*"
            class="hidden-input"
            @change="handleIconFileChange"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.category-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.split-panels {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.panel-card {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #ffffff;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.selected-tip {
  font-size: 13px;
  color: #606266;
  min-height: 20px;
}

.table-icon {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  object-fit: cover;
  border: 1px solid #e5e7eb;
}

:deep(.is-selected-row) {
  --el-table-tr-bg-color: #ecf5ff;
}

.icon-uploader {
  display: flex;
  gap: 12px;
  width: 100%;
}

.icon-preview {
  width: 96px;
  height: 96px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #909399;
  font-size: 13px;
  flex-shrink: 0;
}

.icon-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.icon-actions {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hidden-input {
  display: none;
}

@media (max-width: 1200px) {
  .split-panels {
    grid-template-columns: 1fr;
  }
}
</style>
