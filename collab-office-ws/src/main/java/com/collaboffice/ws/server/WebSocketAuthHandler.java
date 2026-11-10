package com.collaboffice.ws.server;

import com.collaboffice.common.util.JwtTokenUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class WebSocketAuthHandler extends ChannelInboundHandlerAdapter {

    private final JwtTokenUtil jwtTokenUtil;

    public static final AttributeKey<String> USER_ID_KEY = AttributeKey.valueOf("userId");
    public static final AttributeKey<String> CONNECTION_ID_KEY = AttributeKey.valueOf("connectionId");

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest request) {
            URI uri = URI.create(request.uri());
            String path = uri.getPath();
            String query = uri.getQuery();

            if (!"/ws".equals(path)) {
                super.channelRead(ctx, msg);
                return;
            }

            String token = null;
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("token=")) {
                        token = URLDecoder.decode(param.substring(6), StandardCharsets.UTF_8);
                        break;
                    }
                }
            }

            if (token == null || !jwtTokenUtil.validateToken(token)) {
                log.warn("WebSocket鉴权失败, token无效: {}", token);
                ctx.close();
                return;
            }

            String userId = jwtTokenUtil.getUserIdFromToken(token);
            Channel channel = ctx.channel();
            channel.attr(USER_ID_KEY).set(userId);
            channel.attr(CONNECTION_ID_KEY).set(channel.id().asShortText());

            // 重构URI去掉token参数
            String newUri = "/ws";
            request.setUri(newUri);

            log.info("WebSocket握手成功, userId: {}, connectionId: {}", userId, channel.id().asShortText());
        }

        super.channelRead(ctx, msg);
    }
}
