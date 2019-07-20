package com.yunweibang.auth.controller;

import com.yunweibang.auth.common.JsonResponse;
import com.yunweibang.auth.model.AccountRegistryDTO;
import com.yunweibang.auth.model.SendEmailDTO;
import com.yunweibang.auth.model.ValidateEmailDTO;
import com.yunweibang.auth.model.ValidateUserInfoDTO;
import com.yunweibang.auth.service.UserService;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Controller
@RequestMapping("/user")
public class UserController {
    private UserService userService = new UserService();

    @RequestMapping(value = "/password/forgot", method = RequestMethod.GET)
    public String forgot() {
        if (userService.isLdapAuthType()) {
            return "casConsentView";
        } else {
            return "casPasswordlessDisplayView";
        }
    }

    @RequestMapping(value = "/validateUserInfo", method = RequestMethod.POST)
    @ResponseBody
    public JsonResponse<Object> validateUserInfo(@Valid ValidateUserInfoDTO dto, BindingResult result,
                                                 HttpServletRequest request) {
        if (result.hasErrors()) {
            throw new RuntimeException(result.getFieldError().getDefaultMessage());
        }
        return userService.validateUserInfo(request, dto);

    }

    @RequestMapping(value = "/password/sendMail", method = RequestMethod.GET)
    public String sendMail() {

        return "casPasswordlessGetUserIdView";
    }

    @RequestMapping(value = "/sendValidateEmail", method = RequestMethod.POST)
    @ResponseBody
    public JsonResponse<Object> sendValidateEmail(@Valid SendEmailDTO dto, BindingResult result,
                                                  HttpServletRequest request) {
        if (result.hasErrors()) {
            throw new RuntimeException(result.getFieldError().getDefaultMessage());
        }
        return userService.sendTestEmail(request, dto);

    }

    @RequestMapping(value = "/validateEmail", method = RequestMethod.POST)
    @ResponseBody
    public JsonResponse<Object> validateEmail(@Valid ValidateEmailDTO dto, BindingResult result, HttpServletRequest request) {

        if (result.hasErrors()) {
            throw new RuntimeException(result.getFieldError().getDefaultMessage());
        }
        return userService.validateEmail(dto, request);

    }

    @RequestMapping(value = "/password/resetPass", method = RequestMethod.GET)
    public String resetPass() {

        return "casResetPasswordSendInstructionsView";
    }

    @RequestMapping(value = "/editPasswd", method = RequestMethod.POST)
    @ResponseBody
    public JsonResponse<Object> editPasswd(@RequestParam("password1") String password1,
                                           @RequestParam("password2") String password2,
                                           @NotBlank @CookieValue("tmp_token") String token, HttpServletRequest request) {
        return userService.editPasswd(token, password1, password2, request);
    }

    @RequestMapping(value = "/password/success", method = RequestMethod.GET)
    public String passwordSuccess(@CookieValue(value = "tmp_token", required = false) String token) {

        if (token != null && !"".equals(token)) {
            userService.clearPassRecord(token);
        }
        return "casResetPasswordVerifyQuestionsView";
    }

    @RequestMapping(value = "/account/register", method = RequestMethod.GET)
    public String accountRegister() {

        return "casU2fRegistrationView";
    }

    @RequestMapping(value = "/register/save", method = RequestMethod.POST)
    @ResponseBody
    public JsonResponse<Object> saveAccountRegistry(@Valid AccountRegistryDTO dto, BindingResult result, HttpServletRequest request) {

        if (result.hasErrors()) {
            return new JsonResponse<Object>(400, result.getFieldError().getDefaultMessage(), null);
        }
        return userService.saveAccountRegistry(dto, request);

    }

    @RequestMapping(value = "/gohome", method = RequestMethod.POST)
    @ResponseBody
    public JsonResponse<Object> getHomeUrl() {
        return userService.getHomeUrl();
    }

    @RequestMapping(value = "/register/success", method = RequestMethod.GET)
    public String registerSuccess() {
        return "casYubiKeyRegistrationView";
    }

}
