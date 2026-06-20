package org.example.roomsched.util;

import lombok.Data;

/**
 * 统一响应结果封装类
 * 用于 AJAX 接口返回统一的 JSON 格式
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> {

    /** 状态码：200成功，400参数错误，401未登录，403无权限，500服务器错误 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 无数据的成功响应 */
    public static <T> Result<T> ok() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    /** 带数据的成功响应 */
    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /** 带自定义消息的成功响应 */
    public static <T> Result<T> ok(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    /** 错误响应 */
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    /** 带状态码的错误响应 */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
