package com.devicepool.user;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backs the fake login: the frontend lists users, the person picks one, and every
 * subsequent request carries that user's id in the X-User-Id header.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

	private final AppUserRepository users;

	public UserController(AppUserRepository users) {
		this.users = users;
	}

	@GetMapping
	public List<UserDto> list() {
		return users.findAll(Sort.by("name")).stream().map(UserDto::from).toList();
	}

	public record UserDto(Long id, String name, String email, Role role) {
		static UserDto from(AppUser u) {
			return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole());
		}
	}
}
