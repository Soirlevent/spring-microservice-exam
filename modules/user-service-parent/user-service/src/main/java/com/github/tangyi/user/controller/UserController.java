package com.github.tangyi.user.controller;

import com.github.pagehelper.PageInfo;
import com.github.tangyi.common.core.constant.CommonConstant;
import com.github.tangyi.common.core.exceptions.CommonException;
import com.github.tangyi.common.core.model.ResponseBean;
import com.github.tangyi.common.core.utils.DateUtils;
import com.github.tangyi.common.core.utils.PageUtil;
import com.github.tangyi.common.core.utils.SysUtil;
import com.github.tangyi.common.core.utils.excel.ExcelToolUtil;
import com.github.tangyi.common.core.vo.UserVo;
import com.github.tangyi.common.core.web.BaseController;
import com.github.tangyi.common.log.annotation.Log;
import com.github.tangyi.common.security.annotations.AdminTenantTeacherAuthorization;
import com.github.tangyi.common.security.constant.SecurityConstant;
import com.github.tangyi.user.api.dto.UserDto;
import com.github.tangyi.user.api.dto.UserInfoDto;
import com.github.tangyi.user.api.module.*;
import com.github.tangyi.user.excel.listener.UserImportListener;
import com.github.tangyi.user.excel.model.UserExcelModel;
import com.github.tangyi.user.service.DeptService;
import com.github.tangyi.user.service.UserAuthsService;
import com.github.tangyi.user.service.UserRoleService;
import com.github.tangyi.user.service.UserService;
import com.github.tangyi.user.utils.UserUtils;
import com.google.common.collect.Lists;
import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author tangyi
 * @date 2018-08-25 16:20
 */
@Slf4j
@AllArgsConstructor
@Api("??????????????????")
@RestController
@RequestMapping(value = "/v1/user")
public class UserController extends BaseController {

    private final UserService userService;

    private final UserRoleService userRoleService;

    private final DeptService deptService;

    private final UserAuthsService userAuthsService;

    /**
     * ??????id??????
     *
     * @param id id
     * @return ResponseBean
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "??????????????????", notes = "????????????id????????????????????????")
    @ApiImplicitParam(name = "id", value = "??????ID", required = true, dataType = "Long", paramType = "path")
    public ResponseBean<User> user(@PathVariable Long id) {
        return new ResponseBean<>(userService.get(id));
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @return ?????????
     */
    @GetMapping("info")
    @ApiOperation(value = "??????????????????", notes = "????????????????????????????????????")
    @ApiImplicitParam(name = "identityType", value = "????????????", required = true, dataType = "String")
    public ResponseBean<UserInfoDto> userInfo(@RequestParam(required = false) String identityType, OAuth2Authentication authentication) {
        try {
            UserVo userVo = new UserVo();
            if (StringUtils.isNotEmpty(identityType))
                userVo.setIdentityType(Integer.valueOf(identityType));
            userVo.setIdentifier(authentication.getName());
            userVo.setTenantCode(SysUtil.getTenantCode());
            return new ResponseBean<>(userService.findUserInfo(userVo));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CommonException("????????????????????????????????????");
        }
    }

