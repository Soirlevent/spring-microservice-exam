<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input :placeholder="$t('table.examinationName')" v-model="listQuery.examinationName" style="width: 200px;" class="filter-item" @keyup.enter.native="handleFilter"/>
      <el-button v-waves class="filter-item" type="primary" icon="el-icon-search" @click="handleFilter">{{ $t('table.search') }}</el-button>
      <el-button v-if="exam_btn_add" class="filter-item" type="primary" style="margin-left: 10px;" icon="el-icon-check" @click="handleCreate">{{ $t('table.add') }}</el-button>
      <el-button v-if="exam_btn_del" class="filter-item" type="danger" icon="el-icon-delete" @click="handleDeletes">{{ $t('table.del') }}</el-button>
    </div>
    <spinner-loading v-if="listLoading"/>
    <!--่่ฏๅ่กจ-->
    <el-table
      :key="tableKey"
      :data="list"
      :default-sort="{ prop: 'id', order: 'descending' }"
      highlight-current-row
      style="width: 100%;"
      @selection-change="handleSelectionChange"
      @sort-change="sortChange">
      <el-table-column type="selection" width="55"/>
      <el-table-column :label="$t('table.examinationName')">
        <template slot-scope="scope">
          <span>{{ scope.row.examinationName | simpleStrFilter }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('table.examinationType')">
        <template slot-scope="scope">
          <span>{{ scope.row.type | examTypeFilter }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('table.course')">
        <template slot-scope="scope">
          <span>{{ scope.row | courseFilter }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('table.examTime')" width="300">
        <template slot-scope="scope">
          <span>{{ scope.row.startTime | fmtDate('yyyy-MM-dd hh:mm') }}~{{ scope.row.endTime | fmtDate('yyyy-MM-dd hh:mm') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('table.totalScore')">
        <template slot-scope="scope">
          <span>{{ scope.row.totalScore }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('table.status')">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status | statusTypeFilter " effect="dark" size="small">{{ scope.row.status | publicStatusFilter }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('table.actions')" class-name="status-col" width="400">
        <template slot-scope="scope">
          <el-button v-if="exam_btn_edit" type="primary" size="mini" @click="handleUpdate(scope.row)">{{ $t('table.edit') }}</el-button>
          <el-button v-if="exam_btn_edit && scope.row.status == 1" type="success" size="mini" @click="handlePublic(scope.row, 0)">{{ $t('table.public') }}</el-button>
          <el-button v-if="exam_btn_edit && scope.row.status == 0" type="info" size="mini" @click="handlePublic(scope.row, 1)">{{ $t('table.withdraw') }}</el-button>
          <el-button v-if="exam_btn_subject" type="success" size="mini" @click="handleSubjectManagement(scope.row)">{{ $t('table.subjectManagement') }}</el-button>
          <el-button v-if="exam_btn_del" type="danger" size="mini" @click="handleDelete(scope.row)">{{ $t('table.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination v-show="total>0" :current-page="listQuery.pageNum" :page-sizes="[10,20,30, 50]" :page-size="listQuery.pageSize" :total="total" background layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange"/>
    </div>

    <!--่่ฏไฟกๆฏ่กจๅ-->
    <el-dialog :title="textMap[dialogStatus]" :visible.sync="dialogFormVisible" width="70%" top="10vh">
      <el-form ref="dataForm" :rules="rules" :model="temp" :label-position="labelPosition" label-width="100px">
        <el-row>
          <el-col :span="18">
            <el-row>
              <el-col :span="24">
                <el-form-item :label="$t('table.examinationName')" prop="examinationName">
                  <el-input v-model="temp.examinationName" :readonly="temp.readonly"/>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item :label="$t('table.totalScore')" prop="totalScore">
                  <el-input v-model="temp.totalScore"/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('table.course')" prop="course.id">
                  <el-input v-model="temp.course.courseName" @focus="selectCourse"/>
                  <input v-model="temp.course.id" type="hidden">
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item :label="$t('table.startTime')" prop="startTime">
                  <el-date-picker v-model="temp.startTime" :placeholder="$t('table.startTime')" type="datetime" format="yyyy-MM-dd HH:mm" value-format="timestamp"/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('table.endTime')" prop="endTime">
                  <el-date-picker v-model="temp.endTime" :placeholder="$t('table.endTime')" type="datetime" format="yyyy-MM-dd HH:mm" value-format="timestamp"/>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item :label="$t('table.examinationType')" prop="type">
                  <el-radio-group v-model="temp.type">
                    <el-radio :label="0">ๆญฃๅผ่่ฏ</el-radio>
                    <el-radio :label="1">ๆจกๆ่่ฏ</el-radio>
                    <el-radio :label="2">ๅจ็บฟ็ปไน?</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('table.status')">
                  <el-radio-group v-model="temp.status">
                    <el-radio :label="0">ๅทฒๅๅธ</el-radio>
                    <el-radio :label="1">ๆชๅๅธ</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item :label="$t('table.attention')">
                  <el-input :autosize="{ minRows: 3, maxRows: 5}" :placeholder="$t('table.attention')" v-model="temp.attention" type="textarea"/>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item :label="$t('table.remark')">
                  <el-input :autosize="{ minRows: 3, maxRows: 5}" v-model="temp.remark" type="textarea" placeholder="ๅคๆณจ"/>
                </el-form-item>
              </el-col>
            </el-row>
          </el-col>
          <el-col :span="5" :offset="1">
            <el-upload
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :before-upload="beforeAvatarUpload"
              action="api/user/v1/attachment/upload"
              :headers="headers"
              :data="params"
              class="avatar-uploader">
              <img v-if="temp.avatarId !== null" :src="avatar" class="avatar">
              <i v-else class="el-icon-plus avatar-uploader-icon"/>
            </el-upload>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">{{ $t('table.cancel') }}</el-button>
        <el-button v-if="dialogStatus === 'create'" type="primary" @click="createData">{{ $t('table.confirm') }}</el-button>
        <el-button v-else type="primary" @click="updateData">{{ $t('table.confirm') }}</el-button>
      </div>
    </el-dialog>

    <!--่ฏพ็จ้ๆฉๅผน็ช-->
    <el-dialog :visible.sync="dialogCourseVisible" :title="$t('table.course')">
      <el-table v-loading="course.listLoading" :data="course.list" @row-dblclick="selectedCourse">
        <el-table-column :label="$t('table.courseName')" property="courseName" width="150">
          <template slot-scope="scope">
            <span>{{ scope.row.courseName }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('table.college')" property="college" width="200">
          <template slot-scope="scope">
            <span>{{ scope.row.college }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('table.major')" property="major">
          <template slot-scope="scope">
            <span>{{ scope.row.major }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('table.teacher')" property="teacher">'
          <template slot-scope="scope">
            <span>{{ scope.row.teacher }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { fetchList, addObj, putObj, delObj, delAllObj } from '@/api/exam/exam'
import { fetchCourseList } from '@/api/exam/course'
import waves from '@/directive/waves'
import { mapGetters, mapState } from 'vuex'
import { getToken } from '@/utils/auth'
import { checkMultipleSelect, isNotEmpty, notifySuccess, notifyFail, messageSuccess } from '@/utils/util'
import { delAttachment, preview } from '@/api/admin/attachment'
import Tinymce from '@/components/Tinymce'
import SpinnerLoading from '@/components/SpinnerLoading'
import Choices from '@/components/Subjects/Choices'
import MultipleChoices from '@/components/Subjects/MultipleChoices'
import ShortAnswer from '@/components/Subjects/ShortAnswer'

export default {
  name: 'ExamManagement',
  directives: {
    waves
  },
  components: { Tinymce, SpinnerLoading, Choices, MultipleChoices, ShortAnswer },
  filters: {
    courseFilter (row) {
      if (isNotEmpty(row.course) && isNotEmpty(row.course.courseName)) {
        return row.course.courseName
      }
      return ''
    }
  },
  data () {
    return {
      headers: {
        Authorization: 'Bearer ' + getToken()
      },
      params: {
        busiType: '1'
      },
      baseUrl: '/exam',
      tableKey: 0,
      list: null,
      total: null,
      listLoading: true,
      listQuery: {
        pageNum: 1,
        pageSize: 10,
        sort: 'id',
        order: 'descending'
      },
      // ่ฏพ็จ
      course: {
        listQuery: {
          pageNum: 1,
          pageSize: 10,
          sort: 'id',
          order: 'descending'
        },
        list: null,
        total: null,
        listLoading: true
      },
      // ่่ฏไธดๆถไฟกๆฏ
      temp: {
        id: '',
        examinationName: '',
        type: 0,
        attention: '',
        startTime: '',
        endTime: '',
        duration: '',
        totalScore: '',
        totalSubject: '0',
        status: 0,
        avatarId: null,
        collegeId: '',
        majorId: '',
        course: {
          id: '',
          courseName: ''
        },
        remark: ''
      },
      avatar: null,
      dialogFormVisible: false,
      dialogStatus: '',
      textMap: {
        update: '็ผ่พ',
        create: 'ๆฐๅปบ'
      },
      // ๆ?ก้ช่งๅ
      rules: {
        examinationName: [{ required: true, message: '่ฏท่พๅฅ่่ฏๅ็งฐ', trigger: 'change' }],
        courseId: [{ required: true, message: '่ฏท่พๅฅ่่ฏๆๅฑ่ฏพ็จ', trigger: 'change' }],
        startTime: [{ required: true, message: '่ฏท้ๆฉๅผๅงๆถ้ด', trigger: 'change' }],
        endTime: [{ required: true, message: '่ฏท้ๆฉ็ปๆๆถ้ด', trigger: 'change' }],
        totalScore: [{ required: true, message: '่ฏท่พๅฅๆปๅ', trigger: 'change' }]
      },
      downloadLoading: false,
      labelPosition: 'right',
      // ๆ้ฎๆ้
      exam_btn_add: false,
      exam_btn_edit: false,
      exam_btn_del: false,
      exam_btn_subject: false,
      dialogCourseVisible: false,
      courseData: [],
      // ๅค้่่ฏ
      multipleSelection: [],
      uploading: false,
      percentage: 0,
      percentageSubject: 0,
      activeName: '0'
    }
  },
  created () {
    // ๅ?่ฝฝ่่ฏๅ่กจ
    this.getList()
    // ่ทๅ่ฏพ็จๅ่กจ
    fetchCourseList(this.course.listQuery).then(response => {
      this.course.list = [{ id: '', courseName: '่ฏท้ๆฉ' }].concat(response.data.list)
      this.course.total = parseInt(response.data.total)
      this.course.listLoading = false
    })
    this.exam_btn_add = this.permissions['exam:exam:add']
    this.exam_btn_edit = this.permissions['exam:exam:edit']
    this.exam_btn_del = this.permissions['exam:exam:del']
    this.exam_btn_subject = this.permissions['exam:exam:subject']
  },
  computed: {
    ...mapGetters([
      'elements',
      'permissions'
    ]),
    ...mapState({
      sysConfig: state => state.sysConfig.sysConfig
    })
  },
  methods: {
    // ๅ?่ฝฝ่่ฏๅ่กจ
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
        messageSuccess(this, 'ๆไฝๆๅ')
      })
    },
    handleSelectionChange (val) {
      this.multipleSelection = val
    },
    // ๆๅบไบไปถ
    sortChange (column, prop, order) {
      this.listQuery.sort = column.prop
      this.listQuery.order = column.order
      this.getList()
    },
    resetTemp () {
      this.temp = {
        id: '',
        examinationName: '',
        type: 0,
        attention: '',
        startTime: '',
        endTime: '',
        duration: '',
        totalScore: '',
        status: 1,
        avatar: '',
        collegeId: '',
        majorId: '',
        course: {
          id: '',
          courseName: ''
        },
        remark: ''
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
          this.temp.totalScore = parseInt(this.temp.totalScore)
          addObj(this.temp).then(() => {
            this.list.unshift(this.temp)
            this.dialogFormVisible = false
            this.getList()
            notifySuccess(this, 'ๅๅปบๆๅ')
          })
        }
      })
    },
    handleUpdate (row) {
      this.temp = Object.assign({}, row)
      if (!isNotEmpty(this.temp.course)) {
        this.temp.course = {
          id: '',
          courseName: ''
        }
      }
      // ่ทๅๅพ็็้ข่งๅฐๅ
      if (isNotEmpty(this.temp.avatarId)) {
        preview(this.temp.avatarId).then(response => {
          this.avatar = response.data.data
        })
      }
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
            for (const v of this.list) {
              if (v.id === this.temp.id) {
                const index = this.list.indexOf(v)
                this.list.splice(index, 1, this.temp)
                break
              }
            }
            this.dialogFormVisible = false
            this.getList()
            notifySuccess(this, 'ๆดๆฐๆๅ')
          })
        }
      })
    },
    // ๅ?้ค
    handleDelete (row) {
      this.$confirm('็กฎๅฎ่ฆๅ?้คๅ?', 'ๆ็คบ', {
        confirmButtonText: '็กฎๅฎ',
        cancelButtonText: 'ๅๆถ',
        type: 'warning'
      }).then(() => {
        delObj(row.id).then(() => {
          this.dialogFormVisible = false
          this.getList()
          notifySuccess(this, 'ๅ?้คๆๅ')
        })
      }).catch(() => {})
    },
    // ๆน้ๅ?้ค
    handleDeletes () {
      if (checkMultipleSelect(this.multipleSelection, this)) {
        let ids = []
        for (let i = 0; i < this.multipleSelection.length; i++) {
          ids.push(this.multipleSelection[i].id)
        }
        this.$confirm('็กฎๅฎ่ฆๅ?้คๅ?', 'ๆ็คบ', {
          confirmButtonText: '็กฎๅฎ',
          cancelButtonText: 'ๅๆถ',
          type: 'warning'
        }).then(() => {
          delAllObj({ ids: ids }).then(() => {
            this.getList()
            notifySuccess(this, 'ๅ?้คๆๅ')
          })
        }).catch(() => {})
      }
    },
    // ้ๆฉ่ฏพ็จ
    selectCourse () {
      this.course.listLoading = true
      fetchCourseList(this.course.listQuery).then(response => {
        this.course.list = response.data.list
        this.course.total = parseInt(response.data.total)
        this.course.listLoading = false
      })
      this.dialogCourseVisible = true
    },
    // ๅๅป้ๆฉ่ฏพ็จ
    selectedCourse (row) {
      this.temp.course.id = row.id
      this.temp.course.courseName = row.courseName
      this.dialogCourseVisible = false
    },
    // ๅ?่ฝฝ้ข็ฎ
    handleSubjectManagement (row) {
      this.$router.push({
        path: `/exam/subjects/${row.id}`
      })
    },
    // ๅๅธ่่ฏ
    handlePublic (row, status) {
      const tempData = Object.assign({}, row)
      tempData.status = status
      putObj(tempData).then(() => {
        this.getList()
        notifySuccess(this, 'ๆดๆฐๆๅ')
      })
    },
    // ๅพ็ไธไผ?ๅ
    beforeAvatarUpload (file) {
      const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isJPG) {
        this.$message.error('ไธไผ?ๅคดๅๅพ็ๅช่ฝๆฏ jpg/png ๆ?ผๅผ!')
      }
      if (!isLt2M) {
        this.$message.error('ไธไผ?ๅคดๅๅพ็ๅคงๅฐไธ่ฝ่ถ่ฟ 2MB!')
      }
      return isJPG && isLt2M
    },
    // ไธไผ?ๆๅ
    handleAvatarSuccess (res, file) {
      this.$refs['dataForm'].validate((valid) => {
        if (valid) {
          if (isNotEmpty(this.temp.avatarId)) {
            // ๅ?้คๆงๅคดๅ
            delAttachment(this.temp.avatarId).then(() => {
              // ๆดๆฐๅคดๅไฟกๆฏ
              this.temp.avatarId = res.data.id
              putObj(Object.assign({}, this.temp)).then(() => {
                notifySuccess(this, 'ไธไผ?ๆๅ')
                this.dialogFormVisible = false
                this.getList()
              }).catch(() => {
                notifyFail(this, 'ไธไผ?ๅคฑ่ดฅ')
              })
            })
          } else {
            // ๆดๆฐๅคดๅไฟกๆฏ
            this.temp.avatarId = res.data.id
            putObj(Object.assign({}, this.temp)).then(() => {
              notifySuccess(this, 'ไธไผ?ๆๅ')
              this.dialogFormVisible = false
              this.getList()
            }).catch(() => {
              notifyFail(this, 'ไธไผ?ๅคฑ่ดฅ')
            })
          }
        }
      })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
  @import "../../styles/subject.scss";
</style>
