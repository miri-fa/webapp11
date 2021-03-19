package es.urjc.code.daw.marketplace.service;

import es.urjc.code.daw.marketplace.domain.Role;
import es.urjc.code.daw.marketplace.domain.User;
import es.urjc.code.daw.marketplace.repository.RoleRepository;
import es.urjc.code.daw.marketplace.repository.UserRepository;
import es.urjc.code.daw.marketplace.security.user.UserPrincipal;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ROLE = "ROLE_CLIENT";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository authorityRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        Role clientRole = roleRepository.findByName(DEFAULT_ROLE);
        user.getRoles().add(Role.builder().id(clientRole.getId()).build());
        return userRepository.saveAndFlush(user);
    }

    @Override
    public User updateUser(User user) {
        User storedUser = userRepository.findUserById(user.getId());
        storedUser.setFirstName(user.getFirstName());
        storedUser.setSurname(user.getSurname());
        storedUser.setAddress(user.getAddress());
        storedUser.setEmail(user.getEmail());

        if(!Objects.isNull(user.getProfilePictureFilename())) {
            storedUser.setProfilePictureFilename(user.getProfilePictureFilename());
        }

        String newPassword = StringUtils.trim(user.getPassword());
        if(StringUtils.isNotEmpty(newPassword) && StringUtils.isNotBlank(newPassword)) {
            String newEncodedPassword = passwordEncoder.encode(user.getPassword());
            storedUser.setPassword(newEncodedPassword);
        }

        userRepository.saveAndFlush(storedUser);
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        if(principal.getUser().getId().longValue() == user.getId().longValue()) {
            principal.setUser(storedUser);
        }

        return storedUser;
    }

    @Override
    public User enableUser(Long id) {
        User storedUser = userRepository.findUserById(id);
        storedUser.setIsEnabled(true);
        return userRepository.saveAndFlush(storedUser);
    }

    @Override
    public User disableUser(Long id) {
        User storedUser = userRepository.findUserById(id);
        storedUser.setIsEnabled(false);
        return userRepository.saveAndFlush(storedUser);
    }

    @Override
    public boolean existsUserById(Long id) {
        return userRepository.existsById(id);
    }

    @Override
    public User findUserById(Long id) {
        return userRepository.findUserById(id);
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findUserByEmail(email);
    }

    @Override
    public List<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).stream().collect(Collectors.toList());
    }

    @Override
    public User deleteUserById(Long id) {
        User user = userRepository.findUserById(id);
        userRepository.deleteById(id);
        return user;
    }

}