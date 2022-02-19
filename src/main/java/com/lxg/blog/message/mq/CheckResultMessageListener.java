package com.lxg.blog.message.mq;

import com.lxg.blog.common.to.MessageTo;
import com.lxg.blog.common.utils.constant.RedisConstant;
import com.lxg.blog.message.vo.MessageVo;
import com.lxg.blog.message.ws.ProductWebSocket;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@RabbitListener(queues = {"failed-message-queue", "success-message-queue","tip-message-queue",
        "comment-message-queue","focus-message-queue","praise-message-queue"})
@Component
@Slf4j
public class CheckResultMessageListener {
    @Autowired
    StringRedisTemplate template;

    @RabbitHandler
    public void handler(MessageTo messageTo, Channel channel, Message message){
        log.info("收到消息{}:{}",messageTo.getId(),System.currentTimeMillis());
        try {
            boolean hasConsumed = template.opsForSet().isMember(RedisConstant.ArticleCheckMessage,messageTo.getId());
            if(!hasConsumed){
                MessageVo messageVo = new MessageVo();
                BeanUtils.copyProperties(messageTo,messageVo);
                ProductWebSocket.systemSendToUser(messageTo.getReceiverId(),messageVo);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
                template.opsForSet().add(RedisConstant.ArticleCheckMessage,messageTo.getId());
            }else{
                channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,false);
            }
        }catch (Exception e){
            log.warn(e.toString());
        }
        log.info("消息处理成功{}",System.currentTimeMillis());
    }
}
