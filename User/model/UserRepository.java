package com.example.linechatbot.User.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long>
{
    User findByName(String name);

    Optional<User> findByUserId(String userId);

}
