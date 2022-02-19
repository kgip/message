package com.lxg.blog.message.ws;

import com.alibaba.fastjson.JSON;
import com.lxg.blog.common.exception.CustomException;
import com.lxg.blog.common.exception.ErrorCode;
import com.lxg.blog.message.vo.MessageVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/websocket/{userId}")
@Component
@Slf4j
public class ProductWebSocket {
    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;

    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。若要实现服务端与单一客户端通信的话，可以使用Map来存放，其中Key可以为用户id
    private static ConcurrentHashMap<Integer, ProductWebSocket> webSocketSet = new ConcurrentHashMap<>();

    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    //当前发消息的人员编号
    private Integer userId;

    /**
     * 线程安全的统计在线人数
     *
     * @return
     */
    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        ProductWebSocket.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        ProductWebSocket.onlineCount--;
    }

    /**
     * 连接建立成功调用的方法
     *
     * @param param   用户唯一标识
     * @param session 可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    @OnOpen
    public void onOpen(@PathParam(value = "userId") Integer param, Session session) {
        userId = param;//接收到发送消息的人员编号
        this.session = session;
        if(!webSocketSet.containsKey(param)){
            addOnlineCount();
        }
        webSocketSet.put(param, this);//加入线程安全map中
        log.info("用户id：" + param + "加入连接！当前在线人数为" + getOnlineCount());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if (!userId.equals("")) {
            webSocketSet.remove(userId);  //根据用户id从ma中删除
            subOnlineCount();           //在线数减1
            log.info("用户id：" + userId + "关闭连接！当前在线人数为" + getOnlineCount());
        }
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message) {
        if(message.equals("ping")){
            try {
                session.getBasicRemote().sendText("pong");
                log.info("pong");
            }catch (Exception e){
                log.warn(e.toString());
            }
        }
    }

    /**
     * 发生错误时调用
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error(error.toString());
    }

    /**
     * 给指定的人发送消息user->user
     *
     * @param message
     */
    public static void sendToUser(Integer sendUserId, MessageVo message) {
        try {
            if (webSocketSet.get(sendUserId) != null) {
                webSocketSet.get(sendUserId).sendMessage(JSON.toJSONString(message));
            } else {
                if (webSocketSet.get(message.getSenderId()) != null) {
                    webSocketSet.get(message.getSenderId()).sendMessage("用户id：" + sendUserId + "已离线，未收到您的信息！");
                }
                System.out.println("消息接受人:" + sendUserId + "已经离线！");
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.UnknownException.getMsg(),ErrorCode.UnknownException.getCode());
        }
    }
    /**
     * 给所有人发消息user->all
     *
     * @param message
     */
    private static void sendAll(String message,Integer userId) {
        //遍历HashMap
        for (Integer key : webSocketSet.keySet()) {
            try {
                //判断接收用户是否是当前发消息的用户
                if (!userId.equals(key)) {
                    webSocketSet.get(key).sendMessage(message);
                }
            } catch (IOException e) {
                log.error(e.toString());
            }
        }
    }

    /**
     * 管理员发送消息system->user
     *
     * @param message
     */
    public static void systemSendToUser(Integer sendUserId, MessageVo message){
        try {
            if (webSocketSet.get(sendUserId) != null) {
                webSocketSet.get(sendUserId).sendMessage(JSON.toJSONString(message));
            } else {
                log.info(ErrorCode.NotOnline.getMsg(),ErrorCode.NotOnline.getCode());
            }
        }catch (Exception e){
            log.info(ErrorCode.UnknownException.getMsg(),ErrorCode.UnknownException.getCode());
        }
    }

    /**
     * 管理员发送消息system->user
     *
     * @param message
     */
    public static void systemSendToAll(MessageVo message) {
        //遍历HashMap
        for (Integer key : webSocketSet.keySet()) {
            try {
                webSocketSet.get(key).sendMessage(JSON.toJSONString(message));
            } catch (IOException e) {
                log.error(e.toString());
            }
        }
    }

    /**
     * 发送消息
     *
     * @param message
     * @throws IOException
     */
    public void sendMessage(String message) throws IOException {
        //同步发送
        this.session.getBasicRemote().sendText(message);
        //异步发送
        //this.session.getAsyncRemote().sendText(message);
    }
}
