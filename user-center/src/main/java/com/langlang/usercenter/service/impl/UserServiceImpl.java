package com.langlang.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.langlang.usercenter.service.UserService;
import com.langlang.usercenter.model.domain.User;
import com.langlang.usercenter.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.langlang.usercenter.contant.UserContant.UESR_LOGIN_STATE;

/**
 * 用户服务实现类
* @author 李作浪
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2023-11-12 22:51:02
 *
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    @Resource
    private UserMapper userMapper;

    /**
     * 盐值，混淆密码
     */
    public static final String SALT = "langlang";



    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1.校验
        if (StringUtils.isAllBlank(userAccount,userPassword,checkPassword)){
            // todo 修改为自定义异常
            return -1;
        }
        if (userAccount.length() < 4){
            return -1;
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8){
            return -1;
        }
        //账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find())  {
            return -1;
        }
        //密码和检验密码相同
        if (!userPassword.equals(checkPassword)) {
            return -1;
        }
        //账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0){
            return -1;
        }
        // 2.加密
        String encryptPassword = DigestUtils.md5DigestAsHex(( SALT + userPassword).getBytes());
        // 3.插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            return -1;
        }
        return user.getId();
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1.校验
        if (StringUtils.isAllBlank(userAccount,userPassword)){
            return null;
        }
        if (userAccount.length() < 4){
            return null;
        }
        if (userPassword.length() < 8){
            return null;
        }
        //账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find())  {
            return null;
        }
        // 2.加密
        String encryptPassword = DigestUtils.md5DigestAsHex(( SALT + userPassword).getBytes());
        //查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword",encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        //用户不存在
        if (user == null){
            log.info("user login failed, userAccount cannot match userPassword");
            return null;
        }
        // 3.用户脱敏
        User safetUser = getSafetyUser(user);
        // 4.记录用户的登录态
        request.getSession().setAttribute(UESR_LOGIN_STATE,safetUser);
        return safetUser;
    }

    /**
     * 用户脱敏
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null){
            return null;
        }
        User safetUser = new User();
        safetUser.setId(originUser.getId());
        safetUser.setUsername(originUser.getUsername());
        safetUser.setUserAccount(originUser.getUserAccount());
        safetUser.setAvatarUrl(originUser.getAvatarUrl());
        safetUser.setGender(originUser.getGender());
        safetUser.setPhone(originUser.getPhone());
        safetUser.setEmail(originUser.getEmail());
        safetUser.setUserRole(originUser.getUserRole());
        safetUser.setUserStatus(originUser.getUserStatus());
        safetUser.setCreateTime(originUser.getCreateTime());
        return safetUser;
    }

    /**
     * 用户注销
     *
     * @param request 请求
     * @return 1
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        //移除登陆状态
        request.getSession().removeAttribute(UESR_LOGIN_STATE);
        return 1;
    }


}




