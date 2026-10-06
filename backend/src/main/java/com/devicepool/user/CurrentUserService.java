package com.devicepool.user;

import org.springframework.stereotype.Service;

import com.devicepool.common.UnauthorizedException;

/**
 * Stand-in for real authentication. The id comes from the X-User-Id header and is
 * trusted as-is; anyone can impersonate anyone. Fine for this exercise, not for production.
 */
@Service
public class CurrentUserService {

	public static final String HEADER = "X-User-Id";

	private final AppUserRepository users;

	public CurrentUserService(AppUserRepository users) {
		this.users = users;
	}

	public AppUser require(Long userId) {
		if (userId == null) {
			throw new UnauthorizedException("Missing " + HEADER + " header");
		}
		return users.findById(userId)
				.orElseThrow(() -> new UnauthorizedException("Unknown user " + userId));
	}
}
