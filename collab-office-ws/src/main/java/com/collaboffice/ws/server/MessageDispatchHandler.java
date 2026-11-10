package com.collaboffice.ws.server;

import com.collaboffice.common.message.WebSocketMessage;
import com.collaboffice.ws.heartbeat.HeartbeatDetector;
import com.collaboffice.ws.manager.ConnectionManager;
import com.collaboffice.ws.router.MessageRouter;
import com.collaboffice.ws.util.WebSocketJson;
import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class MessageDispatchHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final MessageRouter router;

    public MessageDispatchHandler(MessageRouter router) {
        this.router = router;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("新WebSocket连接: {}", ctx.channel().id().asShortText());
        ConnectionManager.addConnection(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame textFrame) {
            handleTextMessage(ctx, textFrame);
        } else if (frame instanceof PingWebSocketFrame) {
            ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
        } else if (frame instanceof PongWebSocketFrame) {
            // 心跳响应
        } else if (frame instanceof CloseWebSocketFrame) {
            ctx.channel().close();
        } else if (frame instanceof BinaryWebSocketFrame) {
            // 保留二进制消息处理
        }
    }

    private void handleTextMessage(ChannelHandlerContext ctx, TextWebSocketFrame textFrame) {
        try {
            String json = textFrame.text();
            WebSocketMessage message = WebSocketJson.parse(json);
            if (message == null) {
                log.warn("无效的消息格式: {}", json);
                return;
            }
            HeartbeatDetector.updateLastActiveTime(ctx.channel().id().asShortText());
            message.setServerTimestamp(System.currentTimeMillis());
            router.route(ctx.channel(), message);
        } catch (Exception e) {
            log.error("消息处理异常", e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("WebSocket连接断开: {}", ctx.channel().id().asShortText());
        ConnectionManager.removeConnection(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket异常", cause);
        ctx.close();
    }
}
