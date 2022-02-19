package com.lxg.blog.message.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MessageVo {
    private String id;
    private Integer type;
    private Integer senderId;
    private Integer senderName;
    private String content;
    private Long time;
}
