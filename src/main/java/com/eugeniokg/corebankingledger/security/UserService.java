package com.eugeniokg.corebankingledger.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        CurrentUserProvider currentUserProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserProvider = currentUserProvider;
    }

    public UserResponse createUser(CreateUserRequest request) {
        assertCanAssignRole(request.role());

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setCustomerId(request.customerId());
        user.setEnabled(true);
        user = userRepository.save(user);
        return UserResponse.from(user);
    }

    public UserResponse changeRole(UUID userId, ChangeRoleRequest request) {
        assertCanAssignRole(request.role());

        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        user.setRole(request.role());
        user = userRepository.save(user);
        return UserResponse.from(user);
    }

    /**
     * Only an ADMIN may create or promote a user to ADMIN - an OPERATOR may manage
     * CUSTOMER and OPERATOR accounts, but must never be able to mint another ADMIN.
     * Enforced here (service layer) in addition to the controller's @PreAuthorize check.
     */
    private void assertCanAssignRole(Role targetRole) {
        if (targetRole == Role.ADMIN && currentUserProvider.getCurrentUser().getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only an administrator can assign the ADMIN role");
        }
    }
}