    /**
     * ????????????????????????????????????????????????
     *
     * @param identifier   identifier
     * @param identityType identityType
     * @param tenantCode   tenantCode
     * @return ResponseBean
     */
    @GetMapping("anonymousUser/findUserByIdentifier/{identifier}")
    @ApiOperation(value = "??????????????????", notes = "????????????name????????????????????????")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "identifier", value = "??????????????????", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "identityType", value = "??????????????????", dataType = "Integer"),
            @ApiImplicitParam(name = "tenantCode", value = "????????????", required = true, dataType = "String"),
    })
    public ResponseBean<UserVo> findUserByIdentifier(@PathVariable String identifier, @RequestParam(required = false) Integer identityType, @RequestParam @NotBlank String tenantCode) {
        return new ResponseBean<>(userService.findUserByIdentifier(identityType, identifier, tenantCode));
    }

    /**
     * ??????????????????
     *
     * @param pageNum  pageNum
     * @param pageSize pageSize
     * @param sort     sort
     * @param order    order
     * @param userVo   userVo
     * @return PageInfo
     * @author tangyi
     * @date 2018/8/26 22:56
     */
    @GetMapping("userList")
    @ApiOperation(value = "??????????????????")
    @ApiImplicitParams({
            @ApiImplicitParam(name = CommonConstant.PAGE_NUM, value = "????????????", defaultValue = CommonConstant.PAGE_NUM_DEFAULT, dataType = "String"),
            @ApiImplicitParam(name = CommonConstant.PAGE_SIZE, value = "????????????", defaultValue = CommonConstant.PAGE_SIZE_DEFAULT, dataType = "String"),
            @ApiImplicitParam(name = CommonConstant.SORT, value = "????????????", defaultValue = CommonConstant.PAGE_SORT_DEFAULT, dataType = "String"),
            @ApiImplicitParam(name = CommonConstant.ORDER, value = "????????????", defaultValue = CommonConstant.PAGE_ORDER_DEFAULT, dataType = "String"),
            @ApiImplicitParam(name = "userVo", value = "????????????", dataType = "UserVo")
    })
    public PageInfo<UserDto> userList(@RequestParam(value = CommonConstant.PAGE_NUM, required = false, defaultValue = CommonConstant.PAGE_NUM_DEFAULT) String pageNum,
                                      @RequestParam(value = CommonConstant.PAGE_SIZE, required = false, defaultValue = CommonConstant.PAGE_SIZE_DEFAULT) String pageSize,
                                      @RequestParam(value = CommonConstant.SORT, required = false, defaultValue = CommonConstant.PAGE_SORT_DEFAULT) String sort,
                                      @RequestParam(value = CommonConstant.ORDER, required = false, defaultValue = CommonConstant.PAGE_ORDER_DEFAULT) String order,
                                      @RequestParam(value = "name", required = false, defaultValue = "") String name,
                                      UserVo userVo) {
        PageInfo<UserDto> userDtoPageInfo = new PageInfo<>();
        List<UserDto> userDtos = Lists.newArrayList();
        userVo.setTenantCode(SysUtil.getTenantCode());
        User user = new User();
        BeanUtils.copyProperties(userVo, user);
        user.setName(name);
        PageInfo<User> page = userService.findPage(PageUtil.pageInfo(pageNum, pageSize, sort, order), user);
        List<User> users = page.getList();
        if (CollectionUtils.isNotEmpty(users)) {
            // ??????????????????
            List<UserAuths> userAuths = userAuthsService.getListByUsers(users);
            // ??????????????????
            List<Dept> deptList = deptService.getListByUsers(users);
            // ??????????????????????????????
            List<UserRole> userRoles = userRoleService.getByUserIds(users.stream().map(User::getId).collect(Collectors.toList()));
            // ??????????????????
            List<Role> finalRoleList = userService.getUsersRoles(users);
            // ????????????
            users.forEach(tempUser -> userDtos.add(userService.getUserDtoByUserAndUserAuths(tempUser, userAuths, deptList, userRoles, finalRoleList)));
        }
        PageUtil.copyProperties(page, userDtoPageInfo);
        userDtoPageInfo.setList(userDtos);
        return userDtoPageInfo;
    }

    /**
     * ????????????
     *
     * @param userDto userDto
     * @return ResponseBean
     * @author tangyi
     * @date 2018/8/26 14:34
     */
    @PostMapping
    @AdminTenantTeacherAuthorization
    @ApiOperation(value = "????????????", notes = "????????????")
    @ApiImplicitParam(name = "userDto", value = "????????????user", required = true, dataType = "UserDto")
    @Log("????????????")
    public ResponseBean<Boolean> addUser(@RequestBody @Valid UserDto userDto) {
        return new ResponseBean<>(userService.createUser(userDto) > 0);
    }

    /**
     * ????????????
     *
     * @param id      id
     * @param userDto userDto
     * @return ResponseBean
     * @author tangyi
     * @date 2018/8/26 15:06
     */
    @PutMapping("/{id:[a-zA-Z0-9,]+}")
    @AdminTenantTeacherAuthorization
    @ApiOperation(value = "??????????????????", notes = "????????????id??????????????????????????????????????????")
    @ApiImplicitParam(name = "userDto", value = "????????????user", required = true, dataType = "UserDto")
    @Log("????????????")
    public ResponseBean<Boolean> updateUser(@PathVariable Long id, @RequestBody UserDto userDto) {
        try {
            return new ResponseBean<>(userService.updateUser(id, userDto));
        } catch (Exception e) {
            log.error("Update user failed", e);
            throw new CommonException("Update user failed");
        }
    }

    /**
     * ???????????????????????????
     *
     * @param userDto userDto
     * @return ResponseBean
     * @author tangyi
     * @date 2018/10/30 10:06
     */
    @PutMapping("updateInfo")
    @ApiOperation(value = "????????????????????????", notes = "????????????id???????????????????????????")
    @ApiImplicitParam(name = "userDto", value = "????????????user", required = true, dataType = "UserDto")
    @Log("????????????????????????")
    public ResponseBean<Boolean> updateInfo(@RequestBody UserDto userDto) {
        User user = new User();
        BeanUtils.copyProperties(userDto, user);
        user.setCommonValue(SysUtil.getUser(), SysUtil.getSysCode(), SysUtil.getTenantCode());
        return new ResponseBean<>(userService.update(user) > 0);
    }

    /**
     * ????????????
     *
     * @param userDto userDto
     * @return ResponseBean
     * @author tangyi
     * @date 2019/06/21 20:09
     */
    @PutMapping("anonymousUser/updatePassword")
    @ApiOperation(value = "??????????????????", notes = "??????????????????")
    @ApiImplicitParam(name = "userDto", value = "????????????user", required = true, dataType = "UserDto")
    @Log("??????????????????")
    public ResponseBean<Boolean> updatePassword(@RequestBody UserDto userDto) {
        return new ResponseBean<>(userService.updatePassword(userDto) > 0);
    }

    /**
     * ????????????
     *
     * @param userDto userDto
     * @return ResponseBean
     * @author tangyi
     * @date 2019/06/21 18:08
     */
    @PutMapping("updateAvatar")
    @ApiOperation(value = "??????????????????", notes = "????????????id???????????????????????????")
    @ApiImplicitParam(name = "userDto", value = "????????????user", required = true, dataType = "UserDto")
    @Log("??????????????????")
    public ResponseBean<Boolean> updateAvatar(@RequestBody UserDto userDto) {
        return new ResponseBean<>(userService.updateAvatar(userDto) > 0);
    }

    /**
     * ????????????
     *
     * @param id id
     * @return ResponseBean
     * @author tangyi
     * @date 2018/8/26 15:28
     */
    @DeleteMapping("/{id}")
    @AdminTenantTeacherAuthorization
    @ApiOperation(value = "????????????", notes = "??????ID????????????")
    @ApiImplicitParam(name = "id", value = "??????ID", required = true, paramType = "path")
    @Log("????????????")
    public ResponseBean<Boolean> deleteUser(@PathVariable Long id) {
        try {
            User user = userService.get(id);
            user.setCommonValue(SysUtil.getUser(), SysUtil.getSysCode(), SysUtil.getTenantCode());
            return new ResponseBean<>(userService.delete(user) > 0);
        } catch (Exception e) {
            log.error("Delete user failed: {}", e.getMessage(), e);
            throw new CommonException("Delete user failed");
        }
    }

    /**
     * ??????
     *
     * @param ids ids
     * @author tangyi
     * @date 2018/11/26 22:11
     */
    @PostMapping("export")
    @AdminTenantTeacherAuthorization
    @ApiOperation(value = "????????????", notes = "????????????id????????????")
    @ApiImplicitParam(name = "userVo", value = "????????????", required = true, dataType = "UserVo")
    @Log("????????????")
    public void exportUser(@RequestBody Long[] ids, HttpServletRequest request, HttpServletResponse response) {
        try {
            List<User> users;
            if (ArrayUtils.isNotEmpty(ids)) {
                users = userService.findListById(ids);
            } else {
                // ?????????????????????????????????
                User user = new User();
                user.setTenantCode(SysUtil.getTenantCode());
                users = userService.findList(user);
            }
            if (CollectionUtils.isEmpty(users))
                throw new CommonException("???????????????.");
            // ????????????????????????
            List<UserAuths> userAuths = userAuthsService.getListByUsers(users);
            // ?????????????????????dto
            List<UserInfoDto> userInfoDtos = users.stream().map(tempUser -> {
                UserInfoDto userDto = new UserInfoDto();
                userAuths.stream()
                        .filter(userAuth -> userAuth.getUserId().equals(tempUser.getId()))
                        .findFirst()
                        .ifPresent(userAuth -> UserUtils.toUserInfoDto(userDto, tempUser, userAuth));
                return userDto;
            }).collect(Collectors.toList());
			String fileName = "????????????" + DateUtils.localDateMillisToString(LocalDateTime.now());
			ExcelToolUtil.writeExcel(request, response, UserUtils.convertToExcelModel(userInfoDtos), fileName,"sheet1", UserExcelModel.class);
        } catch (Exception e) {
            log.error("Export user data failed", e);
            throw new CommonException("Export user data failed");
        }
    }

    /**
     * ????????????
     *
     * @param file file
     * @return ResponseBean
     * @author tangyi
     * @date 2018/11/28 12:44
     */
    @PostMapping("import")
    @AdminTenantTeacherAuthorization
    @ApiOperation(value = "????????????", notes = "????????????")
    @Log("????????????")
    public ResponseBean<Boolean> importUser(@ApiParam(value = "??????????????????", required = true) MultipartFile file, HttpServletRequest request) {
        try {
            return new ResponseBean<>(ExcelToolUtil.readExcel(file.getInputStream(), UserExcelModel.class, new UserImportListener(userService)));
        } catch (Exception e) {
            log.error("Import user failed", e);
            throw new CommonException("Import user failed");
        }
    }

    /**
     * ????????????
     *
     * @param ids ids
     * @return ResponseBean
     * @author tangyi
     * @date 2018/12/4 9:58
     */
    @PostMapping("deleteAll")
    @AdminTenantTeacherAuthorization
    @ApiOperation(value = "??????????????????", notes = "????????????id??????????????????")
    @ApiImplicitParam(name = "ids", value = "????????????", dataType = "Long")
    @Log("??????????????????")
    public ResponseBean<Boolean> deleteAllUsers(@RequestBody Long[] ids) {
        try {
            boolean success = Boolean.FALSE;
            if (ArrayUtils.isNotEmpty(ids))
                success = userService.deleteAll(ids) > 0;
            return new ResponseBean<>(success);
        } catch (Exception e) {
            log.error("Delete user failed", e);
            throw new CommonException("Delete user failed");
        }
    }

    /**
     * ??????ID??????
     *
     * @param ids ids
     * @return ResponseBean
     * @author tangyi
     * @date 2018/12/31 21:16
     */
    @PostMapping(value = "findById")
    @ApiOperation(value = "??????ID????????????", notes = "??????ID????????????")
    @ApiImplicitParam(name = "ids", value = "??????ID", required = true, paramType = "Long")
    public ResponseBean<List<UserVo>> findById(@RequestBody Long[] ids) {
        return new ResponseBean<>(userService.findUserVoListById(ids));
    }

    /**
     * ??????
     *
     * @param userDto userDto
     * @return ResponseBean
     * @author tangyi
     * @date 2019/01/10 22:35
     */
    @ApiOperation(value = "??????", notes = "??????")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "grant_type", value = "???????????????password???mobile???", required = true, defaultValue = "password", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "code", value = "?????????", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "randomStr", value = "?????????", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "mobile", value = "?????????", dataType = "String", paramType = "query")
    })
    @PostMapping("anonymousUser/register")
    @Log("????????????")
    public ResponseBean<Boolean> register(@RequestBody @Valid UserDto userDto) {
        return new ResponseBean<>(userService.register(userDto));
    }

    /**
     * ????????????????????????
     *
     * @param identityType identityType
     * @param identifier   identifier
     * @param tenantCode   tenantCode
     * @return ResponseBean
     * @author tangyi
     * @date 2019/04/23 15:35
     */
    @ApiOperation(value = "????????????????????????", notes = "????????????????????????")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "identityType", value = "????????????????????????", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "identifier", value = "??????????????????", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "tenantCode", value = "????????????", required = true, dataType = "String"),
    })
    @GetMapping("anonymousUser/checkExist/{identifier}")
    public ResponseBean<Boolean> checkExist(@PathVariable("identifier") String identifier, @RequestParam Integer identityType, @RequestHeader(SecurityConstant.TENANT_CODE_HEADER) String tenantCode) {
        return new ResponseBean<>(userService.checkIdentifierIsExist(identityType, identifier, tenantCode));
    }

    /**
     * ??????????????????
     *
     * @param userVo userVo
     * @return ResponseBean
     * @author tangyi
     * @date 2019/05/09 22:09
     */
    @PostMapping("userCount")
    public ResponseBean<Integer> userCount(UserVo userVo) {
        return new ResponseBean<>(userService.userCount(userVo));
    }

    /**
     * ????????????
     *
     * @param userDto userDto
     * @return ResponseBean
     * @author tangyi
     * @date 2019/6/7 12:00
     */
    @PutMapping("anonymousUser/resetPassword")
    @AdminTenantTeacherAuthorization
    @ApiOperation(value = "????????????", notes = "????????????id????????????")
    @ApiImplicitParam(name = "userDto", value = "????????????user", required = true, dataType = "UserDto")
    @Log("????????????")
    public ResponseBean<Boolean> resetPassword(@RequestBody UserDto userDto) {
        return new ResponseBean<>(userService.resetPassword(userDto));
    }

    /**
     * ???????????????????????????
     *
     * @param userDto userDto
     * @return ResponseBean
     * @author tangyi
     * @date 2020/02/29 16:55
     */
    @PutMapping("anonymousUser/updateLoginInfo")
    @ApiOperation(value = "????????????????????????", notes = "????????????id???????????????????????????")
    @ApiImplicitParam(name = "userDto", value = "????????????user", required = true, dataType = "UserDto")
    @Log("????????????????????????")
    public ResponseBean<Boolean> updateLoginInfo(@RequestBody UserDto userDto) {
        Boolean success = Boolean.FALSE;
        if (StringUtils.isNotBlank(userDto.getIdentifier())) {
            UserAuths userAuths = new UserAuths();
            userAuths.setIdentifier(userDto.getIdentifier());
            userAuths = userAuthsService.getByIdentifier(userAuths);
            if (userAuths != null) {
                User user = new User();
                user.setId(userAuths.getUserId());
                user.setLoginTime(userDto.getLoginTime());
                user.setModifyDate(userDto.getLoginTime());
                user.setModifier(userAuths.getIdentifier());
                success = userService.update(user) > 0;
            }
        }
        return new ResponseBean<>(success);
    }
}
