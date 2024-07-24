package com.yeonieum.orderservice.web.interceptor;
import com.yeonieum.orderservice.global.usercontext.UserContext;
import com.yeonieum.orderservice.global.usercontext.UserContextHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;

public class UserContextInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        UserContext context = UserContextHolder.getContext();

        if (context.getTransactionId() != null) {
            template.header(UserContext.TRANSACTION_ID, context.getTransactionId());
        }

        if (context.getAuthToken() != null) {
            template.header(UserContext.AUTH_TOKEN, context.getAuthToken());
        }

        if (context.getUserId() != null) {
            template.header(UserContext.USER_ID, context.getUserId());
        }

        if (context.getServiceId() != null) {
            template.header(UserContext.SERVICE_ID, context.getServiceId());
        }
    }
}