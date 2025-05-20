package com.example.linechatbot.User.model;


import com.example.linechatbot.User.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateUser(Long id, User updatedUser) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setName(updatedUser.getName());
                    return userRepository.save(user);
                })
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public void saveUserIfNotExists(String userId, String displayName) {
        Optional<User> existing = userRepository.findByUserId(userId);
        if (existing.isEmpty()) {
            User user = User.builder()
                    .userId(userId)
                    .displayName(displayName)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(user);
        }
    }

}
