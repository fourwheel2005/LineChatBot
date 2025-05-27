package com.example.linechatbot.User.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "`user`")
public class User {
    @Id
    private String userId;

    private String name;

    private String displayName;
    private LocalDateTime createdAt;

    private LocalDateTime lastMessageAt;

}
