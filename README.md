# Getting Started
## Summary
- setting `ClientOptions` through `LettuceClientConfigurationBuilderCustomizer` seems to break spring data redis timeout property: `spring.redis.timeout`
- the following does not resolve the bug
  - upgrading spring boot to the latest version (3.1.4 at the time of writing)
  - upgrading redis to the latest version (7.2.1 at the time of writing)

## Steps

### 1. Set up
- run redis in docker
    - `docker-compose up -d`

### 2. Reproduce the bug
- run Spring Boot Application
- call /hget once to allow redis client to connect to redis server at least once (important step to trigger the bug)
    - `curl localhost:8080/hget`
- run DEBUG SLEEP in redis-cli
    ```shell
    docker exec -it redis-timeout-bug-test-redis-1 sh
    /data # redis-cl
    127.0.0.1:6379> DEBUG SLEEP 20
    ```
- call /hget again
  - `curl localhost:8080/hget`
- redis client should time out after the configured timeout, however in reality, the application waits until server responds in ~20s
```shell
curl localhost:8080/hget
redisCommandTimeout=3s, timeElapsed=18325ms, hash={}
```

### 3. Run without ClientOptions customization
- set `customize-client-options` in `application.yml` to false
- run step 2 again
- application successfully times out after the configured duration
```txt
io.lettuce.core.RedisCommandTimeoutException: Command timed out after 3 second(s)
	at io.lettuce.core.internal.ExceptionFactory.createTimeoutException(ExceptionFactory.java:59) ~[lettuce-core-6.1.10.RELEASE.jar:6.1.10.RELEASE]
	at io.lettuce.core.protocol.CommandExpiryWriter.lambda$potentiallyExpire$0(CommandExpiryWriter.java:176) ~[lettuce-core-6.1.10.RELEASE.jar:6.1.10.RELEASE]
	at io.netty.util.concurrent.PromiseTask.runTask(PromiseTask.java:98) ~[netty-common-4.1.94.Final.jar:4.1.94.Final]
	at io.netty.util.concurrent.ScheduledFutureTask.run(ScheduledFutureTask.java:153) ~[netty-common-4.1.94.Final.jar:4.1.94.Final]
	at io.netty.util.concurrent.AbstractEventExecutor.runTask(AbstractEventExecutor.java:174) ~[netty-common-4.1.94.Final.jar:4.1.94.Final]
	at io.netty.util.concurrent.DefaultEventExecutor.run(DefaultEventExecutor.java:66) ~[netty-common-4.1.94.Final.jar:4.1.94.Final]
	at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997) ~[netty-common-4.1.94.Final.jar:4.1.94.Final]
	at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74) ~[netty-common-4.1.94.Final.jar:4.1.94.Final]
	at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30) ~[netty-common-4.1.94.Final.jar:4.1.94.Final]
	at java.base/java.lang.Thread.run(Thread.java:829) ~[na:na]
	Suppressed: java.lang.Exception: #block terminated with an error
		at reactor.core.publisher.BlockingSingleSubscriber.blockingGet(BlockingSingleSubscriber.java:100) ~[reactor-core-3.4.31.jar:3.4.31]
		at reactor.core.publisher.Mono.block(Mono.java:1742) ~[reactor-core-3.4.31.jar:3.4.31]
		at com.example.demo.redistimeoutbug.DemoController.hget(DemoController.java:35) ~[main/:na]
		at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
		at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:na]
		at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:na]
		at java.base/java.lang.reflect.Method.invoke(Method.java:566) ~[na:na]
		at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:205) ~[spring-web-5.3.29.jar:5.3.29]
		at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:150) ~[spring-web-5.3.29.jar:5.3.29]
		at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:117) ~[spring-webmvc-5.3.29.jar:5.3.29]
		at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:895) ~[spring-webmvc-5.3.29.jar:5.3.29]
		at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:808) ~[spring-webmvc-5.3.29.jar:5.3.29]
		at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87) ~[spring-webmvc-5.3.29.jar:5.3.29]
		at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1072) ~[spring-webmvc-5.3.29.jar:5.3.29]
		at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:965) ~[spring-webmvc-5.3.29.jar:5.3.29]
		at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1006) ~[spring-webmvc-5.3.29.jar:5.3.29]
		at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:898) ~[spring-webmvc-5.3.29.jar:5.3.29]
		at javax.servlet.http.HttpServlet.service(HttpServlet.java:529) ~[tomcat-embed-core-9.0.78.jar:4.0.FR]
		at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:883) ~[spring-webmvc-5.3.29.jar:5.3.29]
		at javax.servlet.http.HttpServlet.service(HttpServlet.java:623) ~[tomcat-embed-core-9.0.78.jar:4.0.FR]
		at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:209) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:153) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:51) ~[tomcat-embed-websocket-9.0.78.jar:9.0.78]
		at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:178) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:153) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-5.3.29.jar:5.3.29]
		at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.29.jar:5.3.29]
		at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:178) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:153) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[spring-web-5.3.29.jar:5.3.29]
		at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.29.jar:5.3.29]
		at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:178) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:153) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) ~[spring-web-5.3.29.jar:5.3.29]
		at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:117) ~[spring-web-5.3.29.jar:5.3.29]
		at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:178) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:153) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:167) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:90) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:481) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:130) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:93) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:74) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:343) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:390) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:63) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:926) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1791) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:52) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1191) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:659) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61) ~[tomcat-embed-core-9.0.78.jar:9.0.78]
		... 1 common frames omitted
```
