<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input :placeholder="$t('table.route.routeId')" v-model="listQuery.routeId" style="width: 200px;" class="filter-item" @keyup.enter.native="handleFilter"/>
      <el-button v-waves class="filter-item" type="primary" icon="el-icon-search" @click="handleFilter">{{ $t('table.search') }}</el-button>
      <el-button v-if="route_btn_add" class="filter-item" type="primary" style="margin-left: 10px;" icon="el-icon-check" @click="handleCreate">{{ $t('table.add') }}</el-button>
      <el-button v-if="route_btn_del" class="filter-item" type="danger" icon="el-icon-delete" @click="handleDeletes">{{ $t('table.del') }}</el-button>
      <el-button v-if="route_btn_refresh" class="filter-item" type="success" icon="el-icon-refresh" @click="handleRefreshRoute">{{ $t('table.route.refresh') }}</el-button>
    </div>
    <spinner-loading v-if="listLoading"/>
    <el-table
      :key="tableKey"
      :data="list"
      :default-sort="{ prop: 'id', order: 'descending' }"
      highlight-current-row
      style="width: 100%;"
      @selection-change="handleSelectionChange"
      @sort-change="sortChange">
      <el-table-column type="selection" width="55"/>
      <el-table-column :label="$t('table.route.routeId')" sortable prop="route_id">
        <template slot-scope="scope">
          <span>{{ scope.row.routeId }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('table.route.routeName')" min-width="80">
        <template slot-scope="scope">
          <span>{{ scope.row.routeName }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('table.route.uri')" min-width="80">
        <template slot-scope="scope">
          <span>{{ scope.row.uri }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('table.route.sort')" min-width="80">
        <template slot-scope="scope">
          <span>{{ scope.row.sort }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('table.route.status')" min-width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status | statusTypeFilter" effect="dark" size="small">{{ scope.row.status | statusFilter }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('table.modifier')" property="modifier" min-width="80">
        <template slot-scope="scope">
          <span>{{ scope.row.modifier }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('table.modifyDate')" property="updateTime" width="150">
        <template slot-scope="scope">
          <span>{{ scope.row.modifyDate | fmtDate('yyyy-MM-dd hh:mm') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('table.actions')" class-name="status-col">
        <template slot-scope="scope">
          <el-button v-if="route_btn_edit" type="primary" size="mini" @click="handleUpdate(scope.row)">{{ $t('table.edit') }}</el-button>
          <el-button v-if="route_btn_del" type="danger" size="mini" @click="handleDelete(scope.row)">{{ $t('table.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination v-show="total>0" :current-page="listQuery.pageNum" :page-sizes="[10,20,30, 50]" :page-size="listQuery.pageSize" :total="total" background layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange"/>
    </div>

    <el-dialog :title="textMap[dialogStatus]" :visible.sync="dialogFormVisible" width="60%" top="2vh">
      <el-form ref="dataForm" :rules="rules" :model="temp" :label-position="labelPosition" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item :label="$t('table.route.routeId')" prop="routeId">
              <el-input v-model="temp.routeId" placeholder="??????ID"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('table.route.routeName')" prop="routeName">
              <el-input v-model="temp.routeName" placeholder="????????????"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item :label="$t('table.route.uri')">
              <el-input v-model="temp.uri" placeholder="??????URI"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('table.route.sort')">
              <el-input v-model="temp.sort" placeholder="???????????????"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item :label="$t('table.route.predicates')">
              <el-input v-model="temp.tempPredicatesPath" placeholder="?????????????????????"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('table.route.status')">
              <el-radio-group v-model="temp.status">
                <el-radio :label="0">??????</el-radio>
                <el-radio :label="1">??????</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item :label="$t('table.route.filters')">
              <el-row class="filter-body" :gutter="10" v-for="filter in tempFilters" :key="filter.name">
                <el-col :span="6" class="filter-body-col">
                  <el-input placeholder="??????" v-model="filter.name"/>
                </el-col>
                <el-col :span="16">
                  <el-input placeholder="?????????key=value???????????????????????????" v-model="filter.args"/>
                </el-col>
              </el-row>
              <div class="filter-footer">
                <el-button type="success" size="mini" @click.prevent="addFilter()" style="display: inline-block;margin: 8px;">??????</el-button>
                <el-button type="danger" size="mini" @click.prevent="delFilter()" style="display: inline-block;margin: 8px;">??????</el-button>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">{{ $t('table.cancel') }}</el-button>
        <el-button v-if="dialogStatus=='create'" type="primary" @click="createData">{{ $t('table.confirm') }}</el-button>
        <el-button v-else type="primary" @click="updateData">{{ $t('table.confirm') }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { fetchList, addObj, putObj, delObj, delAllObj, refresh } from '@/api/admin/route'
import waves from '@/directive/waves'
import { mapGetters } from 'vuex'
import { checkMultipleSelect, notifySuccess, notifyFail, messageSuccess, messageWarn, trimComma } from '@/utils/util'
import SpinnerLoading from '@/components/SpinnerLoading'

export default {
  name: 'ClientManagement',
  components: {
    SpinnerLoading
  },
  directives: {
    waves
  },
  filters: {
    statusTypeFilter (status) {
      const statusMap = {
        0: 'success',
        1: 'warning'
      }
      return statusMap[status]
    },
    statusFilter (status) {
      return status === '0' ? '??????' : '??????'
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
        routeId: '',
        sort: 'sort',
        order: 'descending'
      },
      temp: {
        id: '',
        routeId: '',
        routeName: '',
        predicates: [],
        filters: '',
        tempPredicates: [],
        tempPredicatesPath: '',
        tempFilters: [],
        uri: '',
        sort: '',
        status: 0
      },
      tempPredicates: {
        name: 'Path',
        args: {
          _genkey_0: ''
        }
      },
      tempFilters: [],
      checkedKeys: [],
      multipleSelection: [],
      dialogFormVisible: false,
      dialogStatus: '',
      textMap: {
        update: '??????',
        create: '??????'
      },
      rules: {
        routeId: [{ required: true, message: '???????????????ID', trigger: 'change' }],
        routeName: [{ required: true, message: '?????????????????????', trigger: 'change' }]
      },
      downloadLoading: false,
      labelPosition: 'right',
      route_btn_add: false,
      route_btn_edit: false,
      route_btn_del: false,
      route_btn_refresh: false
    }
  },
  created () {
    // ??????????????????Filter
    this.initFilters()
    this.getList()
    this.route_btn_add = this.permissions['sys:route:add']
    this.route_btn_edit = this.permissions['sys:route:edit']
    this.route_btn_del = this.permissions['sys:route:del']
    this.route_btn_refresh = this.permissions['sys:route:refresh']
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
        routeId: '',
        routeName: '',
        predicates: [],
        filters: [],
        tempPredicates: [],
        tempPredicatesPath: '',
        tempFilters: [],
        uri: '',
        sort: '',
        status: 0
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
          const paths = this.temp.tempPredicatesPath.split(',')
          this.temp.predicates = this.getPredicates(paths)
          this.temp.filters = this.getFilters(this.tempFilters)
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
      this.dialogStatus = 'update'
      let tempPredicatesPath = ''
      JSON.parse(this.temp.predicates).forEach(predicate => {
        tempPredicatesPath += predicate.args._genkey_0 + ','
      })
      this.temp.tempPredicatesPath = trimComma(tempPredicatesPath)
      // ??????filters
      this.tempFilters = this.parseFilter(JSON.parse(this.temp.filters))
      this.temp.status = parseInt(this.temp.status)
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate()
      })
    },
    updateData () {
      this.$refs['dataForm'].validate((valid) => {
        if (valid) {
          const tempData = Object.assign({}, this.temp)
          const paths = tempData.tempPredicatesPath.split(',')
          tempData.predicates = this.getPredicates(paths)
          tempData.filters = this.getFilters(this.tempFilters)
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
    handleRefreshRoute () {
      this.$confirm('?????????????????????????', '??????', {
        confirmButtonText: '??????',
        cancelButtonText: '??????',
        type: 'warning'
      }).then(() => {
        refresh().then(() => {
          notifySuccess(this, '????????????')
        }).catch(() => {
          notifyFail(this, '????????????')
        })
      }).catch(() => {})
    },
    addFilter () {
      if (this.tempFilters.length === 0) {
        this.tempFilters.push({ name: '', args: '' })
      } else if (this.tempFilters[this.tempFilters.length - 1].name !== '') {
        this.tempFilters.push({ name: '', args: '' })
      } else {
        messageWarn(this, '?????????????????????')
      }
    },
    delFilter () {
      if (this.tempFilters.length > 0) {
        this.tempFilters.pop()
      } else {
        messageWarn(this, '??????????????????')
      }
    },
    initFilters () {
      // ??????filter
      this.tempFilters.push({ name: 'StripPrefix', args: '_genkey_0=2' })
      this.tempFilters.push({ name: 'RemoveRequestHeader', args: '_genkey_0=Cookie,_genkey_1=Set-Cookie' })
    },
    getPredicates (paths) {
      const predicates = paths.map(path => {
        const predicate = Object.assign(this.tempPredicates)
        predicate.args._genkey_0 = path
        return predicate
      })
      return JSON.stringify(predicates)
    },
    parseFilter (filters) {
      let result = []
      if (filters.length > 0) {
        result = filters.map(filter => {
          let args = ''
          for (let key in filter.args) {
            args = args + key + '=' + filter.args[key] + ','
          }
          return { name: filter.name, args: trimComma(args) }
        })
      }
      return result
    },
    getFilters (filters) {
      let result = filters.map(filter => {
        const tempFilter = Object.assign(filter)
        let tempArgs = Object.assign(tempFilter.args)
        const args = tempArgs.split(',')
        tempArgs = {}
        args.forEach(arg => {
          const argArr = arg.split('=')
          this.$set(tempArgs, argArr[0], argArr[1])
        })
        tempFilter.args = tempArgs
        return tempFilter
      })
      return JSON.stringify(result)
    }
  }
}
</script>

<style lang="scss" scoped>
  .filter-body {
    padding: 5px;
  }
  .filter-footer {
    text-align: center;
    .filter-body-col {
      margin-left: -5px;
    }
  }
</style>
