package com.projecty.projectyweb.user;

import com.projecty.projectyweb.configurations.AnyPermission;
import com.projecty.projectyweb.user.dto.PasswordDto;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

@CrossOrigin()
@RestController
public class UserController {
	private final UserService userService;
	private final UserRepository userRepository;

	public UserController(UserService userService, UserRepository userRepository) {
		this.userService = userService;
		this.userRepository = userRepository;
	}

	@PostMapping("register")
	public void registerPost(@Valid @ModelAttribute RegisterForm registerForm, BindingResult bindingResult)
			throws BindException {
		User user = userService.createUserFromRegisterForm(registerForm);
		userService.validateNewUser(user, bindingResult);
		if (bindingResult.hasErrors()) {
			throw new BindException(bindingResult);
		}
		userService.saveWithPasswordEncrypt(user);
	}

	@GetMapping("settings")
	public User settings() {
		return userService.getCurrentUser();
	}

	@PutMapping("changePassword")
	// FIXME : are the password encrypted ?
	// FIXME Do you need to check "repeated" password here as it should be already
	// checked by the front ?
	public void changePasswordPost(@RequestBody PasswordDto passwordDto

	) throws BindException {
		User user = userService.getCurrentUser();
		BindingResult bindingResult = userService.authUserAndValidatePassword(user, passwordDto.getCurrentPassword(),
				passwordDto.getNewPassword(), passwordDto.getRepeatPassword());
		if (bindingResult == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		} else if (bindingResult.hasErrors()) {
			throw new BindException(bindingResult);
		}
		userService.saveWithPasswordEncrypt(user);
	}

	@GetMapping("auth")
	public User getUser() {
		return userService.getCurrentUser();
	}

	@GetMapping("user/{userName}/avatar")
	@AnyPermission
	public @ResponseBody byte[] getAvatar(@PathVariable("userName") String userName, HttpServletResponse response)
			throws IOException, SQLException {
		Optional<User> maybeUser = userRepository.findByUsername(userName);
		User user = maybeUser.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (user.getAvatar() == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		response.setContentType(user.getAvatar().getContentType());
		response.setHeader("Content-Disposition", "attachment; filename=avatar-" + userName);
		response.flushBuffer();
		return IOUtils.toByteArray(user.getAvatar().getFile().getBinaryStream());
	}
}
