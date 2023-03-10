<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input :placeholder="$t('table.roleName')" v-model="listQuery.roleName" style="width: 200px;" class="filter-item" @keyup.enter.native="handleFilter"/>
      <el-button v-waves class="filter-item" type="primary" icon="el-icon-search" @click="handleFilter">{{ $t('table.search') }}</el-button>
      <el-button v-if="role_btn_add" class="filter-item" style="margin-left: 10px;" icon="el-icon-check" type="primary" @click="handleCreate">{{ $t('table.add') }}</el-button>
      <el-button v-if="role_btn_del" class="filter-item" icon="el-icon-delete" type="danger" @click="handleDeletes">{{ $t('table.del') }}</el-button>
    </div>

    <el-table
      v-loading="listLoading"
      :key="tableKey"
      :data="list"
      :default-sort="{ prop: 'id', order: 'descending' }"
      highlight-current-row
      style="width: 100%;"
      @selection-change="handleSelectionChange"
      @sort-change="sortChange">
      <el-table-column type="selection" width="55"/>
      <el-table-column :label="$t('table.roleCode')" sortable prop="role_code">
        <template slot-scope="scope">
          <span>{{ scope.row.roleCode }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('table.roleName')">
        <template slot-scope="scope">
          <span>{{ scope.row.roleName }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('table.roleDesc')">
        <template slot-scope="scope">
          <span>{{ scope.row.roleDesc }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('table.isDefault')">
        <template slot-scope="scope">
          {{ scope.row.isDefault | isDefaultFilter }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('table.status')" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status | statusTypeFilter" effect="dark" size="small">{{ scope.row.status | statusFilter }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('table.actions')" class-name="status-col" width="300px">
        <template slot-scope="scope">
          <el-button v-if="role_btn_edit" type="primary" size="mini" @click="handleUpdate(scope.row)">{{ $t('table.edit') }}</el-button>
          <el-button v-if="role_btn_auth" type="success" size="mini" @click="handlePermission(scope.row)">{{ $t('table.permission') }}</el-button>
          <el-button v-if="role_btn_del" type="danger" size="mini" @click="handleDelete(scope.row)">{{ $t('table.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination v-show="total>0" :current-page="listQuery.pageNum" :page-sizes="[10,20,30, 50]" :page-size="listQuery.pageSize" :total="total" background layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange"/>
    </div>

    <el-dialog :title="textMap[dialogStatus]" :visible.sync="dialogFormVisible" width="40%" top="10vh">
      <el-form ref="dataForm" :rules="rules" :model="temp" :label-position="labelPosition" label-width="100px">
        <el-form-item :label="$t('table.roleCode')" prop="roleCode">
          <el-input v-model="temp.roleCode" :readonly="temp.readonly"/>
        </el-form-item>
        <el-form-item :label="$t('table.roleName')" prop="roleName">
          <el-input v-model="temp.roleName"/>
        </el-form-item>
        <el-form-item :label="$t('table.roleDesc')" prop="roleDesc">
          <el-input v-model="temp.roleDesc"/>
        </el-form-item>
        <el-form-item :label="$t('table.isDefault')">
          <el-radio-group v-model="temp.isDefault">
            <el-radio :label="1">???</el-radio>
            <el-radio :label="0">???</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('table.status')">
          <el-radio-group v-model="temp.status">
            <el-radio :label="0">??????</el-radio>
            <el-radio :label="1">??????</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">{{ $t('table.cancel') }}</el-button>
        <el-button v-if="dialogStatus=='create'" type="primary" @click="createData">{{ $t('table.confirm') }}</el-button>
        <el-button v-else type="primary" @click="updateData">{{ $t('table.confirm') }}</el-button>
      </div>
    </el-dialog>

    <el-dialog :visible.sync="dialogPermissionVisible" title="????????????" width="40%" top="10vh">
      <el-tree
        ref="menuTree"
        :data="treePermissionData"
        :props="permissionProps"
        :default-checked-keys="checkedKeys"
        show-checkbox
        class="filter-tree"
        node-key="id"
        highlight-current
        check-strictly
      />
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogPermissionVisible = false">{{ $t('table.cancel') }}</el-button>
        <el-button v-if="role_btn_edit" type="primary" @click="savePermission(id, roleCode)">??????</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import { fetchList, addObj, putObj, delObj, fetchRoleTree, permissionUpdate, delAllObj } from '@/api/admin/role'
import { fetchTree } from '@/api/admin/menu'
import waves from '@/directive/waves'
import { mapGetters } from 'vuex'
import { checkMultipleSelect, notifySuccess, messageSuccess } from '@/utils/util'

export default {
  name: 'RoleManagement',
  directives: {
    waves
  },
  filters: {
    statusTypeFilter (status) {
      const statusMap = {
        0: 'success',
        1: 'danger'
      }
      return statusMap[status]
    },
    statusFilter (status) {
      return status === 0 ? '??????' : '??????'
    },
    isDefaultFilter (status) {
      return status === 1 ? '???' : '???'
    }
  },
  data () {
    return {
      tableKey: 0,
      list: null,
      total: null,
      listLoading: true,
      listQuery: {
        pageNum: 1,
        pageSize: 10,
        roleName: undefined,
        sort: 'id',
        order: 'descending'
      },
      temp: {
        id: '',
        roleName: '',
        roleCode: '',
        roleDesc: '',
        status: 0,
        isDefault: 0
      },
      treeData: [],
      treePermissionData: [],
      checkedKeys: [],
      multipleSelection: [],
      dialogFormVisible: false,
      dialogPermissionVisible: false,
      dialogStatus: '',
      textMap: {
        update: '??????',
        create: '??????'
      },
      pvData: [],
      rules: {
        roleName: [{ required: true, message: '?????????????????????', trigger: 'change' }],
        roleCode: [{ required: true, message: '?????????????????????', trigger: 'change' }]
      },
      downloadLoading: false,
      permissionProps: {
        children: 'children',
        lable: 'name'
      },
      labelPosition: 'right',
      role_btn_add: false,
      role_btn_edit: false,
      role_btn_del: false,
      role_btn_auth: false
    }
  },
  created () {
    this.getList()
    this.role_btn_add = this.permissions['sys:role:add']
    this.role_btn_edit = this.permissions['sys:role:edit']
    this.role_btn_del = this.permissions['sys:role:del']
    this.role_btn_auth = this.permissions['sys:role:auth']
  },
  computed: {
    ...mapGetters([
      'elements',
      'permissions'
    ])
  },
  methods: {
    getList () {
      this.listLoading = true
      fetchList(this.listQuery).then(response => {
        this.list = response.data.list
        this.total = parseInt(response.data.total)
        // Just to simulate the time of the request
        setTimeout(() => {
          this.listLoading = false
        }, 500)
      }).catch(() => {
        this.listLoading = false
      })
    },
    handleFilter () {
      this.listQuery.pageNum = 1
      this.getList()
    },
    handleSizeChange (val) {
      this.listQuery.limit = val
      this.getList()
    },
    handleCurrentChange (val) {
      this.listQuery.pageNum = val
      this.getList()
    },
    handleModifyStatus (row, status) {
      row.status = status
      putObj(row).then(() => {
        this.dialogFormVisible = false
        messageSuccess(this, '????????????')
      })
    },
    handleSelectionChange (val) {
      this.multipleSelection = val
    },
    sortChange (column, prop, order) {
      this.listQuery.sort = column.prop
      this.listQuery.order = column.order
      this.getList()
    },
    resetTemp () {
      this.temp = {
        id: '',
        roleName: '',
        roleCode: '',
        roleDesc: '',
        status: 0,
        isDefault: 0
      }
    },
    handleCreate () {
      this.resetTemp()
      this.dialogStatus = 'create'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate()
      })
    },
    createData () {
      this.$refs['dataForm'].validate((valid) => {
        if (valid) {
          addObj(this.temp).then(() => {
            this.list.unshift(this.temp)
            this.dialogFormVisible = false
            this.getList()
            notifySuccess(this, '????????????')
          })
        }
      })
    },
    handleUpdate (row) {
      this.temp = Object.assign({}, row) // copy obj
      this.temp.readonly = true
      this.dialogStatus = 'update'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate()
      })
    },
    updateData () {
      this.$refs['dataForm'].validate((valid) => {
        if (valid) {
          const tempData = Object.assign({}, this.temp)
          putObj(tempData).then(() => {
            this.dialogFormVisible = false
            this.getList()
            notifySuccess(this, '????????????')
          })
        }
      })
    },
    // ??????
    handleDelete (row) {
      this.$confirm('???????????????????', '??????', {
        confirmButtonText: '??????',
        cancelButtonText: '??????',
        type: 'warning'
      }).then(() => {
        delObj(row.id).then(() => {
          this.dialogFormVisible = false
          this.getList()
          notifySuccess(this, '????????????')
        })
      }).catch(() => {})
    },
    // ????????????
    handleDeletes () {
      if (checkMultipleSelect(this.multipleSelection, this)) {
        let ids = []
        for (let i = 0; i < this.multipleSelection.length; i++) {
          ids.push(this.multipleSelection[i].id)
        }
        this.$confirm('???????????????????', '??????', {
          confirmButtonText: '??????',
          cancelButtonText: '??????',
          type: 'warning'
        }).then(() => {
          delAllObj(ids).then(() => {
            this.dialogFormVisible = false
            this.getList()
            notifySuccess(this, '????????????')
          })
        }).catch(() => {})
      }
    },
    // ????????????
    handlePermission (row) {
      fetchRoleTree(row.roleCode)
        .then(response => {
          this.checkedKeys = response.data
          return fetchTree()
        })
        .then(response => {
          this.treePermissionData = response.data
          this.dialogPermissionVisible = true
          this.id = row.id
          this.roleCode = row.roleCode
        })
    },
    // ????????????
    savePermission (id, roleCode) {
      const keys = this.$refs.menuTree.getCheckedKeys()
      let menus = ''
      if (keys.length > 0) {
        for (let i = 0; i < keys.length; i++) {
          menus = menus + keys[i] + ','
        }
      }
      // ??????
      permissionUpdate(id, menus).then(() => {
        this.dialogPermissionVisible = false
        // ????????????
        fetchTree()
          .then(response => {
            this.treePermissionData = response.data
            return fetchRoleTree(roleCode)
          })
          .then(response => {
            this.checkedKeys = response.data
            notifySuccess(this, '????????????')
          })
      })
    }
  }
}
</script>
