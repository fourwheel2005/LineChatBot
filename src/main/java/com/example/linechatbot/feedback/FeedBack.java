package com.example.linechatbot.feedback;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "`feed_back`")
public class FeedBack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    private String displayName;

    private String sentiment; // "พอใจ" หรือ "ไม่พอใจ"

    private LocalDateTime createdAt;
}
