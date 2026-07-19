package com.pal.dipesh.razorpay.common.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pal.dipesh.razorpay.common.annotation.ResponseMessage;
import com.pal.dipesh.razorpay.common.entity.ApiResponse;

import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.pal.dipesh.razorpay")
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public GlobalResponseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        // Skip if the controller already returns an ApiResponse (avoids double wrapping, including error responses produced by GlobalExceptionHandler).
        Class<?> paramType = returnType.getParameterType();

        if (ApiResponse.class.isAssignableFrom(paramType)) {
            return false;
        }

        // Unwrap ResponseEntity<T> / HttpEntity<T> and skip if T is already ApiResponse.
        if (HttpEntity.class.isAssignableFrom(paramType)) {
            ResolvableType bodyType = ResolvableType.forMethodParameter(returnType).getGeneric(0);
            Class<?> resolvedBody = bodyType.resolve();

            if (resolvedBody != null && ApiResponse.class.isAssignableFrom(resolvedBody)) {
                return false;
            }
        }

        // Skip springdoc / swagger endpoints so we don't corrupt their JSON.
        String declaringPkg = returnType.getContainingClass().getPackageName();

        return !declaringPkg.startsWith("org.springdoc") && !declaringPkg.startsWith("springfox");
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  @NonNull MethodParameter returnType,
                                  @NonNull MediaType selectedContentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @NonNull ServerHttpRequest request,
                                  @NonNull ServerHttpResponse response) {

        // Defensive: already wrapped.
        if (body instanceof ApiResponse<?>) {
            return body;
        }

        HttpStatus status = resolveStatus(response);
        String path = (request instanceof ServletServerHttpRequest servletReq)
                ? servletReq.getServletRequest().getRequestURI()
                : request.getURI().getPath();

        String message = resolveMessage(returnType);

        ApiResponse<Object> wrapped = ApiResponse.success(body, message, status, path);

        if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
            try {
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return objectMapper.writeValueAsString(wrapped);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize ApiResponse for String return type", e);
            }
        }

        return wrapped;
    }

    private String resolveMessage(MethodParameter returnType) {
        ResponseMessage methodAnn = returnType.getMethodAnnotation(ResponseMessage.class);

        if (methodAnn != null) {
            return methodAnn.value();
        }

        ResponseMessage classAnn = returnType.getContainingClass().getAnnotation(ResponseMessage.class);

        if (classAnn != null) {
            return classAnn.value();
        }

        return "Success";
    }

    private HttpStatus resolveStatus(ServerHttpResponse response) {
        if (response instanceof ServletServerHttpResponse servletResp) {
            HttpStatus resolved = HttpStatus.resolve(servletResp.getServletResponse().getStatus());

            if (resolved != null) {
                return resolved;
            }
        }

        return HttpStatus.OK;
    }
}
