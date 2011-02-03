package webbit.netty;

import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.util.CharsetUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;

import static org.jboss.netty.buffer.ChannelBuffers.copiedBuffer;

public class NettyHttpResponse implements webbit.HttpResponse {

    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private final ChannelHandlerContext ctx;
    private final HttpRequest request;
    private final HttpResponse response;
    private Charset charset;

    public NettyHttpResponse(ChannelHandlerContext ctx, HttpRequest request, HttpResponse response) {
        this.ctx = ctx;
        this.request = request;
        this.response = response;
        this.charset = DEFAULT_CHARSET;
    }

    @Override
    public NettyHttpResponse charset(Charset charset) {
        this.charset = charset;
        return this;
    }

    @Override
    public Charset charset() {
        return charset;
    }

    @Override
    public NettyHttpResponse status(int status) {
        response.setStatus(HttpResponseStatus.valueOf(status));
        return this;
    }

    @Override
    public int status() {
        return response.getStatus().getCode();
    }

    @Override
    public NettyHttpResponse header(String name, String value) {
        if (value == null) {
            response.removeHeader(name);
        } else {
            response.setHeader(name, value);
        }
        return this;
    }

    @Override
    public NettyHttpResponse header(String name, int value) {
        response.setHeader(name, value);
        return this;
    }

    @Override
    public NettyHttpResponse content(String content) {
        response.setContent(copiedBuffer(content, charset()));
        return this;
    }

    @Override
    public NettyHttpResponse content(byte[] content) {
        response.setContent(copiedBuffer(content));
        return this;
    }

    @Override
    public NettyHttpResponse error(Throwable error) {
        response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        String message = getStackTrace(error);
        header("Content-Type", "text/plain");
        header("Content-Length", message.length());
        content(message);
        flushResponse();
        return this;
    }

    private String getStackTrace(Throwable error) {
        StringWriter buffer = new StringWriter();
        PrintWriter writer = new PrintWriter(buffer);
        error.printStackTrace(writer);
        writer.flush();
        System.out.println("buffer = " + buffer);
        return buffer.toString();
    }

    @Override
    public NettyHttpResponse end() {
        // Generate an error page if response status code is not OK (200).
        if (response.getStatus().getCode() != 200) {
            response.setContent(copiedBuffer(response.getStatus().toString(), CharsetUtil.UTF_8));
            header("Content-Length", response.getContent().readableBytes());
        }
        flushResponse();
        return this;
    }

    private void flushResponse() {
        // Send the response and close the connection.
        ctx.getChannel().write(response)
                .addListener(ChannelFutureListener.CLOSE);
    }

}
