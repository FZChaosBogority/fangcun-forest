package com.fangcun.forest.admin.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ModelAndView business(BusinessException exception) {
        return error(exception.getStatus(), exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, ConstraintViolationException.class})
    public ModelAndView invalidParameter(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "请求参数不正确，请检查编号、页码和每页数量。");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView unsupportedMethod(HttpRequestMethodNotSupportedException exception) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "此页面不支持该请求方式。");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView notFound(NoResourceFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "访问的页面或资源不存在。");
    }

    @ExceptionHandler(DataAccessException.class)
    public ModelAndView database(DataAccessException exception) {
        log.error("Database operation failed", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "数据暂时无法读取，请稍后重试。");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView unexpected(Exception exception) {
        log.error("Unexpected request failure", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "服务暂时不可用，请稍后重试。");
    }

    private ModelAndView error(HttpStatus status, String message) {
        ModelAndView view = new ModelAndView("error/message");
        view.setStatus(status);
        view.addObject("status", status.value());
        view.addObject("message", message);
        return view;
    }
}
